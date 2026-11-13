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
 * correo ni contraseña), así que /api/movil no usa la sesión de HU-28: quién hace la petición
 * lo resuelve PersonalActualResolver con X-Personal-Id. Se evalúa antes que SeguridadConfig
 * y solo aplica a /api/movil/**; el resto de /api sigue pidiendo sesión.
 * Temporal hasta que el personal tenga su propio inicio de sesión.
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