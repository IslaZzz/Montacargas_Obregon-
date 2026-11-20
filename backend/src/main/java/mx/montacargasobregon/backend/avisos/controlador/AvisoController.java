package mx.montacargasobregon.backend.avisos.controlador;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.montacargasobregon.backend.avisos.dto.ApartadoProximoResponse;
import mx.montacargasobregon.backend.avisos.dto.AvisoDescuentoResponse;
import mx.montacargasobregon.backend.avisos.servicio.ApartadoProximoService;
import mx.montacargasobregon.backend.avisos.servicio.AvisoDescuentoService;

/** Rutas HTTP de los avisos. */
@RestController
@RequestMapping("/api/avisos")
public class AvisoController {

    private final ApartadoProximoService apartados;
    private final AvisoDescuentoService descuentos;

    public AvisoController(ApartadoProximoService apartados, AvisoDescuentoService descuentos) {
        this.apartados = apartados;
        this.descuentos = descuentos;
    }

    /** GET /api/avisos/apartados-proximos?dias=7: rentas que empiezan pronto y no tienen la carta firmada. */
    @GetMapping("/apartados-proximos")
    public List<ApartadoProximoResponse> apartadosProximos(@RequestParam(defaultValue = "7") int dias) {
        return apartados.listar(dias);
    }

    /** GET /api/avisos/descuentos: respuestas de gerencia a los descuentos del usuario con la sesión abierta. */
    @GetMapping("/descuentos")
    public List<AvisoDescuentoResponse> descuentos(Authentication usuario) {
        return descuentos.listar(usuario.getName());
    }

    /** PATCH /api/avisos/descuentos/leidos: los marca todos como leídos y devuelve la lista actualizada. */
    @PatchMapping("/descuentos/leidos")
    public List<AvisoDescuentoResponse> marcarLeidos(Authentication usuario) {
        return descuentos.marcarLeidos(usuario.getName());
    }

    /** PATCH /api/avisos/descuentos/{id}/leido: marca un aviso como leído y devuelve la lista actualizada. */
    @PatchMapping("/descuentos/{id}/leido")
    public List<AvisoDescuentoResponse> marcarLeido(@PathVariable Long id, Authentication usuario) {
        return descuentos.marcarLeido(id, usuario.getName());
    }
}
