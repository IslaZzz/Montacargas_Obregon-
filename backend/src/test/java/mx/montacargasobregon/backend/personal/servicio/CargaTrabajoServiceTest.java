package mx.montacargasobregon.backend.personal.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.personal.dto.CargaPorMecanico;
import mx.montacargasobregon.backend.personal.dto.MecanicoCargaResponse;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;

@ExtendWith(MockitoExtension.class)
class CargaTrabajoServiceTest {

    @Mock
    private PersonalRepository personalRepositorio;

    @Mock
    private MantenimientoRepository mantenimientoRepositorio;

    @Mock
    private RevisionRepository revisionRepositorio;

    private CargaTrabajoService servicio;

    private Personal rafael;
    private Personal martin;

    @BeforeEach
    void setUp() {
        servicio = new CargaTrabajoService(personalRepositorio, mantenimientoRepositorio, revisionRepositorio);

        rafael = new Personal("MEC-10", "Rafael", "Amaya", null, TipoPersonal.MECANICO);
        ReflectionTestUtils.setField(rafael, "id", 1L);

        martin = new Personal("MEC-11", "Martín", "Cota", null, TipoPersonal.MECANICO);
        ReflectionTestUtils.setField(martin, "id", 2L);

        when(personalRepositorio.findByTipoAndActivoTrue(TipoPersonal.MECANICO))
                .thenReturn(List.of(rafael, martin));
    }

    @Test
    void mecanicosConCarga_sumaRevisionesYMantenimientosDelMismoMecanico() {
        when(mantenimientoRepositorio.contarPorMecanico(any()))
                .thenReturn(List.of(new CargaPorMecanico(1L, 2L)));
        when(revisionRepositorio.contarPorMecanico(any()))
                .thenReturn(List.of(new CargaPorMecanico(1L, 1L), new CargaPorMecanico(2L, 1L)));

        List<MecanicoCargaResponse> mecanicos = servicio.mecanicosConCarga();

        assertEquals(2, mecanicos.size());
        assertEquals("Rafael Amaya", mecanicos.get(0).nombre());
        assertEquals(3, mecanicos.get(0).equiposActivos());
        assertEquals("Martín Cota", mecanicos.get(1).nombre());
        assertEquals(1, mecanicos.get(1).equiposActivos());
    }

    @Test
    void mecanicosConCarga_muestraCero_cuandoNoTieneTrabajo() {
        when(mantenimientoRepositorio.contarPorMecanico(any())).thenReturn(List.of());
        when(revisionRepositorio.contarPorMecanico(any())).thenReturn(List.of());

        List<MecanicoCargaResponse> mecanicos = servicio.mecanicosConCarga();

        assertEquals("MEC-10", mecanicos.get(0).codigo());
        assertEquals(0, mecanicos.get(0).equiposActivos());
        assertEquals(0, mecanicos.get(1).equiposActivos());
    }

    @Test
    void mecanicosConCarga_soloCuentaAsignadosYEnProceso() {
        when(mantenimientoRepositorio.contarPorMecanico(any())).thenReturn(List.of());
        when(revisionRepositorio.contarPorMecanico(any())).thenReturn(List.of());

        servicio.mecanicosConCarga();

        verify(mantenimientoRepositorio).contarPorMecanico(
                List.of(EstadoMantenimiento.ASIGNADO, EstadoMantenimiento.EN_PROCESO));
        verify(revisionRepositorio).contarPorMecanico(
                List.of(EstadoRevision.ASIGNADO, EstadoRevision.EN_PROCESO));
    }
}