package mx.montacargasobregon.backend.rentas.detalle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;

/** Todo lo que muestra el detalle de una renta. */
public record DetalleRentaResponse(
        Long id,
        String folio,
        EstadoRenta estado,
        String cliente,
        String contacto,
        String telefono,
        String direccion,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        LocalDateTime entregaDesde,
        LocalDateTime entregaHasta,
        LocalDateTime recoleccionDesde,
        LocalDateTime recoleccionHasta,
        TrasladoRecoleccion trasladoRecoleccion,   // Null mientras no se confirma la recolección
        List<EquipoDeRenta> equipos) {

    /** El traslado que va por los equipos. */
    public record TrasladoRecoleccion(Long id, String folio, EstadoTraslado estado, boolean yaInicio,
                                      String responsable) {   // Null si no tiene operador
    }

    /** Un equipo de la renta y en qué paso va. */
    public record EquipoDeRenta(Long id, String numeroUnidad, String equipo, String categoria,
                                FaseEquipoRenta fase) {
    }

    public static DetalleRentaResponse de(Renta r, Traslado recoleccion) {
        return new DetalleRentaResponse(
                r.getId(),
                r.getFolio(),
                r.getEstado(),
                nombreCliente(r.getCliente()),
                r.getContactoObra(),
                r.getTelefonoContacto(),
                r.getDireccionObra(),
                r.getFechaInicio(),
                r.getFechaFin(),
                r.getEntregaDesde(),
                r.getEntregaHasta(),
                r.getRecoleccionDesde(),
                r.getRecoleccionHasta(),
                recoleccion == null ? null : deTraslado(recoleccion),
                r.getEquipos().stream()
                        .filter(er -> er.getFase() != FaseEquipoRenta.CANCELADO)
                        .map(DetalleRentaResponse::deEquipo)
                        .toList());
    }

    private static TrasladoRecoleccion deTraslado(Traslado t) {
        String responsable = t.getResponsable() == null
                ? null
                : t.getResponsable().getNombres() + " " + t.getResponsable().getApellidoPaterno();
        return new TrasladoRecoleccion(t.getId(), t.getFolio(), t.getEstado(), t.isYaInicio(), responsable);
    }

    private static EquipoDeRenta deEquipo(EquipoRenta er) {
        var equipo = er.getEquipo();
        return new EquipoDeRenta(
                equipo.getId(),
                equipo.getNumeroUnidad(),
                equipo.getModelo().getMarca() + " " + equipo.getModelo().getNombre(),
                equipo.getModelo().getCategoria().getNombre(),
                er.getFase());
    }

    // La empresa si la tiene; si no, el nombre de la persona
    private static String nombreCliente(Cliente c) {
        return c.getEmpresa() != null && !c.getEmpresa().isBlank()
                ? c.getEmpresa()
                : c.getNombre() + " " + c.getApellidoPaterno();
    }
}