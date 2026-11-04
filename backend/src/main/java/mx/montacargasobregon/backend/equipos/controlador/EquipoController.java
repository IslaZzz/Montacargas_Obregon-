package mx.montacargasobregon.backend.equipos.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.equipos.dto.CambiarEstadoRequest;
import mx.montacargasobregon.backend.equipos.dto.EquipoRequest;
import mx.montacargasobregon.backend.equipos.dto.EquipoResponse;
import mx.montacargasobregon.backend.equipos.servicio.EquipoService;

/** Rutas HTTP de los equipos. */
@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService servicio;

    public EquipoController(EquipoService servicio) {
        this.servicio = servicio;
    }

    /** POST /api/equipos: registra una unidad nueva. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoResponse registrar(@Valid @RequestBody EquipoRequest datos) {
        return EquipoResponse.toEquipoResponse(servicio.registrar(datos));
    }

    /** GET /api/equipos?texto=abc: busca equipos; sin "texto" devuelve todos. */
    @GetMapping
    public List<EquipoResponse> buscar(@RequestParam(required = false) String texto) {
        return servicio.buscar(texto).stream()
                .map(EquipoResponse::toEquipoResponse)
                .toList();
    }

    /** GET /api/equipos/5: obtiene el equipo con id 5; si no existe, responde 404. */
    @GetMapping("/{id}")
    public EquipoResponse obtener(@PathVariable Long id) {
        return EquipoResponse.toEquipoResponse(servicio.obtener(id));
    }

    /** PUT /api/equipos/5: cambia la serie y el modelo del equipo. */
    @PutMapping("/{id}")
    public EquipoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EquipoRequest datos) {
        return EquipoResponse.toEquipoResponse(servicio.actualizar(id, datos));
    }

    /** PATCH /api/equipos/5/estado: cambia solo el estado operativo. */
    @PatchMapping("/{id}/estado")
    public EquipoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest datos) {
        return EquipoResponse.toEquipoResponse(servicio.cambiarEstado(id, datos.estado()));
    }

    /** DELETE /api/equipos/5: da de baja el equipo, no lo borra. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@PathVariable Long id) {
        servicio.darDeBaja(id);
    }
}