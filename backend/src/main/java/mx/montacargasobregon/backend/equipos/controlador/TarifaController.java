package mx.montacargasobregon.backend.equipos.controlador;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.equipos.dto.TarifaRequest;
import mx.montacargasobregon.backend.equipos.dto.TarifaResponse;
import mx.montacargasobregon.backend.equipos.servicio.TarifaService;

/**
 * Rutas HTTP de las tarifas por modelo (HU-04).
 */
@RestController
@RequestMapping("/api/tarifas")
public class TarifaController {

    private final TarifaService servicio;

    public TarifaController(TarifaService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/tarifas: todos los modelos con su tarifa por día. */
    @GetMapping
    public List<TarifaResponse> listar() {
        return servicio.listar().stream()
                .map(TarifaResponse::toTarifaResponse)
                .toList();
    }

    /** PATCH /api/tarifas/5: cambia la tarifa del modelo con id 5; si no existe, responde 404. */
    @PatchMapping("/{modeloId}")
    public TarifaResponse actualizar(@PathVariable Long modeloId,
            @Valid @RequestBody TarifaRequest datos) {
        // @Valid: si la tarifa es nula, cero, negativa o tiene más de 2 decimales, responde 400
        return TarifaResponse.toTarifaResponse(servicio.actualizar(modeloId, datos.tarifaDia()));
    }
}