package mx.montacargasobregon.backend.movil.traslados;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;

/** Las tareas del operador de traslado en la app móvil. */
@RestController
@RequestMapping("/api/movil/traslados")
public class TareasTrasladoController {

    private final TareasTrasladoService servicio;

    public TareasTrasladoController(TareasTrasladoService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/movil/traslados?tipo=ENTREGA: mis entregas o mis recolecciones. */
    @GetMapping
    public List<TareaResponse> misTareas(@RequestParam TipoTraslado tipo, PersonalActual personal) {
        personal.exigir(TipoPersonal.OP_TRASLADO);
        return servicio.misTareas(personal, tipo);
    }

    /** GET /api/movil/traslados/{id}: el detalle de una tarea mía. */
    @GetMapping("/{id}")
    public DetalleTareaResponse detalle(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.OP_TRASLADO);
        return servicio.detalle(id, personal);
    }

    /** POST /api/movil/traslados/{id}/iniciar: salí con los equipos. */
    @PostMapping("/{id}/iniciar")
    public DetalleTareaResponse iniciar(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.OP_TRASLADO);
        return servicio.iniciar(id, personal);
    }

    /** POST /api/movil/traslados/{id}/completar: ya entregué o ya recogí. */
    @PostMapping("/{id}/completar")
    public DetalleTareaResponse completar(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.OP_TRASLADO);
        return servicio.completar(id, personal);
    }
}