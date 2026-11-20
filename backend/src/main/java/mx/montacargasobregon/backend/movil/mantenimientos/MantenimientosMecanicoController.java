package mx.montacargasobregon.backend.movil.mantenimientos;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;

/** Los mantenimientos del mecánico en la app móvil. */
@RestController
@RequestMapping("/api/movil/mantenimientos")
public class MantenimientosMecanicoController {

    private final MantenimientosMecanicoService servicio;

    public MantenimientosMecanicoController(MantenimientosMecanicoService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/movil/mantenimientos: mis mantenimientos nuevos y en curso. */
    @GetMapping
    public List<MantenimientoMecanicoResponse> misMantenimientos(PersonalActual personal) {
        personal.exigir(TipoPersonal.MECANICO);
        return servicio.misMantenimientos(personal);
    }

    /** GET /api/movil/mantenimientos/{id} */
    @GetMapping("/{id}")
    public DetalleMantenimientoMecanicoResponse detalle(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.MECANICO);
        return servicio.detalle(id, personal);
    }

    /** POST /api/movil/mantenimientos/{id}/iniciar: el mecánico lo abrió. */
    @PostMapping("/{id}/iniciar")
    public DetalleMantenimientoMecanicoResponse iniciar(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.MECANICO);
        return servicio.iniciar(id, personal);
    }

    /** PUT /api/movil/mantenimientos/{id}/tareas/{tareaId}: atender o corregir una tarea. */
    @PutMapping("/{id}/tareas/{tareaId}")
    public DetalleMantenimientoMecanicoResponse atender(@PathVariable Long id, @PathVariable Long tareaId,
                                                        @Valid @RequestBody AtenderTareaRequest datos,
                                                        PersonalActual personal) {
        personal.exigir(TipoPersonal.MECANICO);
        return servicio.atender(id, tareaId, personal, datos);
    }

    /** POST /api/movil/mantenimientos/{id}/tareas: agregar lo que encontró. */
    @PostMapping("/{id}/tareas")
    @ResponseStatus(HttpStatus.CREATED)
    public DetalleMantenimientoMecanicoResponse agregar(@PathVariable Long id,
                                                        @Valid @RequestBody AgregarTareaRequest datos,
                                                        PersonalActual personal) {
        personal.exigir(TipoPersonal.MECANICO);
        return servicio.agregar(id, personal, datos);
    }

    /** POST /api/movil/mantenimientos/{id}/tareas/{tareaId}/incidencia: no se pudo completar. */
    @PostMapping("/{id}/tareas/{tareaId}/incidencia")
    public DetalleMantenimientoMecanicoResponse incidencia(@PathVariable Long id, @PathVariable Long tareaId,
                                                           @Valid @RequestBody IncidenciaRequest datos,
                                                           PersonalActual personal) {
        personal.exigir(TipoPersonal.MECANICO);
        return servicio.reportarIncidencia(id, tareaId, personal, datos);
    }

    /** POST /api/movil/mantenimientos/{id}/finalizar */
    @PostMapping("/{id}/finalizar")
    public DetalleMantenimientoMecanicoResponse finalizar(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.MECANICO);
        return servicio.finalizar(id, personal);
    }
}