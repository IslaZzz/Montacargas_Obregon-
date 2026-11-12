package mx.montacargasobregon.backend.rentas.captura;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/** Una renta capturada, para confirmarle al auxiliar lo que se guardó. */
public record RentaCapturadaResponse(
        Long id,
        String folio,
        String cliente,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        LocalDateTime entregaDesde,
        LocalDateTime entregaHasta,
        LocalDateTime recoleccionDesde,
        LocalDateTime recoleccionHasta,
        String estado,
        List<String> equipos) {   // Números de unidad

    public static RentaCapturadaResponse deRenta(Renta r) {
        return new RentaCapturadaResponse(
                r.getId(),
                r.getFolio(),
                nombreCliente(r.getCliente()),
                r.getFechaInicio(),
                r.getFechaFin(),
                r.getEntregaDesde(),
                r.getEntregaHasta(),
                r.getRecoleccionDesde(),
                r.getRecoleccionHasta(),
                r.getEstado().name(),
                r.getEquipos().stream().map(er -> er.getEquipo().getNumeroUnidad()).toList());
    }

    // La empresa si la tiene; si no, el nombre de la persona
    private static String nombreCliente(Cliente c) {
        return c.getEmpresa() != null && !c.getEmpresa().isBlank()
                ? c.getEmpresa()
                : c.getNombre() + " " + c.getApellidoPaterno();
    }
}