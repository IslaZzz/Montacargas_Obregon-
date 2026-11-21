package mx.montacargasobregon.backend.movil.sesion;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/**
 * Llena los parámetros PersonalActual de los controladores a partir del encabezado
 * "Authorization: Bearer <token>", validando la sesión móvil (existe, no está cerrada,
 * la persona está activa y no ha vencido). Responde 401 si algo de eso falla.
 *
 * El servicio se pide con ObjectProvider (no en el constructor) para que este resolver se pueda
 * crear en pruebas de solo la capa web (@WebMvcTest), donde no se cargan los servicios.
 */
@Component
public class PersonalActualResolver implements HandlerMethodArgumentResolver {

    public static final String ENCABEZADO = HttpHeaders.AUTHORIZATION;

    private final ObjectProvider<SesionMovilService> servicio;

    public PersonalActualResolver(ObjectProvider<SesionMovilService> servicio) {
        this.servicio = servicio;
    }

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return parametro.getParameterType().equals(PersonalActual.class);
    }

    @Override
    public PersonalActual resolveArgument(MethodParameter parametro, ModelAndViewContainer mavContainer,
                                          NativeWebRequest peticion, WebDataBinderFactory binderFactory) {
        String token = SesionMovilService.tokenDe(peticion.getHeader(ENCABEZADO));
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
        }
        return servicio.getObject().validarToken(token);
    }
}
