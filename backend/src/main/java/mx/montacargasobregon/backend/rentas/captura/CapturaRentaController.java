package mx.montacargasobregon.backend.rentas.captura;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Rutas HTTP de la captura manual de rentas (temporal).
 * Comparte /api/rentas con la consulta de rentas, pero no repite ninguna de sus rutas.
 */
@RestController
@RequestMapping("/api/rentas")
public class CapturaRentaController {

    private final CapturaRentaService servicio;

    public CapturaRentaController(CapturaRentaService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/rentas/equipos-disponibles?inicio=2026-11-10&fin=2026-11-20 */
    @GetMapping("/equipos-disponibles")
    public List<EquipoDisponibleResponse> equiposDisponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return servicio.equiposDisponibles(inicio, fin);
    }

    /** POST /api/rentas: captura una renta; con horario de entrega, crea también revisiones y traslado. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentaCapturadaResponse crear(@Valid @RequestBody CrearRentaRequest datos) {
        return servicio.crear(datos);
    }

    /** PATCH /api/rentas/5/entrega: confirma el horario de entrega de una renta ya capturada. */
    @PatchMapping("/{id}/entrega")
    public RentaCapturadaResponse confirmarEntrega(@PathVariable Long id, @Valid @RequestBody HorarioRequest datos) {
        return servicio.confirmarEntrega(id, datos);
    }

    /** GET /api/rentas/sin-recoleccion: rentas en curso que aún no tienen horario de recolección. */
    @GetMapping("/sin-recoleccion")
    public List<RentaCapturadaResponse> sinRecoleccion() {
        return servicio.rentasSinRecoleccion();
    }

    /** PATCH /api/rentas/5/recoleccion: confirma el horario de recolección y crea su traslado. */
    @PatchMapping("/{id}/recoleccion")
    public RentaCapturadaResponse confirmarRecoleccion(@PathVariable Long id, @Valid @RequestBody HorarioRequest datos) {
        return servicio.confirmarRecoleccion(id, datos);
    }

}