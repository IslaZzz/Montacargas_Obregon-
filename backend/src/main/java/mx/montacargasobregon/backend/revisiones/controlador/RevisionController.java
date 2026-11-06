package mx.montacargasobregon.backend.revisiones.controlador;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.revisiones.dto.AsignarRevisionesRequest;
import mx.montacargasobregon.backend.revisiones.dto.RevisionesRentaResponse;
import mx.montacargasobregon.backend.revisiones.servicio.RevisionService;

/** Rutas HTTP de las revisiones. */
@RestController
@RequestMapping("/api/revisiones")
public class RevisionController {

    private final RevisionService servicio;

    public RevisionController(RevisionService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/revisiones: una fila por renta y tipo, con sus revisiones adentro. */
    @GetMapping
    public List<RevisionesRentaResponse> listar() {
        return servicio.listarPorRenta();
    }

    /** PATCH /api/revisiones/asignar: asigna o cambia el mecánico; devuelve la lista actualizada. */
    @PatchMapping("/asignar")
    public List<RevisionesRentaResponse> asignar(@Valid @RequestBody AsignarRevisionesRequest datos) {
        return servicio.asignar(datos);
    }
}