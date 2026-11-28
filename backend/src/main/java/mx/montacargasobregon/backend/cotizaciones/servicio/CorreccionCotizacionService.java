package mx.montacargasobregon.backend.cotizaciones.servicio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.comun.DatosContactoSitio;
import mx.montacargasobregon.backend.cotizaciones.dto.CorregirCotizacionRequest;
import mx.montacargasobregon.backend.cotizaciones.modelo.CalculoPrecio;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Descuento;
import mx.montacargasobregon.backend.cotizaciones.modelo.EquipoCotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.TipoDescuento;
import mx.montacargasobregon.backend.usuarios.Usuario;
import mx.montacargasobregon.backend.usuarios.repositorio.UsuarioRepository;

/** Corregir una cotización que gerencia rechazó y volver a mandarla (HU-32). */
@Service
public class CorreccionCotizacionService {

    private final CotizacionService cotizaciones;
    private final CrearCotizacionService armado;
    private final UsuarioRepository usuarios;

    public CorreccionCotizacionService(CotizacionService cotizaciones, CrearCotizacionService armado,
            UsuarioRepository usuarios) {
        this.cotizaciones = cotizaciones;
        this.armado = armado;
        this.usuarios = usuarios;
    }

    /**
     * Guarda los datos corregidos. Con descuento la cotización vuelve a En autorización;
     * sin descuento queda en Borrador. Responde 409 si no está Rechazada y 400 si algún dato
     * o el descuento no son válidos.
     */
    @Transactional
    public Cotizacion corregir(Long id, CorregirCotizacionRequest datos, String correoAuxiliar) {
        Cotizacion c = cotizaciones.obtener(id);
        if (c.getEstado() != EstadoCotizacion.RECHAZADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede corregir una cotización rechazada");
        }

        // Mismas reglas que al crearla: fechas desde hoy, sin equipos repetidos y todos rentables
        List<EquipoCotizacion> equipos = armado.cotizarEquipos(datos.fechaEntrega(), datos.fechaRecoleccion(),
                datos.equipoIds());
        Descuento descuento = descuento(datos, subtotal(c, equipos));
        Usuario auxiliar = usuarios.findByCorreoIgnoreCase(correoAuxiliar)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar"));

        c.corregir(datos.fechaEntrega(), datos.fechaRecoleccion(),
                CrearCotizacionService.direccionObra(datos.direccionObra()),
                new DatosContactoSitio(datos.contactoObra().trim(), datos.telefonoContacto()),
                equipos, descuento, auxiliar);
        return c; // Dentro de la transacción, JPA guarda los cambios solo
    }

    // Equipos nuevos + los traslados que ya tenía, antes del descuento
    private static BigDecimal subtotal(Cotizacion c, List<EquipoCotizacion> equipos) {
        List<BigDecimal> conceptos = new ArrayList<>(equipos.stream().map(EquipoCotizacion::getImporte).toList());
        conceptos.add(c.getTrasladoEntrega());
        conceptos.add(c.getTrasladoRecoleccion());
        return CalculoPrecio.calcularSubtotal(conceptos);
    }

    // null si ya no lleva descuento
    private static Descuento descuento(CorregirCotizacionRequest datos, BigDecimal subtotal) {
        if (datos.descuentoTipo() == TipoDescuento.NINGUNO) {
            return null;
        }
        if (!Descuento.valorValido(datos.descuentoTipo(), datos.descuentoValor(), subtotal)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descuento inválido");
        }
        String motivo = datos.descuentoMotivo();
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe el motivo del descuento");
        }
        return new Descuento(datos.descuentoTipo(), datos.descuentoValor(), motivo.trim());
    }
}
