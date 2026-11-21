package mx.montacargasobregon.backend.sesion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import jakarta.servlet.http.HttpServletResponse;
import mx.montacargasobregon.backend.usuarios.TipoUsuario;

/**
 * Inicio de sesión (HU-28): todo /api pide una sesión abierta, menos iniciarla y cerrarla.
 * La sesión vive en el servidor y el navegador solo guarda su cookie.
 * Acceso por rol (HU-29): las rutas de SOLO_GERENCIA responden 403 a un auxiliar.
 */
@Configuration
public class SeguridadConfig {

    /** Rutas de la API que solo puede usar gerencia: autorizaciones (HU-16) e ingresos. */
    static final String[] SOLO_GERENCIA = {"/api/autorizaciones/**", "/api/ingresos/**"};

    /** Altas que solo hace gerencia: registrar un equipo y agregar una etiqueta (HU-02). Consultarlos es de todos. */
    static final String[] ALTAS_DE_GERENCIA = {"/api/equipos", "/api/etiquetas"};

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, SecurityContextRepository sesiones) throws Exception {
        http
                // La cookie de sesión es SameSite=Strict (application.properties): otro sitio no puede usarla
                .csrf(AbstractHttpConfigurer::disable)
                // La pantalla de inicio de sesión es del frontend; aquí solo hay JSON
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .securityContext(c -> c.securityContextRepository(sesiones))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/sesion").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/sesion").permitAll()
                        // Acceso por rol (HU-29): lo de estas rutas es solo para gerencia
                        .requestMatchers(SOLO_GERENCIA).hasRole(TipoUsuario.GERENTE.name())
                        .requestMatchers(HttpMethod.POST, ALTAS_DE_GERENCIA).hasRole(TipoUsuario.GERENTE.name())
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(sinSesion())
                        .accessDeniedHandler(sinPermiso()));
        return http.build();
    }

    /** Sin sesión responde 401 con el mismo formato que ManejadorErrores. */
    private AuthenticationEntryPoint sinSesion() {
        return (request, response, ex) ->
                responder(response, HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar");
    }

    /** Con sesión pero sin el rol que pide la ruta responde 403, con el mismo formato. */
    private AccessDeniedHandler sinPermiso() {
        return (request, response, ex) ->
                responder(response, HttpStatus.FORBIDDEN, "No tienes permiso para hacer esto");
    }

    private static void responder(HttpServletResponse response, HttpStatus estado, String mensaje)
            throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"mensaje\":\"" + mensaje + "\",\"errores\":{}}");
    }

    @Bean
    public SecurityContextRepository sesiones() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder cifrado() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager autenticador(UserDetailsService usuarios, PasswordEncoder cifrado) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(usuarios);
        proveedor.setPasswordEncoder(cifrado);
        return new ProviderManager(proveedor);
    }
}
