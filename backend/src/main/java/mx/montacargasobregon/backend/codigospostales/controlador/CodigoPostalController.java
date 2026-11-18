package mx.montacargasobregon.backend.codigospostales.controlador;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mx.montacargasobregon.backend.codigospostales.dto.CodigoPostalResponse;
import mx.montacargasobregon.backend.codigospostales.servicio.CodigoPostalService;

@RestController
@RequestMapping("/api/codigos-postales")
public class CodigoPostalController {

    private final CodigoPostalService servicio;

    public CodigoPostalController(CodigoPostalService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{codigoPostal:\\d{5}}")
    public CodigoPostalResponse buscar(@PathVariable String codigoPostal) {
        return servicio.buscar(codigoPostal);
    }
}
