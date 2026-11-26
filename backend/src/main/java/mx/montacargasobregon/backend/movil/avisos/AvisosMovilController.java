package mx.montacargasobregon.backend.movil.avisos;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import mx.montacargasobregon.backend.avisos.dto.AvisoOperacionResponse;
import mx.montacargasobregon.backend.avisos.servicio.AvisoOperacionService;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;

/** Avisos de quien usa la app móvil (por ahora, las tareas que le reasignaron). */
@RestController
@RequestMapping("/api/movil/avisos")
public class AvisosMovilController {

    private final AvisoOperacionService servicio;

    public AvisosMovilController(AvisoOperacionService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/movil/avisos: los que no ha leído. */
    @GetMapping
    public List<AvisoOperacionResponse> pendientes(PersonalActual personal) {
        return servicio.pendientesDe(personal.id());
    }

    /** PATCH /api/movil/avisos/leidos: ya los vio. */
    @PatchMapping("/leidos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLeidos(PersonalActual personal) {
        servicio.marcarLeidosDe(personal.id());
    }
}