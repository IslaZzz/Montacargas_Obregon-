package mx.montacargasobregon.backend.cotizaciones.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.contratos.modelo.Contrato;
import mx.montacargasobregon.backend.contratos.servicio.ContratoService;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Descuento;
import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.ResultadoAutorizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.TipoDescuento;
import mx.montacargasobregon.backend.cotizaciones.repositorio.CotizacionRepository;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.rentas.servicio.RentaDeCotizacionService;
import mx.montacargasobregon.backend.usuarios.Usuario;
import mx.montacargasobregon.backend.usuarios.repositorio.UsuarioRepository;

/** Reglas de negocio para consultar las cotizaciones y cambiar su estado. */
@Service
public class CotizacionService {

    private final CotizacionRepository repositorio;
    private final RentaDeCotizacionService rentas;
    private final ContratoService contratos;
    private final UsuarioRepository usuarios;

    public CotizacionService(CotizacionRepository repositorio, RentaDeCotizacionService rentas,
            ContratoService contratos, UsuarioRepository usuarios) {
        this.repositorio = repositorio;
        this.rentas = rentas;
        this.contratos = contratos;
        this.usuarios = usuarios;
    }

    /** Todas las cotizaciones, la más nueva primero. */
    @Transactional(readOnly = true)
    public List<Cotizacion> listar() {
        return repositorio.findAllByOrderByIdDesc();
    }

    /** Obtiene una cotización por su id; si no existe, responde 404. */
    @Transactional(readOnly = true)
    public Cotizacion obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cotización no encontrada"));
    }

    /**
     * Cambia el estado: Borrador → Enviada → Aceptada o No aceptada.
     * Repetir el estado actual no cambia nada (por ejemplo, volver a descargar el
     * PDF de una enviada).
     */
    @Transactional
    public Cotizacion cambiarEstado(Long id, EstadoCotizacion nuevoEstado) {
        Cotizacion cotizacion = obtener(id);

        // Autorizar o rechazar un descuento tiene sus propias operaciones
        if (nuevoEstado != EstadoCotizacion.ENVIADA
                && nuevoEstado != EstadoCotizacion.ACEPTADA
                && nuevoEstado != EstadoCotizacion.NO_ACEPTADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ese estado no se cambia desde aquí");
        }

        if (cotizacion.getEstado() == nuevoEstado) {
            return cotizacion;
        }
        // Si ya pasó la vigencia, no se acepta aunque la tarea todavía no la haya marcado como Vencida (HU-17)
        if (nuevoEstado == EstadoCotizacion.ACEPTADA
                && cotizacion.vigenciaTerminada(LocalDate.now(Cotizacion.ZONA_HORARIA))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cotización vencida");
        }
        if (!cotizacion.getEstado().puedeCambiarA(nuevoEstado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cotización ya no puede cambiar a ese estado");
        }
        // Sin equipos no hay PDF que mandar
        if (nuevoEstado == EstadoCotizacion.ENVIADA && cotizacion.getEquipos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Agrega al menos un equipo");
        }
        // Un descuento solo se envía al cliente si gerencia lo autorizó
        if (nuevoEstado == EstadoCotizacion.ENVIADA && cotizacion.getDescuento() != null
                && (cotizacion.getAutorizacion() == null
                        || cotizacion.getAutorizacion().getResultado() != ResultadoAutorizacion.AUTORIZADA)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El descuento necesita la autorización de gerencia");
        }

        cotizacion.cambiarEstado(nuevoEstado, LocalDateTime.now(Cotizacion.ZONA_HORARIA));

        // Al aceptarla se genera su contrato en Apartado (HU-23), en la misma transacción: si falla,
        // tampoco queda aceptada. La renta ya no nace aquí: nace al registrar el anticipo (HU-24)
        if (nuevoEstado == EstadoCotizacion.ACEPTADA) {
            contratos.generar(cotizacion);
        }
        return cotizacion; // Dentro de la transacción, JPA guarda el cambio solo
    }

    /**
     * Pasa a Vencida las cotizaciones Enviadas cuya vigencia ya terminó (HU-17).
     * Vence si hoy es después de «válida hasta» (creación + Cotizacion.DIAS_VIGENCIA). Devuelve cuántas cambiaron.
     */
    @Transactional
    public int vencerCotizaciones() {
        LocalDate hoy = LocalDate.now(Cotizacion.ZONA_HORARIA);
        LocalDateTime limite = hoy.minusDays(Cotizacion.DIAS_VIGENCIA).atStartOfDay();

        List<Cotizacion> vencidas = repositorio
                .findByEstadoAndFechaHoraCreacionBefore(EstadoCotizacion.ENVIADA, limite);
        vencidas.forEach(Cotizacion::vencer);
        return vencidas.size(); // Dentro de la transacción, JPA guarda los cambios solo
    }

    /**
     * Guarda el costo de traslado de una cotización en Borrador y recalcula su
     * precio.
     * Si ya no es Borrador, responde 409.
     */
    @Transactional
    public Cotizacion registrarTraslado(Long id, BigDecimal entrega, BigDecimal recoleccion) {
        Cotizacion cotizacion = obtener(id);

        if (cotizacion.getEstado() != EstadoCotizacion.BORRADOR) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El traslado solo se puede capturar en una cotización en Borrador");
        }
        if (cotizacion.getDescuento() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El traslado no se puede cambiar en una cotización con descuento");
        }

        cotizacion.registrarTraslado(entrega, recoleccion);
        return cotizacion; // Dentro de la transacción, JPA guarda el cambio solo
    }

    /**
     * Marca el precio de la cotización como definido, al terminar el último paso del precio.
     * Si ya no es Borrador, responde 409.
     */
    @Transactional
    public Cotizacion definirPrecio(Long id) {
        Cotizacion c = obtener(id);

        if (c.getEstado() != EstadoCotizacion.BORRADOR) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El precio solo se puede definir en una cotización en Borrador");
        }

        c.definirPrecio();
        return c;
    }

    /** La renta que salió de la cotización, si ya tiene. */
    @Transactional(readOnly = true)
    public Optional<Renta> renta(Long cotizacionId) {
        return rentas.buscar(cotizacionId);
    }

    /** El contrato que se generó al aceptar la cotización, si ya tiene (HU-23). */
    @Transactional(readOnly = true)
    public Optional<Contrato> contrato(Long cotizacionId) {
        return contratos.buscarDeCotizacion(cotizacionId);
    }

    /** Sus equipos que están en el contrato Apartado de otra cotización aceptada, en las mismas fechas (HU-23). */
    @Transactional(readOnly = true)
    public Set<Long> equiposEnOtraCotizacionAceptada(Cotizacion cotizacion) {
        return contratos.equiposEnOtraCotizacionAceptada(cotizacion);
    }

    /**
     * Aplica o quita el descuento de una cotización en Borrador (HU-15).
     * Con descuento, la cotización pasa a En autorización para que gerencia lo
     * revise (HU-16).
     */
    @Transactional
    public Cotizacion aplicarDescuento(Long id, TipoDescuento tipo, BigDecimal valor, String motivo,
            String correoSolicitante) {
        Cotizacion c = obtener(id);

        if (c.getEstado() != EstadoCotizacion.BORRADOR) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede cambiar el descuento de una cotización en Borrador");
        }

        // Sin descuento: la cotización se queda sin Descuento (0..1) y sigue en
        // Borrador
        if (tipo == TipoDescuento.NINGUNO) {
            c.aplicarDescuento(null);
            return c;
        }

        if (!descuentoValido(tipo, valor, c.getSubtotal())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descuento inválido");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe el motivo del descuento");
        }

        Usuario solicitante = usuarios.findByCorreoIgnoreCase(correoSolicitante)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar"));

        c.aplicarDescuento(new Descuento(tipo, valor, motivo.trim()));
        c.enviarAAutorizacion(solicitante);
        return c;
    }

    /**
     * Porcentaje: mayor a 0 y menor a 100. Monto: mayor a 0 y menor al subtotal.
     */
    private boolean descuentoValido(TipoDescuento tipo, BigDecimal valor, BigDecimal subtotal) {
        if (valor == null || valor.signum() <= 0) {
            return false;
        }
        BigDecimal limite = tipo == TipoDescuento.PORCENTAJE ? new BigDecimal("100") : subtotal;
        return valor.compareTo(limite) < 0;
    }
}