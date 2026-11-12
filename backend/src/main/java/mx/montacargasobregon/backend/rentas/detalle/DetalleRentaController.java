package mx.montacargasobregon.backend.rentas.detalle;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Rutas HTTP del detalle de la renta. Van bajo /detalle para no chocar con la consulta de rentas. */
@RestController
@RequestMapping("/api/rentas/{id}/detalle")
public class DetalleRentaController {

    private final DetalleRentaService servicio;

    public DetalleRentaController(DetalleRentaService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/rentas/5/detalle */
    @GetMapping
    public DetalleRentaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /** PATCH /api/rentas/5/detalle/recoleccion: confirma o cambia el horario de recolección. */
    @PatchMapping("/recoleccion")
    public DetalleRentaResponse confirmarRecoleccion(@PathVariable Long id,
                                                     @Valid @RequestBody HorarioRecoleccionRequest datos) {
        return servicio.confirmarRecoleccion(id, datos);
    }
}