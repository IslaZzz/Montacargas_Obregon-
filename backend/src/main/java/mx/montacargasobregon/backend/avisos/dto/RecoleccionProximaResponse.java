package mx.montacargasobregon.backend.avisos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;

/**
 * OHU-25: una renta en curso que está por terminar (o ya terminó) y cuya recolección
 * no se ha confirmado. recoleccionDesde y trasladoFolio son null si todavía no tiene horario.
 */
public record RecoleccionProximaResponse(
        Long rentaId,
        String folio,
        String cliente,
        String domicilio,
        List<String> equipos,
        LocalDate fechaFin,
        long diasParaFin,   // 0 = hoy, 1 = mañana; negativo si ya venció
        boolean vencida,
        LocalDateTime recoleccionDesde,
        String trasladoFolio) {

    public static RecoleccionProximaResponse de(Renta renta, Traslado recoleccion, LocalDate hoy) {
        List<String> equipos = renta.getEquipos().stream()
                .filter(er -> er.getFase() != FaseEquipoRenta.CANCELADO)
                .map(er -> er.getEquipo().getNumeroUnidad())
                .sorted()
                .toList();
        long dias = ChronoUnit.DAYS.between(hoy, renta.getFechaFin());

        return new RecoleccionProximaResponse(
                renta.getId(),
                renta.getFolio(),
                ApartadoProximoResponse.nombreCliente(renta.getCliente()),
                renta.getDireccionObra(),
                equipos,
                renta.getFechaFin(),
                dias,
                dias < 0,
                recoleccion == null ? null : recoleccion.getFechaHoraInicio(),
                recoleccion == null ? null : recoleccion.getFolio());
    }
}