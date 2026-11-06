package mx.montacargasobregon.backend.operacion.inicio;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ruta HTTP de la pantalla de inicio. */
@RestController
@RequestMapping("/api/inicio")
public class InicioController {

    private final InicioService servicio;

    public InicioController(InicioService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/inicio: pendientes de hoy, avisos y próximas operaciones. */
    @GetMapping
    public InicioResponse consultar() {
        return servicio.consultar();
    }
}