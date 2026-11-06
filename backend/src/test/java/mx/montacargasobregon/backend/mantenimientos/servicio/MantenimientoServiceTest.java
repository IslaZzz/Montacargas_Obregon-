package mx.montacargasobregon.backend.mantenimientos.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.mantenimientos.dto.AsignarRequest;
import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.TipoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.servicio.PersonalService;

@ExtendWith(MockitoExtension.class)
class MantenimientoServiceTest {

    @Mock
    private MantenimientoRepository repositorio;

    @Mock
    private PersonalService personalServicio;

    private MantenimientoService servicio;

    private Personal rafael;
    private Mantenimiento fuga;
    private Mantenimiento frenos;

    @BeforeEach
    void setUp() {
        servicio = new MantenimientoService(repositorio, personalServicio);

        rafael = new Personal("MEC-10", "Rafael", "Amaya", null, TipoPersonal.MECANICO);
        ReflectionTestUtils.setField(rafael, "id", 1L);

        fuga = new Mantenimiento(new Equipo("MC-014", "SN-014", 2020, null), TipoMantenimiento.CORRECTIVO,
                "Fuga de aceite hidráulico", null, LocalDateTime.of(2026, 10, 22, 8, 0));
        ReflectionTestUtils.setField(fuga, "id", 10L);

        frenos = new Mantenimiento(new Equipo("MC-021", "SN-021", 2019, null), TipoMantenimiento.CORRECTIVO,
                "Frenos sin respuesta", null, LocalDateTime.of(2026, 10, 23, 9, 0));
        ReflectionTestUtils.setField(frenos, "id", 11L);
    }

    @Test
    void asignar_exitoso_cuandoTodosEstanPendientes() {
        when(personalServicio.mecanicoActivo(1L)).thenReturn(rafael);
        when(repositorio.findAllById(Set.of(10L, 11L))).thenReturn(List.of(fuga, frenos));

        List<Mantenimiento> asignados = servicio.asignar(new AsignarRequest(List.of(10L, 11L), 1L));

        assertEquals(2, asignados.size());
        assertEquals(EstadoMantenimiento.ASIGNADO, fuga.getEstado());
        assertEquals(EstadoMantenimiento.ASIGNADO, frenos.getEstado());
        assertSame(rafael, fuga.getMecanico());
        assertSame(rafael, frenos.getMecanico());
    }

    @Test
    void asignar_ignoraIdsRepetidos() {
        when(personalServicio.mecanicoActivo(1L)).thenReturn(rafael);
        when(repositorio.findAllById(Set.of(10L))).thenReturn(List.of(fuga));

        List<Mantenimiento> asignados = servicio.asignar(new AsignarRequest(List.of(10L, 10L), 1L));

        assertEquals(1, asignados.size());
        assertSame(rafael, fuga.getMecanico());
    }

    @Test
    void asignar_responde404_cuandoUnMantenimientoNoExiste() {
        when(personalServicio.mecanicoActivo(1L)).thenReturn(rafael);
        when(repositorio.findAllById(Set.of(10L, 99L))).thenReturn(List.of(fuga));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.asignar(new AsignarRequest(List.of(10L, 99L), 1L)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNull(fuga.getMecanico());
    }

    @Test
    void asignar_rechazaConConflictoYNoAsignaNinguno_cuandoUnoYaTermino() {
        ReflectionTestUtils.setField(frenos, "estado", EstadoMantenimiento.COMPLETADO);
        when(personalServicio.mecanicoActivo(1L)).thenReturn(rafael);
        when(repositorio.findAllById(Set.of(10L, 11L))).thenReturn(List.of(fuga, frenos));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.asignar(new AsignarRequest(List.of(10L, 11L), 1L)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("MC-021"));
        // El que sí se podía asignar tampoco se asignó
        assertEquals(EstadoMantenimiento.PENDIENTE, fuga.getEstado());
        assertNull(fuga.getMecanico());
    }

    @Test
    void asignar_noBuscaMantenimientos_cuandoElMecanicoNoEsValido() {
        when(personalServicio.mecanicoActivo(2L)).thenThrow(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "La persona elegida no es un mecánico activo"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.asignar(new AsignarRequest(List.of(10L), 2L)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(repositorio);
    }
}