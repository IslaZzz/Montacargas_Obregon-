package mx.montacargasobregon.backend.movil.sesion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Todo lo que está bajo /api/movil pide una sesión vigente, salvo una lista corta y deliberada.
 * Si alguien agrega un endpoint móvil sin recibir PersonalActual, esta prueba falla.
 */
class MovilEndpointsProtegidosTest {

    // Abiertos a propósito (ver MovilSeguridadConfig): entrar, cerrar, validar por su cuenta y la imagen de <img>
    private static final Set<String> ABIERTOS = Set.of(
            "SesionMovilController#entrar",
            "SesionMovilController#yo",
            "SesionMovilController#cerrar",
            "FotoController#imagen");

    @Test
    void cadaEndpointMovilExigeSesionSalvoLosAbiertosAPropositoYNoHayOtros() throws Exception {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> sinProteger = new ArrayList<>();
        int revisados = 0;
        for (BeanDefinition definicion : escaner.findCandidateComponents("mx.montacargasobregon.backend")) {
            Class<?> controlador = Class.forName(definicion.getBeanClassName());
            RequestMapping base = AnnotatedElementUtils.findMergedAnnotation(controlador, RequestMapping.class);
            if (base == null || !esMovil(base)) {
                continue;
            }
            for (Method metodo : controlador.getDeclaredMethods()) {
                if (AnnotatedElementUtils.findMergedAnnotation(metodo, RequestMapping.class) == null) {
                    continue;
                }
                revisados++;
                String nombre = controlador.getSimpleName() + "#" + metodo.getName();
                boolean pidePersonal = List.of(metodo.getParameterTypes()).contains(PersonalActual.class);
                if (!pidePersonal && !ABIERTOS.contains(nombre)) {
                    sinProteger.add(nombre);
                }
                if (pidePersonal && ABIERTOS.contains(nombre)) {
                    sinProteger.add(nombre + " (está en la lista de abiertos pero sí exige sesión: quítalo de la lista)");
                }
            }
        }

        assertTrue(revisados > 0, "No se encontró ningún endpoint de /api/movil");
        assertTrue(sinProteger.isEmpty(), "Endpoints móviles sin PersonalActual: " + sinProteger);
        assertFalse(ABIERTOS.isEmpty());
    }

    private static boolean esMovil(RequestMapping mapeo) {
        for (String ruta : mapeo.value()) {
            if (ruta.startsWith("/api/movil")) {
                return true;
            }
        }
        return false;
    }
}

