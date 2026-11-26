package mx.montacargasobregon.backend.contratos.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.contratos.modelo.Contrato;
import mx.montacargasobregon.backend.contratos.modelo.EstadoContrato;
import mx.montacargasobregon.backend.contratos.repositorio.ContratoRepository;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;

/** Genera el contrato al aceptar una cotización y vence los apartados sin anticipo (HU-23). */
@Service
public class ContratoService {

    // Las cotizaciones que todavía se pueden aceptar: son las que avisan de un equipo apartado en otra
    private static final Set<EstadoCotizacion> SIN_RESPUESTA = EnumSet.of(
            EstadoCotizacion.BORRADOR, EstadoCotizacion.EN_AUTORIZACION, EstadoCotizacion.ENVIADA);

    private final ContratoRepository repositorio;

    public ContratoService(ContratoRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Genera el contrato de la cotización aceptada, en Apartado; si ya tiene uno, regresa ese.
     * No crea la renta ni bloquea los equipos: eso pasa al registrar el anticipo (HU-24).
     */
    @Transactional
    public Contrato generar(Cotizacion cotizacion) {
        return repositorio.findByCotizacionId(cotizacion.getId()).orElseGet(() -> repositorio.save(
                new Contrato(siguienteFolio(), cotizacion, LocalDateTime.now(Cotizacion.ZONA_HORARIA))));
    }

    /** Obtiene un contrato por su id; si no existe, responde 404. */
    @Transactional(readOnly = true)
    public Contrato obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato no encontrado"));
    }

    /** El contrato que salió de esa cotización, si ya se generó. */
    @Transactional(readOnly = true)
    public Optional<Contrato> buscarDeCotizacion(Long cotizacionId) {
        return repositorio.findByCotizacionId(cotizacionId);
    }

    /** Pasa a Apartado vencido los apartados cuya fecha límite de anticipo ya llegó. Devuelve cuántos cambiaron. */
    @Transactional
    public int vencerApartados() {
        List<Contrato> vencidos = repositorio.findByEstadoAndFechaLimiteAnticipoLessThanEqual(
                EstadoContrato.APARTADO, LocalDate.now(Cotizacion.ZONA_HORARIA));
        vencidos.forEach(Contrato::vencerApartado);
        return vencidos.size(); // Dentro de la transacción, JPA guarda los cambios solo
    }

    /**
     * Los equipos de la cotización que están en el contrato Apartado de otra cotización aceptada, en fechas
     * que se empalman: muestran «Este equipo está en otra cotización aceptada». Siguen disponibles.
     * Solo aplica a las cotizaciones que todavía se pueden aceptar.
     */
    @Transactional(readOnly = true)
    public Set<Long> equiposEnOtraCotizacionAceptada(Cotizacion c) {
        if (!SIN_RESPUESTA.contains(c.getEstado()) || c.getEquipos().isEmpty()) {
            return Set.of();
        }
        List<Long> equipoIds = c.getEquipos().stream().map(e -> e.getEquipo().getId()).toList();
        // Días completos: del inicio del día de entrega al final del día de recolección
        LocalDateTime desde = c.getFechaHoraEntrega().toLocalDate().atStartOfDay();
        LocalDateTime hasta = c.getFechaHoraRecoleccion().toLocalDate().plusDays(1).atStartOfDay();
        return new HashSet<>(repositorio.equiposEnContratoDeOtraCotizacion(
                EstadoContrato.APARTADO, c.getId(), equipoIds, desde, hasta));
    }

    // CON-0087 -> CON-0088
    private String siguienteFolio() {
        Integer ultimo = repositorio.ultimoNumeroFolio();
        return String.format("CON-%04d", ultimo == null ? 1 : ultimo + 1);
    }
}
