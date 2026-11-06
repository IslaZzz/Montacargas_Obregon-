package mx.montacargasobregon.backend.equipos.consulta;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Ruta HTTP de la consulta de equipos por estado. No repite ninguna ruta de EquipoController. */
@RestController
@RequestMapping("/api/equipos")
public class ConsultaEquiposController {

    private final ConsultaEquiposService servicio;

    public ConsultaEquiposController(ConsultaEquiposService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/equipos/estados, o con periodo: /api/equipos/estados?inicio=2026-11-10&fin=2026-11-20 */
    @GetMapping("/estados")
    public List<EquipoEstadoResponse> consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return servicio.consultar(inicio, fin);
    }
}