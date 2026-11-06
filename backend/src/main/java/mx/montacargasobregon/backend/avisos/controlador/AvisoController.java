package mx.montacargasobregon.backend.avisos.controlador;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.montacargasobregon.backend.avisos.dto.ApartadoProximoResponse;
import mx.montacargasobregon.backend.avisos.servicio.ApartadoProximoService;

/** Rutas HTTP de los avisos. */
@RestController
@RequestMapping("/api/avisos")
public class AvisoController {

    private final ApartadoProximoService apartados;

    public AvisoController(ApartadoProximoService apartados) {
        this.apartados = apartados;
    }

    /** GET /api/avisos/apartados-proximos?dias=7: rentas que empiezan pronto y no tienen la carta firmada. */
    @GetMapping("/apartados-proximos")
    public List<ApartadoProximoResponse> apartadosProximos(@RequestParam(defaultValue = "7") int dias) {
        return apartados.listar(dias);
    }
}
