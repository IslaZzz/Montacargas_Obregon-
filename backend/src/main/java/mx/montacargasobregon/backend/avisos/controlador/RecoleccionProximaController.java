package mx.montacargasobregon.backend.avisos.controlador;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.montacargasobregon.backend.avisos.dto.RecoleccionProximaResponse;
import mx.montacargasobregon.backend.avisos.servicio.RecoleccionProximaService;

/** Recordatorio de recolecciones próximas (OHU-25). */
@RestController
@RequestMapping("/api/avisos/recolecciones-proximas")
public class RecoleccionProximaController {

    private final RecoleccionProximaService servicio;

    public RecoleccionProximaController(RecoleccionProximaService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/avisos/recolecciones-proximas?dias=7: rentas por terminar sin recolección confirmada. */
    @GetMapping
    public List<RecoleccionProximaResponse> listar(@RequestParam(defaultValue = "7") int dias) {
        return servicio.listar(dias);
    }
}