package mx.montacargasobregon.backend.movil.sesion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la app móvil. Los mecánicos y operadores no son usuarios del sistema (no tienen
 * correo ni contraseña), así que /api/movil no usa la sesión de HU-28 (cookie): entran con su código
 * (POST /api/movil/sesion) y reciben un token que mandan en "Authorization: Bearer <token>".
 * Este filtro solo deja la cadena sin sesión HTTP; el token lo valida PersonalActualResolver, y
 * cada endpoint que necesita saber quién es recibe un parámetro PersonalActual (401 si no sirve).
 * Sin PersonalActual solo quedan abiertos, a propósito: POST /sesion (entrar), DELETE /sesion
 * (cerrar, idempotente), GET /yo (valida el token por su cuenta) y GET /fotos/{id} (la carga un
 * &lt;img&gt;, que no puede mandar encabezados). MovilEndpointsProtegidosTest vigila esa lista.
 * Se evalúa antes que SeguridadConfig y solo aplica a /api/movil/**; el resto de /api sigue pidiendo sesión.
 */
@Configuration
public class MovilSeguridadConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain filtrosMovil(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/movil/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }
}