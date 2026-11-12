package mx.montacargasobregon.backend.operacion.inicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.TipoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.operacion.inicio.InicioResponse.Aviso;
import mx.montacargasobregon.backend.operacion.inicio.InicioResponse.Operacion;
import mx.montacargasobregon.backend.operacion.inicio.InicioResponse.TipoAviso;
import mx.montacargasobregon.backend.operacion.inicio.InicioResponse.TipoOperacion;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.rentas.repositorio.RentaRepository;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

@ExtendWith(MockitoExtension.class)
class InicioServiceTest {

    private static final LocalDate HOY = LocalDate.of(2030, 10, 10);
    private static final LocalDateTime AHORA = HOY.atTime(12, 0);

    @Mock
    private TrasladoRepository trasladoRepositorio;

    @Mock
    private RevisionRepository revisionRepositorio;

    @Mock
    private MantenimientoRepository mantenimientoRepositorio;

    @Mock
    private RentaRepository rentaRepositorio;

    private InicioService servicio;

    private Cliente cliente;
    private Modelo modelo;
    private Personal operador;

    @BeforeEach
    void setUp() {
        servicio = new InicioService(trasladoRepositorio, revisionRepositorio, mantenimientoRepositorio, rentaRepositorio);

        cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        modelo = new Modelo("8FGU25", "Toyota", "TOY-8FGU25", null, new BigDecimal("1500"),
                new Categoria("Montacargas"));
        operador = new Personal("OPT-10", "Luis", "Valenzuela", null, TipoPersonal.OP_TRASLADO);
    }

    private Renta renta(Long id, String folio, LocalDate inicio, LocalDate fin) {
        Renta r = RentaDePrueba.nueva(folio, cliente, inicio, fin, "Calle Sufragio Efectivo 300", "Ramón Gutiérrez", "6441000001");
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    private EquipoRenta equipo(Renta renta, String numeroUnidad, FaseEquipoRenta fase) {
        EquipoRenta er = renta.agregarEquipo(new Equipo(numeroUnidad, "SN-" + numeroUnidad, 2021, modelo));
        er.cambiarFase(fase);
        return er;
    }

    private void sinDatos() {
        when(trasladoRepositorio.listarTodosConRelaciones()).thenReturn(List.of());
        when(revisionRepositorio.listar()).thenReturn(List.of());
        when(mantenimientoRepositorio.listar()).thenReturn(List.of());
        when(rentaRepositorio.findAll()).thenReturn(List.of());
    }

    @Test
    void consultar_juntaPendientesAvisosYOperaciones() {
        // Programada con entrega hoy y un equipo todavía sin preparar
        Renta programada = renta(20L, "R-2091", HOY, HOY.plusDays(10));
        programada.confirmarEntrega(HOY.atTime(14, 0), HOY.atTime(16, 0));
        EquipoRenta listo = equipo(programada, "MC-030", FaseEquipoRenta.LISTO_ENTREGA);
        EquipoRenta reservado = equipo(programada, "MC-031", FaseEquipoRenta.RESERVADO);

        // En curso que termina mañana sin recolección
        Renta enCurso = renta(21L, "R-2092", HOY.minusDays(10), HOY.plusDays(1));
        enCurso.cambiarEstado(EstadoRenta.EN_CURSO);
        EquipoRenta conCliente = equipo(enCurso, "MC-032", FaseEquipoRenta.EN_OPERACION);

        // Entrega de esta mañana sin marcar, recolección de la tarde sin operador y una ya realizada
        Traslado atrasado = new Traslado("TR-0001", enCurso, TipoTraslado.ENTREGA, HOY.atTime(8, 0), HOY.atTime(10, 0));
        atrasado.asignarResponsable(operador);
        Traslado sinOperador = new Traslado("TR-0002", enCurso, TipoTraslado.RECOLECCION,
                HOY.atTime(15, 0), HOY.atTime(17, 0));
        Traslado realizado = new Traslado("TR-0003", enCurso, TipoTraslado.ENTREGA,
                HOY.plusDays(1).atTime(8, 0), HOY.plusDays(1).atTime(10, 0));
        realizado.completar();

        // Dos revisiones de salida de la misma renta y una ya terminada
        Revision salida1 = new Revision(reservado, TipoRevision.SALIDA, HOY.atTime(9, 0));
        Revision salida2 = new Revision(listo, TipoRevision.SALIDA, HOY.atTime(9, 0));
        Revision terminada = new Revision(conCliente, TipoRevision.SALIDA, HOY.atTime(9, 0));
        ReflectionTestUtils.setField(terminada, "estado", EstadoRevision.COMPLETADA);

        // Un mantenimiento vencido, uno sin fecha y uno terminado
        Mantenimiento vencido = new Mantenimiento(listo.getEquipo(), TipoMantenimiento.CORRECTIVO,
                "Fuga de aceite", null, HOY.minusDays(1).atTime(9, 0));
        Mantenimiento sinFecha = new Mantenimiento(conCliente.getEquipo(), TipoMantenimiento.CORRECTIVO,
                "Llanta baja", null, null);
        Mantenimiento listoMant = new Mantenimiento(conCliente.getEquipo(), TipoMantenimiento.CORRECTIVO,
                "Batería", null, HOY.atTime(9, 0));
        ReflectionTestUtils.setField(listoMant, "estado", EstadoMantenimiento.COMPLETADO);

        when(trasladoRepositorio.listarTodosConRelaciones()).thenReturn(List.of(atrasado, sinOperador, realizado));
        when(revisionRepositorio.listar()).thenReturn(List.of(salida1, salida2, terminada));
        when(mantenimientoRepositorio.listar()).thenReturn(List.of(vencido, sinFecha, listoMant));
        when(rentaRepositorio.findAll()).thenReturn(List.of(programada, enCurso));

        InicioResponse inicio = servicio.consultar(AHORA);

        assertEquals(HOY, inicio.hoy());
        assertEquals(new InicioResponse.Pendientes(1, 1, 2, 2), inicio.pendientes());

        assertEquals(List.of(
                        TipoAviso.TRASLADO_ATRASADO,
                        TipoAviso.TRASLADO_SIN_OPERADOR,
                        TipoAviso.ENTREGA_EN_RIESGO,
                        TipoAviso.MANTENIMIENTO_VENCIDO,
                        TipoAviso.FALTA_RECOLECCION),
                inicio.avisos().stream().map(Aviso::tipo).toList());
        Aviso atrasadoAviso = inicio.avisos().get(0);
        assertEquals("Entrega sin marcar realizada - TR-0001", atrasadoAviso.titulo());
        assertEquals("Renta R-2092 · era hoy de 08:00 a 10:00", atrasadoAviso.detalle());
        assertEquals("/traslados", atrasadoAviso.ruta());
        Aviso riesgo = inicio.avisos().get(2);
        assertEquals("Entrega hoy - Renta R-2091", riesgo.titulo());
        assertEquals("Falta 1 de 2 equipos por preparar · 14:00 a 16:00", riesgo.detalle());
        assertEquals(20L, riesgo.rentaId());
        assertEquals("Mantenimiento vencido - MC-030", inicio.avisos().get(3).titulo());
        assertEquals(21L, inicio.avisos().get(4).rentaId());

        // Desde hoy y por fecha: la entrega atrasada de hoy, las revisiones juntas y la recolección
        List<Operacion> operaciones = inicio.operaciones();
        assertEquals(List.of(TipoOperacion.ENTREGA, TipoOperacion.REVISION, TipoOperacion.RECOLECCION),
                operaciones.stream().map(Operacion::tipo).toList());
        assertEquals("Revisión de salida", operaciones.get(1).titulo());
        assertEquals("Renta R-2091 · 2 equipos", operaciones.get(1).detalle());
        assertTrue(operaciones.get(2).detalle().endsWith("Sin operador"));
    }

    @Test
    void consultar_sinNada_noMuestraAvisos() {
        sinDatos();

        InicioResponse inicio = servicio.consultar(AHORA);

        assertEquals(new InicioResponse.Pendientes(0, 0, 0, 0), inicio.pendientes());
        assertTrue(inicio.avisos().isEmpty());
        assertTrue(inicio.operaciones().isEmpty());
    }

    @Test
    void consultar_avisaQueElMantenimientoVenceHoy() {
        Equipo equipo = new Equipo("MC-040", "SN-040", 2021, modelo);
        Mantenimiento hoy = new Mantenimiento(equipo, TipoMantenimiento.CORRECTIVO, "Freno", null, HOY.atTime(18, 0));
        when(trasladoRepositorio.listarTodosConRelaciones()).thenReturn(List.of());
        when(revisionRepositorio.listar()).thenReturn(List.of());
        when(mantenimientoRepositorio.listar()).thenReturn(List.of(hoy));
        when(rentaRepositorio.findAll()).thenReturn(List.of());

        InicioResponse inicio = servicio.consultar(AHORA);

        assertEquals("Mantenimiento vence hoy - MC-040", inicio.avisos().get(0).titulo());
        assertEquals("Freno · Sin mecánico", inicio.avisos().get(0).detalle());
        assertEquals(TipoOperacion.MANTENIMIENTO, inicio.operaciones().get(0).tipo());
    }
}