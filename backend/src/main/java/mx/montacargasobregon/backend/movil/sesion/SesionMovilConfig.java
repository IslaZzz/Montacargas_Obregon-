package mx.montacargasobregon.backend.movil.sesion;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra PersonalActualResolver para que los controladores puedan recibir PersonalActual. */
@Configuration
public class SesionMovilConfig implements WebMvcConfigurer {

    private final PersonalActualResolver resolver;

    public SesionMovilConfig(PersonalActualResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolver);
    }
}