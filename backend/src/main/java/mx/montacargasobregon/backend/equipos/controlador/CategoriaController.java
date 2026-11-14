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
import mx.montacargasobregon.backend.equipos.dto.CategoriaRequest;
import mx.montacargasobregon.backend.equipos.dto.CategoriaResponse;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.servicio.CategoriaRepetidaException;
import mx.montacargasobregon.backend.equipos.servicio.CategoriaService;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService servicio;

    public CategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<CategoriaResponse> listar() {
        return servicio.listar().stream().map(CategoriaController::aResponse).toList();
    }

    @GetMapping("/{id}")
    public CategoriaResponse obtener(@PathVariable Long id) {
        return aResponse(servicio.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest datos) {
        return aResponse(servicio.crear(datos));
    }

    @PutMapping("/{id}")
    public CategoriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest datos) {
        return aResponse(servicio.actualizar(id, datos));
    }

    @ExceptionHandler(CategoriaRepetidaException.class)
    public ResponseEntity<Map<String, Object>> repetida(CategoriaRepetidaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "mensaje", "Hay datos por corregir.",
                "errores", Map.of("nombre", ex.getMessage())));
    }

    private static CategoriaResponse aResponse(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre());
    }
}
