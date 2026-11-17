package mx.montacargasobregon.backend.equipos.controlador;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.equipos.dto.DefinicionCampoResponse;
import mx.montacargasobregon.backend.equipos.dto.ModeloDetalleResponse;
import mx.montacargasobregon.backend.equipos.dto.ModeloRequest;
import mx.montacargasobregon.backend.equipos.servicio.DatosModeloException;
import mx.montacargasobregon.backend.equipos.servicio.ModeloService;

/** Rutas HTTP de los modelos de equipo y sus especificaciones (HU-36). */
@RestController
@RequestMapping("/api")
public class ModeloController {

    private final ModeloService servicio;

    public ModeloController(ModeloService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/modelos: todos los modelos con sus especificaciones. */
    @GetMapping("/modelos")
    public List<ModeloDetalleResponse> listar() {
        return servicio.listar().stream().map(ModeloDetalleResponse::toResponse).toList();
    }

    /** GET /api/modelos/5: un modelo con sus especificaciones. */
    @GetMapping("/modelos/{id}")
    public ModeloDetalleResponse obtener(@PathVariable Long id) {
        return ModeloDetalleResponse.toResponse(servicio.obtener(id));
    }

    /** POST /api/modelos: registra un modelo, sin tarifa. */
    @PostMapping("/modelos")
    @ResponseStatus(HttpStatus.CREATED)
    public ModeloDetalleResponse crear(@Valid @RequestBody ModeloRequest datos) {
        return ModeloDetalleResponse.toResponse(servicio.crear(datos));
    }

    /** PUT /api/modelos/5: cambia los datos y especificaciones del modelo 5. */
    @PutMapping("/modelos/{id}")
    public ModeloDetalleResponse actualizar(@PathVariable Long id, @Valid @RequestBody ModeloRequest datos) {
        return ModeloDetalleResponse.toResponse(servicio.actualizar(id, datos));
    }

    /** GET /api/categorias/1/campos: los campos de especificación que pide la categoría 1. */
    @GetMapping("/categorias/{categoriaId}/campos")
    public List<DefinicionCampoResponse> campos(@PathVariable Long categoriaId) {
        return servicio.camposDe(categoriaId).stream().map(DefinicionCampoResponse::toResponse).toList();
    }

    @ExceptionHandler(DatosModeloException.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(DatosModeloException ex) {
        return ResponseEntity.status(ex.getEstado()).body(Map.of(
                "mensaje", ex.getMessage(),
                "errores", ex.getErrores()));
    }
}
