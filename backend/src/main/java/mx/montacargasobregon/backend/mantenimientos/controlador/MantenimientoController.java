package mx.montacargasobregon.backend.mantenimientos.controlador;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.mantenimientos.dto.AsignarRequest;
import mx.montacargasobregon.backend.mantenimientos.dto.MantenimientoResponse;
import mx.montacargasobregon.backend.mantenimientos.servicio.MantenimientoService;

import org.springframework.web.bind.annotation.PathVariable;


/** Rutas HTTP de los mantenimientos. */
@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoController {

    private final MantenimientoService servicio;

    public MantenimientoController(MantenimientoService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/mantenimientos: todos, del más próximo a salir al más lejano. */
    @GetMapping
    public List<MantenimientoResponse> listar() {
        return servicio.listar().stream()
                .map(MantenimientoResponse::toMantenimientoResponse)
                .toList();
    }

    /** PATCH /api/mantenimientos/asignar: asigna o cambia el mecánico de uno o varios. */
    @PatchMapping("/asignar")
    public List<MantenimientoResponse> asignar(@Valid @RequestBody AsignarRequest datos) {
        return servicio.asignar(datos).stream()
                .map(MantenimientoResponse::toMantenimientoResponse)
                .toList();
    }

    /** PATCH /api/mantenimientos/5/terminar: el mecánico avisó que el equipo quedó reparado. */
    @PatchMapping("/{id}/terminar")
    public MantenimientoResponse terminar(@PathVariable Long id) {
        return MantenimientoResponse.toMantenimientoResponse(servicio.terminar(id));
    }
}