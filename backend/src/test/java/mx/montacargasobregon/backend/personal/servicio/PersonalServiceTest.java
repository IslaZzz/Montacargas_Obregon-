package mx.montacargasobregon.backend.personal.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;

@ExtendWith(MockitoExtension.class)
class PersonalServiceTest {

    @Mock
    private PersonalRepository repositorio;

    private PersonalService servicio;

    private Personal rafael;

    @BeforeEach
    void setUp() {
        servicio = new PersonalService(repositorio);

        rafael = new Personal("MEC-10", "Rafael", "Amaya", null, TipoPersonal.MECANICO);
        ReflectionTestUtils.setField(rafael, "id", 1L);
    }

    @Test
    void mecanicoActivo_devuelveAlMecanico_cuandoEsMecanicoYEstaActivo() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(rafael));

        assertSame(rafael, servicio.mecanicoActivo(1L));
    }

    @Test
    void mecanicoActivo_responde404_cuandoNoExiste() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.mecanicoActivo(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Mecánico no encontrado", ex.getReason());
    }

    @Test
    void mecanicoActivo_responde400_cuandoEsOperadorDeTraslado() {
        Personal luis = new Personal("OPT-10", "Luis", "Valenzuela", null, TipoPersonal.OP_TRASLADO);
        ReflectionTestUtils.setField(luis, "id", 2L);
        when(repositorio.findById(2L)).thenReturn(Optional.of(luis));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.mecanicoActivo(2L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("La persona elegida no es un mecánico activo", ex.getReason());
    }

    @Test
    void mecanicoActivo_responde400_cuandoEstaInactivo() {
        rafael.desactivar();
        when(repositorio.findById(1L)).thenReturn(Optional.of(rafael));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.mecanicoActivo(1L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}