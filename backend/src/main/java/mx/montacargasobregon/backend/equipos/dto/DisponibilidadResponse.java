package mx.montacargasobregon.backend.equipos.dto;

import java.util.List;

// "ocupados" son los equipos de la búsqueda que no aparecen por estar rentados o apartados
public record DisponibilidadResponse(List<EquipoDisponibleResponse> equipos, int ocupados) {
}
