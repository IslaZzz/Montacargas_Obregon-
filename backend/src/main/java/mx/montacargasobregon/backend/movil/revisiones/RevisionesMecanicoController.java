package mx.montacargasobregon.backend.movil.revisiones;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;

/** Las revisiones del mecánico en la app móvil. */
@RestController
@RequestMapping("/api/movil/revisiones")
public class RevisionesMecanicoController {

    private final RevisionesMecanicoService servicio;

    public RevisionesMecanicoController(RevisionesMecanicoService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/movil/revisiones?tipo=SALIDA: mis revisiones por hacer (sin tipo, todas). */
    @GetMapping
    public List<RevisionMecanicoResponse> misRevisiones(@RequestParam(required = false) TipoRevision tipo,
                                                        PersonalActual personal) {
        personal.exigir(TipoPersonal.MECANICO);
        return servicio.misRevisiones(personal, tipo);
    }

    /** GET /api/movil/revisiones/{id} */
    @GetMapping("/{id}")
    public DetalleRevisionMecanicoResponse detalle(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.MECANICO);
        return servicio.detalle(id, personal);
    }

    /** POST /api/movil/revisiones/{id}/iniciar: el mecánico la abrió. */
    @PostMapping("/{id}/iniciar")
    public DetalleRevisionMecanicoResponse iniciar(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.MECANICO);
        return servicio.iniciar(id, personal);
    }

    /** POST /api/movil/revisiones/{id}/terminar: guarda lo que llenó y la termina. */
    @PostMapping("/{id}/terminar")
    public RevisionTerminadaResponse terminar(@PathVariable Long id, @Valid @RequestBody CapturaRevisionRequest datos,
                                              PersonalActual personal) {
        personal.exigir(TipoPersonal.MECANICO);
        return servicio.terminar(id, personal, datos);
    }
}