package mx.montacargasobregon.backend.equipos.controlador;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.equipos.dto.EtiquetaRequest;
import mx.montacargasobregon.backend.equipos.dto.EtiquetaResponse;
import mx.montacargasobregon.backend.equipos.servicio.EtiquetaRepetidaException;
import mx.montacargasobregon.backend.equipos.servicio.EtiquetaService;

/** Rutas HTTP de las etiquetas de equipo (HU-02). */
@RestController
@RequestMapping("/api/etiquetas")
public class EtiquetaController {

    private final EtiquetaService servicio;

    public EtiquetaController(EtiquetaService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/etiquetas: todas, por nombre. */
    @GetMapping
    public List<EtiquetaResponse> listar() {
        return servicio.listar().stream().map(EtiquetaResponse::de).toList();
    }

    /** POST /api/etiquetas: agrega una etiqueta nueva. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EtiquetaResponse crear(@Valid @RequestBody EtiquetaRequest datos) {
        return EtiquetaResponse.de(servicio.crear(datos));
    }

    @ExceptionHandler(EtiquetaRepetidaException.class)
    public ResponseEntity<Map<String, Object>> repetida(EtiquetaRepetidaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "mensaje", "Hay datos por corregir.",
                "errores", Map.of("nombre", ex.getMessage())));
    }
}
