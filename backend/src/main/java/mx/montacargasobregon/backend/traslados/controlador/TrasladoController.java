package mx.montacargasobregon.backend.traslados.controlador;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.traslados.dto.AsignarTrasladoRequest;
import mx.montacargasobregon.backend.traslados.dto.OperadorDisponibilidadResponse;
import mx.montacargasobregon.backend.traslados.dto.TrasladoResponse;
import mx.montacargasobregon.backend.traslados.servicio.TrasladoService;

@RestController
@RequestMapping("/api/traslados")
public class TrasladoController {

    private final TrasladoService servicio;

    public TrasladoController(TrasladoService servicio) {
        this.servicio = servicio;
    }

    /**
     * GET /api/traslados
     * Listado de traslados con filtros por tipo/asignación y rango de fechas.
     */
    @GetMapping
    public List<TrasladoResponse> listar(
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        return servicio.listar(filtro, fechaInicio, fechaFin);
    }

    /**
     * GET /api/traslados/{id}
     * Detalle de un traslado específico.
     */
    @GetMapping("/{id}")
    public TrasladoResponse detalle(@PathVariable Long id) {
        return servicio.obtenerPorId(id);
    }

    /**
     * GET /api/traslados/disponibilidad
     * Consulta de operadores y su estado (Disponible/Ocupado) para un horario específico o un traslado.
     */
    @GetMapping("/disponibilidad")
    public List<OperadorDisponibilidadResponse> disponibilidad(
            @RequestParam(required = false) Long trasladoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHoraInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHoraFin
    ) {
        return servicio.consultarDisponibilidad(trasladoId, fechaHoraInicio, fechaHoraFin);
    }

    /**
     * POST /api/traslados/{id}/asignar
     * Asignar o cambiar el operador responsable de un traslado.
     */
    @PostMapping("/{id}/asignar")
    public TrasladoResponse asignar(
            @PathVariable Long id,
            @Valid @RequestBody AsignarTrasladoRequest request
    ) {
        return servicio.asignar(id, request);
    }

    /**
     * Soporte adicional para PATCH /api/traslados/{id}/asignar si el cliente usa PATCH.
     */
    @PatchMapping("/{id}/asignar")
    public TrasladoResponse asignarPatch(
            @PathVariable Long id,
            @Valid @RequestBody AsignarTrasladoRequest request
    ) {
        return servicio.asignar(id, request);
    }

    /**
     * PATCH /api/traslados/{id}/completar
     * Marca el traslado como realizado y hace avanzar la renta.
     */
    @PatchMapping("/{id}/completar")
    public TrasladoResponse completar(@PathVariable Long id) {
        return servicio.completar(id);
    }

}

