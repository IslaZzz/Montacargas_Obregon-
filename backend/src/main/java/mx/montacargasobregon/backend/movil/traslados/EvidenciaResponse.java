package mx.montacargasobregon.backend.movil.traslados;

import java.util.List;

import mx.montacargasobregon.backend.movil.fotos.FotoResponse;

/**
 * OHU13: la evidencia fotográfica de una recolección. Cada equipo lleva sus cuatro vistas;
 * foto es null en la vista que falta. editable es false una vez confirmada la recolección.
 */
public record EvidenciaResponse(String folio, String rentaFolio, int tomadas, int requeridas, boolean editable,
                                List<EquipoEvidencia> equipos) {

    public record EquipoEvidencia(Long id, String numeroUnidad, int tomadas, List<VistaEvidencia> vistas) {
    }

    public record VistaEvidencia(String vista, FotoResponse foto) {
    }
}