package mx.montacargasobregon.backend.avisos.controlador;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mx.montacargasobregon.backend.avisos.dto.AvisoOperacionResponse;
import mx.montacargasobregon.backend.avisos.servicio.AvisoOperacionService;

/** Avisos de la operación para el usuario de la oficina con la sesión abierta. */
@RestController
@RequestMapping("/api/avisos/operacion")
public class AvisoOperacionController {

    private final AvisoOperacionService servicio;

    public AvisoOperacionController(AvisoOperacionService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/avisos/operacion: los 20 más recientes. */
    @GetMapping
    public List<AvisoOperacionResponse> listar(Authentication usuario) {
        return servicio.listar(usuario.getName());
    }

    /** PATCH /api/avisos/operacion/leidos: los marca todos como leídos. */
    @PatchMapping("/leidos")
    public List<AvisoOperacionResponse> marcarLeidos(Authentication usuario) {
        return servicio.marcarLeidos(usuario.getName());
    }

    /** PATCH /api/avisos/operacion/{id}/leido: marca uno como leído. */
    @PatchMapping("/{id}/leido")
    public List<AvisoOperacionResponse> marcarLeido(@PathVariable Long id, Authentication usuario) {
        return servicio.marcarLeido(id, usuario.getName());
    }
}