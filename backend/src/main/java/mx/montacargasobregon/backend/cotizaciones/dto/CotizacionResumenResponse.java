package mx.montacargasobregon.backend.cotizaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;

/** Una fila de la lista de cotizaciones. */
public record CotizacionResumenResponse(Long id, String folio, String cliente, String empresa,
                                        LocalDateTime fechaCreacion, LocalDateTime fechaEntrega,
                                        LocalDateTime fechaRecoleccion, BigDecimal total, String estado) {

    public static CotizacionResumenResponse toCotizacionResumenResponse(Cotizacion c) {
        return new CotizacionResumenResponse(
                c.getId(),
                c.getFolio(),
                nombreCompleto(c.getCliente()),
                c.getCliente().getEmpresa(),
                c.getFechaHoraCreacion(),
                c.getFechaHoraEntrega(),
                c.getFechaHoraRecoleccion(),
                c.getTotal(),
                c.getEstado().name());
    }

    private static String nombreCompleto(Cliente c) {
        String nombre = c.getNombre() + " " + c.getApellidoPaterno();
        return c.getApellidoMaterno() == null ? nombre : nombre + " " + c.getApellidoMaterno();
    }
}
