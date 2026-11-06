package mx.montacargasobregon.backend.avisos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import mx.montacargasobregon.backend.cartas.modelo.EstadoCarta;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/** Una renta programada que está por empezar y cuya carta todavía no se firma. */
public record ApartadoProximoResponse(
        Long rentaId,
        String folio,
        String cliente,
        List<EquipoApartado> equipos,
        LocalDate fechaInicio,
        LocalDateTime entregaDesde, // Vacío si todavía no se confirma el horario
        long diasParaInicio,        // 0 = hoy, 1 = mañana
        EstadoCarta estadoCarta) {

    /** Un equipo de la renta, como se muestra en el aviso. */
    public record EquipoApartado(String numeroUnidad, String modelo) {

        static EquipoApartado de(Equipo e) {
            return new EquipoApartado(e.getNumeroUnidad(),
                    e.getModelo().getMarca() + " " + e.getModelo().getNombre());
        }
    }

    public static ApartadoProximoResponse de(Renta renta, EstadoCarta estadoCarta, LocalDate hoy) {
        List<EquipoApartado> equipos = renta.getEquipos().stream()
                // Un equipo cancelado ya no sale en esta renta
                .filter(er -> er.getFase() != FaseEquipoRenta.CANCELADO)
                .map(er -> EquipoApartado.de(er.getEquipo()))
                .sorted(Comparator.comparing(EquipoApartado::numeroUnidad))
                .toList();

        return new ApartadoProximoResponse(
                renta.getId(),
                renta.getFolio(),
                nombreCliente(renta.getCliente()),
                equipos,
                renta.getFechaInicio(),
                renta.getEntregaDesde(),
                ChronoUnit.DAYS.between(hoy, renta.getFechaInicio()),
                estadoCarta);
    }

    // La empresa si la tiene; si no, el nombre de la persona
    static String nombreCliente(Cliente c) {
        return c.getEmpresa() != null && !c.getEmpresa().isBlank()
                ? c.getEmpresa()
                : c.getNombre() + " " + c.getApellidoPaterno();
    }
}
