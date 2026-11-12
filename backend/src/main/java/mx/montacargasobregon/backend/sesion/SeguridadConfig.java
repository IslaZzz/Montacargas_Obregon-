package mx.montacargasobregon.backend.sesion;

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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Inicio de sesión (HU-28): todo /api pide una sesión abierta, menos iniciarla y cerrarla.
 * La sesión vive en el servidor y el navegador solo guarda su cookie.
 */
@Configuration
public class SeguridadConfig {

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
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e.authenticationEntryPoint(sinSesion()));
        return http.build();
    }

    /** Sin sesión responde 401 con el mismo formato que ManejadorErrores. */
    private AuthenticationEntryPoint sinSesion() {
        return (request, response, ex) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"mensaje\":\"Inicia sesión para continuar\",\"errores\":{}}");
        };
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
