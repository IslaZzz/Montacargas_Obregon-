package mx.montacargasobregon.backend.personal.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

@ExtendWith(MockitoExtension.class)
class PersonalServiceTest {

    @Mock
    private PersonalRepository repositorio;

    @Mock
    private TrasladoRepository traslados;

    @Mock
    private RevisionRepository revisiones;

    @Mock
    private MantenimientoRepository mantenimientos;

    private PersonalService servicio;

    private Personal rafael;
    private Personal luis;

    @BeforeEach
    void setUp() {
        servicio = new PersonalService(repositorio, traslados, revisiones, mantenimientos);

        rafael = new Personal("MEC-10", "Rafael", "Amaya", null, TipoPersonal.MECANICO);
        ReflectionTestUtils.setField(rafael, "id", 1L);

        luis = new Personal("OPT-10", "Luis", "Valenzuela", null, TipoPersonal.OP_TRASLADO);
        ReflectionTestUtils.setField(luis, "id", 2L);
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

    // OHU28: sin pendientes, la baja deja a la persona inactiva
    @Test
    void desactivar_dejaInactivo_cuandoNoTienePendientes() {
        when(repositorio.findById(2L)).thenReturn(Optional.of(luis));
        when(traslados.countByResponsableIdAndEstadoIn(eq(2L), any())).thenReturn(0L);
        when(revisiones.countByMecanicoIdAndEstadoIn(eq(2L), any())).thenReturn(0L);
        when(mantenimientos.countByMecanicoIdAndEstadoIn(eq(2L), any())).thenReturn(0L);

        servicio.desactivar(2L);

        assertFalse(luis.isActivo());
    }

    // OHU28: con traslados pendientes no se da de baja y se dice qué falta
    @Test
    void desactivar_responde409_cuandoTieneTrasladosPendientes() {
        when(repositorio.findById(2L)).thenReturn(Optional.of(luis));
        when(traslados.countByResponsableIdAndEstadoIn(eq(2L), any())).thenReturn(2L);
        when(revisiones.countByMecanicoIdAndEstadoIn(eq(2L), any())).thenReturn(0L);
        when(mantenimientos.countByMecanicoIdAndEstadoIn(eq(2L), any())).thenReturn(0L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.desactivar(2L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("2 traslados"));
        assertTrue(luis.isActivo());
    }

    @Test
    void desactivar_juntaTodoLoPendiente_enElMensaje() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(rafael));
        when(traslados.countByResponsableIdAndEstadoIn(eq(1L), any())).thenReturn(0L);
        when(revisiones.countByMecanicoIdAndEstadoIn(eq(1L), any())).thenReturn(1L);
        when(mantenimientos.countByMecanicoIdAndEstadoIn(eq(1L), any())).thenReturn(3L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.desactivar(1L));

        assertEquals("No se puede dar de baja: tiene pendientes 1 revisión y 3 mantenimientos. "
                + "Reasígnalos o termínalos primero.", ex.getReason());
        assertTrue(rafael.isActivo());
    }
}