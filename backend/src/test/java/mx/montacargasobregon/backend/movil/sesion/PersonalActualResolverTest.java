package mx.montacargasobregon.backend.movil.sesion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;

@ExtendWith(MockitoExtension.class)
class PersonalActualResolverTest {

    @Mock
    private SesionMovilService servicio;

    // El resolver pide el servicio cuando lo necesita, no al construirse
    @Mock
    private ObjectProvider<SesionMovilService> proveedor;

    private PersonalActualResolver resolver;

    @BeforeEach
    void setUp() {
        lenient().when(proveedor.getObject()).thenReturn(servicio);
        resolver = new PersonalActualResolver(proveedor);
    }

    @Test
    void obtieneAQuienUsaLaAppDelTokenBearer() {
        PersonalActual esperado = new PersonalActual(7L, "OPT-10", "Luis Valenzuela", TipoPersonal.OP_TRASLADO);
        when(servicio.validarToken("abc123")).thenReturn(esperado);

        PersonalActual actual = resolver.resolveArgument(null, null, peticion("Bearer abc123"), null);

        assertEquals(esperado, actual);
    }

    @Test
    void sinEncabezadoResponde401() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(null, null, peticion(null), null));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(servicio);
    }

    @Test
    void encabezadoQueNoEsBearerResponde401() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(null, null, peticion("7"), null));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(servicio);
    }

    @Test
    void yaNoLeeElEncabezadoInseguroXPersonalId() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("X-Personal-Id", "7");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(null, null, new ServletWebRequest(peticion), null));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(servicio);
    }

    @Test
    void siElServicioRechazaElTokenElErrorSePropaga() {
        when(servicio.validarToken("viejo")).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tu sesión de 22 horas venció."));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(null, null, peticion("Bearer viejo"), null));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verify(servicio).validarToken("viejo");
    }

    @Test
    void exigirRechazaOtroTipoDePersonal() {
        PersonalActual actual = new PersonalActual(7L, "OPT-10", "Luis Valenzuela", TipoPersonal.OP_TRASLADO);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> actual.exigir(TipoPersonal.MECANICO));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    private static ServletWebRequest peticion(String autorizacion) {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        if (autorizacion != null) {
            peticion.addHeader(PersonalActualResolver.ENCABEZADO, autorizacion);
        }
        return new ServletWebRequest(peticion);
    }
}
