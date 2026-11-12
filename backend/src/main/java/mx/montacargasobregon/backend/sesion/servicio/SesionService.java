package mx.montacargasobregon.backend.sesion.servicio;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.sesion.dto.SesionResponse;
import mx.montacargasobregon.backend.usuarios.Usuario;
import mx.montacargasobregon.backend.usuarios.repositorio.UsuarioRepository;

/** Busca al usuario que inicia sesión por su correo (HU-28). */
@Service
@Transactional(readOnly = true)
public class SesionService implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public SesionService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** Spring Security lo llama al iniciar sesión para comparar la contraseña cifrada. */
    @Override
    public UserDetails loadUserByUsername(String correo) {
        Usuario u = usuarios.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario"));

        // Un usuario dado de baja no puede entrar. El rol se usa para el acceso por rol (HU-29)
        return User.withUsername(u.getCorreo())
                .password(u.getContrasena())
                .roles(u.getTipo().name())
                .disabled(!u.isActivo())
                .build();
    }

    /** Los datos del usuario con la sesión abierta, para el encabezado. */
    public SesionResponse datos(String correo) {
        return usuarios.findByCorreoIgnoreCase(correo)
                .filter(Usuario::isActivo)
                .map(SesionResponse::toSesionResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar"));
    }
}
