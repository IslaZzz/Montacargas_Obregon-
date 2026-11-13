package mx.montacargasobregon.backend.sesion.controlador;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import mx.montacargasobregon.backend.sesion.dto.IniciarSesionRequest;
import mx.montacargasobregon.backend.sesion.dto.SesionResponse;
import mx.montacargasobregon.backend.sesion.servicio.SesionService;

/** Iniciar sesión, ver quién la tiene y cerrarla (HU-28). */
@RestController
@RequestMapping("/api/sesion")
public class SesionController {

    private final AuthenticationManager autenticador;
    private final SecurityContextRepository sesiones;
    private final SesionService servicio;

    public SesionController(AuthenticationManager autenticador, SecurityContextRepository sesiones,
                            SesionService servicio) {
        this.autenticador = autenticador;
        this.sesiones = sesiones;
        this.servicio = servicio;
    }

    /** POST /api/sesion: revisa el correo y la contraseña y abre la sesión. */
    @PostMapping
    public SesionResponse iniciar(@Valid @RequestBody IniciarSesionRequest datos,
                                  HttpServletRequest request, HttpServletResponse response) {
        Authentication usuario;
        try {
            usuario = autenticador.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                    datos.correo().trim(), datos.contrasena()));
        } catch (AuthenticationException e) {
            // El mismo mensaje si falla el correo, la contraseña o si el usuario está de baja
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }

        // Cambia el id de la sesión al entrar, para que no sirva uno que alguien ya conocía
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(usuario);
        SecurityContextHolder.setContext(contexto);
        sesiones.saveContext(contexto, request, response);

        return servicio.datos(usuario.getName());
    }

    /** GET /api/sesion: el usuario con la sesión abierta; 401 si no hay sesión. */
    @GetMapping
    public SesionResponse actual(Authentication usuario) {
        return servicio.datos(usuario.getName());
    }

    /** DELETE /api/sesion: cierra la sesión. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrar(HttpServletRequest request, HttpServletResponse response, Authentication usuario) {
        new SecurityContextLogoutHandler().logout(request, response, usuario);
        new CookieClearingLogoutHandler("JSESSIONID").logout(request, response, usuario);
    }
}
