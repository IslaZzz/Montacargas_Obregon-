package mx.montacargasobregon.backend.movil.historial;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;

/** El historial del mecánico. El resumen de cada trabajo sale del detalle de revisiones o mantenimientos. */
@RestController
@RequestMapping("/api/movil/historial")
public class HistorialMecanicoController {

    private final HistorialMecanicoService servicio;

    public HistorialMecanicoController(HistorialMecanicoService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/movil/historial: lo que terminé en los últimos 30 días. */
    @GetMapping
    public List<HistorialMecanicoResponse> miHistorial(PersonalActual personal) {
        personal.exigir(TipoPersonal.MECANICO);
        return servicio.miHistorial(personal);
    }
}