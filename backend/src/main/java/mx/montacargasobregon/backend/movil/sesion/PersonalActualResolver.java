package mx.montacargasobregon.backend.movil.sesion;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;

/**
 * Llena los parámetros PersonalActual de los controladores a partir del encabezado X-Personal-Id.
 * Temporal mientras el personal no tenga su propio inicio de sesión.
 *
 * El repositorio se pide con ObjectProvider (no en el constructor) para que este resolver se pueda
 * crear en pruebas de solo la capa web (@WebMvcTest), donde no se cargan los repositorios.
 */
@Component
public class PersonalActualResolver implements HandlerMethodArgumentResolver {

    public static final String ENCABEZADO = "X-Personal-Id";

    private final ObjectProvider<PersonalRepository> personalRepositorio;

    public PersonalActualResolver(ObjectProvider<PersonalRepository> personalRepositorio) {
        this.personalRepositorio = personalRepositorio;
    }

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return parametro.getParameterType().equals(PersonalActual.class);
    }

    @Override
    public PersonalActual resolveArgument(MethodParameter parametro, ModelAndViewContainer mavContainer,
                                          NativeWebRequest peticion, WebDataBinderFactory binderFactory) {
        Personal personal = personalRepositorio.getObject().findById(leerId(peticion.getHeader(ENCABEZADO)))
                .filter(Personal::isActivo)
                .orElseThrow(() -> noIdentificado("Tu usuario ya no está activo. Elige quién eres de nuevo."));
        return PersonalActual.de(personal);
    }

    private static Long leerId(String valor) {
        if (valor == null || valor.isBlank()) {
            throw noIdentificado("Elige quién eres para continuar.");
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw noIdentificado("Elige quién eres para continuar.");
        }
    }

    private static ResponseStatusException noIdentificado(String mensaje) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, mensaje);
    }
}