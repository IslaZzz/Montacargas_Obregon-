package mx.montacargasobregon.backend.movil.traslados;

import java.time.LocalDateTime;
import java.util.List;

import mx.montacargasobregon.backend.comun.dto.DomicilioResponse;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;

/**
 * El detalle de una tarea. Trae ya resuelto si el operador puede iniciarla o marcarla realizada,
 * y el motivo cuando no, para que la app solo lo muestre.
 */
public record DetalleTareaResponse(Long id, String folio, TipoTraslado tipo, EstadoTraslado estado,
                                   LocalDateTime inicio, LocalDateTime fin, String rentaFolio,
                                   EstadoRenta estadoRenta, String cliente, DomicilioResponse domicilio,
                                   boolean firmaEnSitio, List<EquipoTareaResponse> equipos, int listos,
                                   boolean puedeIniciar, boolean puedeCompletar, String bloqueo) {

    public static DetalleTareaResponse de(Traslado traslado, boolean firmaEnSitio, FaseEquipoRenta faseRequerida,
                                          boolean puedeIniciar, boolean puedeCompletar, String bloqueo) {
        Renta renta = traslado.getRenta();
        List<EquipoTareaResponse> equipos = renta.getEquipos().stream()
                .filter(er -> er.getFase() != FaseEquipoRenta.CANCELADO)
                .map(er -> EquipoTareaResponse.de(er, faseRequerida))
                .toList();

        return new DetalleTareaResponse(
                traslado.getId(),
                traslado.getFolio(),
                traslado.getTipo(),
                traslado.getEstado(),
                traslado.getFechaHoraInicio(),
                traslado.getFechaHoraFin(),
                renta.getFolio(),
                renta.getEstado(),
                TareaResponse.nombreCliente(renta.getCliente()),
                TareaResponse.domicilioDeLaObra(renta),
                firmaEnSitio,
                equipos,
                (int) equipos.stream().filter(EquipoTareaResponse::listo).count(),
                puedeIniciar,
                puedeCompletar,
                bloqueo);
    }
}