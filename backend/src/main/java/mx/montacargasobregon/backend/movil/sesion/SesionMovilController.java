package mx.montacargasobregon.backend.movil.sesion;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Entrar a la app móvil con el código del gafete y manejar la sesión de esa jornada. */
@RestController
@RequestMapping("/api/movil")
public class SesionMovilController {

    private final SesionMovilService servicio;

    public SesionMovilController(SesionMovilService servicio) {
        this.servicio = servicio;
    }

    /**
     * POST /api/movil/sesion: abre una sesión con el código (la anterior de esa persona se cierra).
     * Responde { token, venceEn, personal }; 401 si el código no existe o la persona está de baja.
     */
    @PostMapping("/sesion")
    public SesionMovilResponse entrar(@Valid @RequestBody IngresarCodigoRequest datos) {
        return servicio.entrar(datos.codigo());
    }

    /** GET /api/movil/yo: { venceEn, personal } de la sesión del token; 401 si no sirve o ya venció. */
    @GetMapping("/yo")
    public SesionVigenteResponse yo(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String autorizacion) {
        return servicio.validarSesion(SesionMovilService.tokenDe(autorizacion));
    }

    /** DELETE /api/movil/sesion: cierra la sesión del token. 204 siempre, aunque ya estuviera cerrada. */
    @DeleteMapping("/sesion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String autorizacion) {
        servicio.cerrarSesion(SesionMovilService.tokenDe(autorizacion));
    }
}
