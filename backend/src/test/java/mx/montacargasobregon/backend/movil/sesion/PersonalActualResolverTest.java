package mx.montacargasobregon.backend.movil.sesion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;

@ExtendWith(MockitoExtension.class)
class PersonalActualResolverTest {

    @Mock
    private PersonalRepository personalRepositorio;

    // El resolver pide el repositorio cuando lo necesita, no al construirse
    @Mock
    private ObjectProvider<PersonalRepository> proveedor;

    @InjectMocks
    private PersonalActualResolver resolver;

    @BeforeEach
    void setUp() {
        lenient().when(proveedor.getObject()).thenReturn(personalRepositorio);
    }

    @Test
    void obtieneAQuienUsaLaAppDelEncabezado() {
        when(personalRepositorio.findById(7L)).thenReturn(Optional.of(operador()));

        PersonalActual actual = resolver.resolveArgument(null, null, peticion("7"), null);

        assertEquals(7L, actual.id());
        assertEquals("Rafael Amaya", actual.nombre());
        assertEquals(TipoPersonal.OP_TRASLADO, actual.tipo());
    }

    @Test
    void sinEncabezadoResponde401() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(null, null, peticion(null), null));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void encabezadoInvalidoResponde401() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(null, null, peticion("abc"), null));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void personalInactivoResponde401() {
        Personal inactivo = operador();
        inactivo.desactivar();
        when(personalRepositorio.findById(7L)).thenReturn(Optional.of(inactivo));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(null, null, peticion("7"), null));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void exigirRechazaOtroTipoDePersonal() {
        PersonalActual actual = PersonalActual.de(operador());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> actual.exigir(TipoPersonal.MECANICO));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    private static ServletWebRequest peticion(String personalId) {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        if (personalId != null) {
            peticion.addHeader(PersonalActualResolver.ENCABEZADO, personalId);
        }
        return new ServletWebRequest(peticion);
    }

    private static Personal operador() {
        Personal p = new Personal("OP-01", "Rafael", "Amaya", "López", TipoPersonal.OP_TRASLADO);
        ReflectionTestUtils.setField(p, "id", 7L);
        return p;
    }
}