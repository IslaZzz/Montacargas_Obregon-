package mx.montacargasobregon.backend.mantenimientos.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;

class MantenimientoTest {

    private Personal rafael;
    private Personal martin;
    private Mantenimiento mantenimiento;

    @BeforeEach
    void setUp() {
        rafael = new Personal("MEC-10", "Rafael", "Amaya", null, TipoPersonal.MECANICO);
        martin = new Personal("MEC-11", "Martín", "Cota", null, TipoPersonal.MECANICO);

        Equipo equipo = new Equipo("MC-014", "SN-014", 2020, null);
        mantenimiento = new Mantenimiento(equipo, TipoMantenimiento.CORRECTIVO,
                "Fuga de aceite hidráulico", null, LocalDateTime.of(2026, 10, 22, 8, 0));
    }

    @Test
    void nuevo_quedaPendienteSinMecanico() {
        assertEquals(EstadoMantenimiento.PENDIENTE, mantenimiento.getEstado());
        assertNull(mantenimiento.getMecanico());
        assertNotNull(mantenimiento.getFechaReporte());
        assertTrue(mantenimiento.puedeAsignarse());
    }

    @Test
    void asignar_pendiente_pasaAAsignadoConMecanicoYFecha() {
        mantenimiento.asignar(rafael);

        assertEquals(EstadoMantenimiento.ASIGNADO, mantenimiento.getEstado());
        assertSame(rafael, mantenimiento.getMecanico());
        assertNotNull(mantenimiento.getFechaAsignacion());
    }

    @Test
    void asignar_yaAsignado_cambiaDeMecanicoYSigueAsignado() {
        mantenimiento.asignar(rafael);
        mantenimiento.asignar(martin);

        assertEquals(EstadoMantenimiento.ASIGNADO, mantenimiento.getEstado());
        assertSame(martin, mantenimiento.getMecanico());
    }

    @Test
    void asignar_enProceso_cambiaDeMecanicoYConservaElEstado() {
        mantenimiento.asignar(rafael);
        ReflectionTestUtils.setField(mantenimiento, "estado", EstadoMantenimiento.EN_PROCESO);

        mantenimiento.asignar(martin);

        assertEquals(EstadoMantenimiento.EN_PROCESO, mantenimiento.getEstado());
        assertSame(martin, mantenimiento.getMecanico());
    }

    @Test
    void puedeAsignarse_esFalso_cuandoEstaCompletado() {
        ReflectionTestUtils.setField(mantenimiento, "estado", EstadoMantenimiento.COMPLETADO);
        assertFalse(mantenimiento.puedeAsignarse());
    }

    @Test
    void puedeAsignarse_esFalso_cuandoEstaCancelado() {
        ReflectionTestUtils.setField(mantenimiento, "estado", EstadoMantenimiento.CANCELADO);
        assertFalse(mantenimiento.puedeAsignarse());
    }
}