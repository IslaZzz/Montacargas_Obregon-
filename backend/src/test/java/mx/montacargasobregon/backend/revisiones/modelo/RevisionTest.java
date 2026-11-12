package mx.montacargasobregon.backend.revisiones.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

class RevisionTest {

    private Personal rafael;
    private Personal martin;
    private Revision revision;

    @BeforeEach
    void setUp() {
        rafael = new Personal("MEC-10", "Rafael", "Amaya", null, TipoPersonal.MECANICO);
        martin = new Personal("MEC-11", "Martín", "Cota", null, TipoPersonal.MECANICO);

        Cliente cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        Renta renta = new Renta("R-2070", cliente, LocalDate.of(2026, 10, 22), LocalDate.of(2026, 11, 5),
                "Blvd. Rodolfo Elías Calles 1520", "Ramón Gutiérrez", "6441000001");
        EquipoRenta equipoRenta = renta.agregarEquipo(new Equipo("MC-030", "SN-030", 2021, null));

        revision = new Revision(equipoRenta, TipoRevision.SALIDA, LocalDateTime.of(2026, 10, 22, 7, 0));
    }

    @Test
    void nueva_quedaPendienteSinMecanicoNiResultado() {
        assertEquals(EstadoRevision.PENDIENTE, revision.getEstado());
        assertNull(revision.getMecanico());
        assertNull(revision.getResultado());
        assertTrue(revision.puedeAsignarse());
    }

    @Test
    void asignar_pendiente_pasaAAsignadoConMecanicoYFecha() {
        revision.asignar(rafael);

        assertEquals(EstadoRevision.ASIGNADO, revision.getEstado());
        assertSame(rafael, revision.getMecanico());
        assertNotNull(revision.getFechaAsignacion());
    }

    @Test
    void asignar_enProceso_cambiaDeMecanicoYConservaElEstado() {
        revision.asignar(rafael);
        ReflectionTestUtils.setField(revision, "estado", EstadoRevision.EN_PROCESO);

        revision.asignar(martin);

        assertEquals(EstadoRevision.EN_PROCESO, revision.getEstado());
        assertSame(martin, revision.getMecanico());
    }

    @Test
    void puedeAsignarse_esFalso_cuandoEstaCompletada() {
        ReflectionTestUtils.setField(revision, "estado", EstadoRevision.COMPLETADA);
        assertFalse(revision.puedeAsignarse());
    }

    @Test
    void puedeAsignarse_esFalso_cuandoEstaCancelada() {
        ReflectionTestUtils.setField(revision, "estado", EstadoRevision.CANCELADA);
        assertFalse(revision.puedeAsignarse());
    }


    @Test
    void registrarResultado_terminaLaRevisionConLoQueReportoElMecanico() {
        revision.asignar(rafael);
        assertTrue(revision.puedeRegistrarResultado());

        revision.registrarResultado(ResultadoRevision.CON_OBSERVACIONES, "Rayón en la cabina");

        assertEquals(EstadoRevision.COMPLETADA, revision.getEstado());
        assertEquals(ResultadoRevision.CON_OBSERVACIONES, revision.getResultado());
        assertEquals("Rayón en la cabina", revision.getNotas());
        assertNotNull(revision.getFechaInicio());
        assertNotNull(revision.getFechaFin());
        assertFalse(revision.puedeRegistrarResultado());
    }

    @Test
    void puedeRegistrarResultado_esFalso_cuandoNoTieneMecanico() {
        assertFalse(revision.puedeRegistrarResultado());
    }
}