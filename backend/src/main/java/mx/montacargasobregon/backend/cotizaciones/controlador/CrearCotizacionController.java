package mx.montacargasobregon.backend.cotizaciones.controlador;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.cotizaciones.dto.CotizacionResponse;
import mx.montacargasobregon.backend.cotizaciones.dto.CrearCotizacionRequest;
import mx.montacargasobregon.backend.cotizaciones.servicio.CrearCotizacionService;

@RestController
@RequestMapping("/api/cotizaciones")
public class CrearCotizacionController {

    private final CrearCotizacionService servicio;

    public CrearCotizacionController(CrearCotizacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CotizacionResponse crear(@Valid @RequestBody CrearCotizacionRequest datos) {
        return CotizacionResponse.toCotizacionResponse(servicio.crear(datos));
    }
}
