package mx.montacargasobregon.backend.cotizaciones.controlador;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.cotizaciones.dto.CorregirCotizacionRequest;
import mx.montacargasobregon.backend.cotizaciones.dto.CotizacionResponse;
import mx.montacargasobregon.backend.cotizaciones.servicio.CorreccionCotizacionService;

/** Ruta para corregir una cotización rechazada (HU-32). */
@RestController
@RequestMapping("/api/cotizaciones")
public class CorreccionCotizacionController {

    private final CorreccionCotizacionService servicio;

    public CorreccionCotizacionController(CorreccionCotizacionService servicio) {
        this.servicio = servicio;
    }

    /**
     * PUT /api/cotizaciones/5/correccion: guarda la cotización corregida. Con descuento pasa a
     * En autorización; sin descuento, a Borrador. Responde 409 si no está Rechazada y 400 si algún
     * dato no es válido. Una rechazada nunca tiene renta, por eso la respuesta va sin ella.
     */
    @PutMapping("/{id}/correccion")
    public CotizacionResponse corregir(@PathVariable Long id, @Valid @RequestBody CorregirCotizacionRequest datos,
            Authentication usuario) {
        return CotizacionResponse.toCotizacionResponse(servicio.corregir(id, datos, usuario.getName()));
    }
}
