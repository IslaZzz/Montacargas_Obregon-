package mx.montacargasobregon.backend.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Devuelve los errores como JSON: { mensaje, errores: { campo: texto } },
 * para que puedan mostrarse bien en el frontend.
 */
@RestControllerAdvice
public class ManejadorErrores {

    /** Errores de validación de los DTO: responde 400 con el detalle por campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));

        return ResponseEntity.badRequest().body(Map.of(
                "mensaje", "Hay datos por corregir.",
                "errores", errores));
    }

    /**
     * Errores que lanzan los servicios (404, 409...): conserva el código
     * y manda el motivo como mensaje, con el mismo formato que la validación.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> estado(ResponseStatusException ex) {
        String mensaje = ex.getReason() != null
                ? ex.getReason()
                : "No fue posible completar la operación.";

        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "mensaje", mensaje,
                "errores", Map.of()));
    }
}