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
import java.time.LocalDate;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;


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

    @Test
    void terminar_loDejaCompletado() {
        mantenimiento.asignar(rafael);
        assertTrue(mantenimiento.puedeTerminarse());

        mantenimiento.terminar();

        assertEquals(EstadoMantenimiento.COMPLETADO, mantenimiento.getEstado());
        assertNotNull(mantenimiento.getFechaInicio());
        assertNotNull(mantenimiento.getFechaFin());
        assertFalse(mantenimiento.puedeTerminarse());
    }

    @Test
    void puedeTerminarse_esFalso_cuandoNoTieneMecanico() {
        assertFalse(mantenimiento.puedeTerminarse());
    }

    @Test
    void deRevision_quedaLigadoALaRevisionYLoReportaSuMecanico() {
        Cliente cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        Renta renta = RentaDePrueba.nueva("R-2091", cliente, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 20),
                "Calle Sufragio Efectivo 300", "Ramón Gutiérrez", "6441000001");
        Equipo equipo = new Equipo("MC-030", "SN-030", 2021, null);
        Revision revision = new Revision(renta.agregarEquipo(equipo), TipoRevision.SALIDA,
                LocalDateTime.of(2026, 11, 10, 8, 0));
        revision.asignar(rafael);

        Mantenimiento m = Mantenimiento.deRevision(revision, "Fuga de aceite", revision.getFechaProgramada());

        assertSame(revision, m.getRevision());
        assertSame(equipo, m.getEquipo());
        assertSame(rafael, m.getReportadoPor());
        assertEquals(TipoMantenimiento.CORRECTIVO, m.getTipo());
        assertEquals(EstadoMantenimiento.PENDIENTE, m.getEstado());
        assertEquals(revision.getFechaProgramada(), m.getFechaLimite());
    }
}