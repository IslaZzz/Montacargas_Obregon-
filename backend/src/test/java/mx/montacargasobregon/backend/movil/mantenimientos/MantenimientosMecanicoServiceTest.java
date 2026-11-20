package mx.montacargasobregon.backend.movil.mantenimientos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.CapturaCampo;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.DefinicionCampo;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.equipos.modelo.TipoCampo;
import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoTarea;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.OrigenTarea;
import mx.montacargasobregon.backend.mantenimientos.modelo.TareaMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.TipoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.mantenimientos.servicio.MantenimientoService;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoInspeccion;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.ValorRevision;

@ExtendWith(MockitoExtension.class)
class MantenimientosMecanicoServiceTest {

    @Mock
    private MantenimientoRepository repositorio;

    @Mock
    private MantenimientoService mantenimientoServicio;

    private MantenimientosMecanicoService servicio;

    private final Personal rafael = personal(1L, "MEC-10");
    private final PersonalActual actual = PersonalActual.de(rafael);
    private Mantenimiento mantenimiento;

    @BeforeEach
    void setUp() {
        servicio = new MantenimientosMecanicoService(repositorio, mantenimientoServicio);

        // Una revisión de salida con una falla y una inspección en atención
        Categoria montacargas = new Categoria("Montacargas");
        Modelo modelo = new Modelo("8FGU25", "Toyota", "TOY-8FGU25", null, new BigDecimal("1500"), montacargas);
        Cliente cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        Renta renta = RentaDePrueba.nueva("R-2070", cliente, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 30),
                "Blvd. Rodolfo Elías Calles 1520", "Ramón Gutiérrez", "6441000001");
        Revision revision = new Revision(renta.agregarEquipo(new Equipo("EQ-017", "SN-017", 2021, modelo)),
                TipoRevision.SALIDA, LocalDateTime.of(2026, 11, 20, 8, 0));
        revision.asignar(rafael);
        revision.registrarValores(List.of(
                new ValorRevision(campo(montacargas, "Horómetro", TipoCampo.DECIMAL, 1), "1284.5", null, null),
                new ValorRevision(campo(montacargas, "Fluidos y fugas", TipoCampo.INSPECCION, 2), null,
                        EstadoInspeccion.FALLA, "Fuga en manguera hidráulica"),
                new ValorRevision(campo(montacargas, "Llantas", TipoCampo.INSPECCION, 3), null,
                        EstadoInspeccion.ATENCION, "Desgaste en llanta delantera derecha"),
                new ValorRevision(campo(montacargas, "Seguridad", TipoCampo.INSPECCION, 4), null,
                        EstadoInspeccion.BIEN, null)), null);

        mantenimiento = Mantenimiento.deRevision(revision, "Fluidos y fugas: Fuga en manguera hidráulica",
                revision.getFechaProgramada());
        ReflectionTestUtils.setField(mantenimiento, "id", 70L);
        mantenimiento.asignar(rafael);
        long id = 100;
        for (TareaMantenimiento tarea : mantenimiento.getTareas()) {
            ReflectionTestUtils.setField(tarea, "id", id++);
        }
    }

    @Test
    void elMantenimientoTraeUnaTareaPorFallaYPorAtencion() {
        List<TareaMantenimiento> tareas = mantenimiento.getTareas();

        assertEquals(2, tareas.size());
        assertEquals("Fluidos y fugas", tareas.get(0).getTitulo());
        assertEquals("Fuga en manguera hidráulica", tareas.get(0).getHallazgo());
        assertEquals("Llantas", tareas.get(1).getTitulo());
        assertEquals(OrigenTarea.ASIGNADA, tareas.get(1).getOrigen());
    }

    @Test
    void abrirloLoDejaEnCursoConLoQueSeAnotoEnLaRevision() {
        when(repositorio.findById(70L)).thenReturn(Optional.of(mantenimiento));

        DetalleMantenimientoMecanicoResponse detalle = servicio.iniciar(70L, actual);

        assertEquals(EstadoMantenimiento.EN_PROCESO, mantenimiento.getEstado());
        assertEquals(4, detalle.revision().size());
        assertEquals("1284.5", detalle.revision().get(0).valor());
        assertFalse(detalle.puedeFinalizar());
    }

    @Test
    void atenderTodasLasTareasPermiteFinalizar() {
        when(repositorio.findById(70L)).thenReturn(Optional.of(mantenimiento));

        servicio.atender(70L, 100L, actual, new AtenderTareaRequest("Cambio de manguera", "Manguera 1/2\"", "1 h", null));
        DetalleMantenimientoMecanicoResponse detalle =
                servicio.atender(70L, 101L, actual, new AtenderTareaRequest("Rotación de llantas", null, null, " "));

        assertEquals(2, detalle.atendidas());
        assertTrue(detalle.puedeFinalizar());
        assertNull(mantenimiento.getTareas().get(1).getObservaciones());

        servicio.finalizar(70L, actual);
        verify(mantenimientoServicio).terminar(70L);
    }

    @Test
    void noSeFinalizaConTareasPendientes() {
        when(repositorio.findById(70L)).thenReturn(Optional.of(mantenimiento));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.finalizar(70L, actual));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(mantenimientoServicio, never()).terminar(70L);
    }

    @Test
    void agregarUnaCategoriaLaDejaPendiente() {
        when(repositorio.findById(70L)).thenReturn(Optional.of(mantenimiento));

        servicio.agregar(70L, actual, new AgregarTareaRequest("Fuga de aceite", "Cárter", null));

        TareaMantenimiento agregada = mantenimiento.getTareas().get(2);
        assertEquals(OrigenTarea.AGREGADA, agregada.getOrigen());
        assertEquals(EstadoTarea.PENDIENTE, agregada.getEstado());
        assertEquals("Cárter", agregada.getComponente());
    }

    @Test
    void laIncidenciaDejaLaTareaPendienteConElAviso() {
        when(repositorio.findById(70L)).thenReturn(Optional.of(mantenimiento));

        servicio.reportarIncidencia(70L, 101L, actual, new IncidenciaRequest("No hay llanta en almacén"));

        TareaMantenimiento llanta = mantenimiento.getTareas().get(1);
        assertEquals(EstadoTarea.PENDIENTE, llanta.getEstado());
        assertEquals("No hay llanta en almacén", llanta.getIncidencia());
        assertEquals(EstadoMantenimiento.ASIGNADO, mantenimiento.getEstado());
    }

    @Test
    void unMantenimientoReportadoDesdeLaOficinaTomaLaFallaComoTarea() {
        Mantenimiento deOficina = new Mantenimiento(new Equipo("EQ-024", "SN-024", 2022, null),
                TipoMantenimiento.PREVENTIVO, "Servicio de 500 horas", null, null);
        deOficina.asignar(rafael);

        deOficina.iniciar();

        assertEquals(1, deOficina.getTareas().size());
        assertEquals("Servicio de 500 horas", deOficina.getTareas().get(0).getHallazgo());
    }

    private static DefinicionCampo campo(Categoria categoria, String nombre, TipoCampo tipo, int orden) {
        return new DefinicionCampo(categoria, nombre, tipo, null, true, orden, List.of(), CapturaCampo.REVISION);
    }

    private static Personal personal(Long id, String codigo) {
        Personal p = new Personal(codigo, "Rafael", "Amaya", null, TipoPersonal.MECANICO);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }
}