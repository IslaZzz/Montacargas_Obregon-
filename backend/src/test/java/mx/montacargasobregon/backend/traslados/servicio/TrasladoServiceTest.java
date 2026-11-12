package mx.montacargasobregon.backend.traslados.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;

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
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.dto.AsignarTrasladoRequest;
import mx.montacargasobregon.backend.traslados.dto.OperadorDisponibilidadResponse;
import mx.montacargasobregon.backend.traslados.dto.TrasladoResponse;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

@ExtendWith(MockitoExtension.class)
class TrasladoServiceTest {

    @Mock
    private TrasladoRepository repositorio;

    @Mock
    private PersonalRepository personalRepositorio;

    @Mock
    private AvanceRentaService avanceRenta;

    private TrasladoService servicio;

    private Cliente cliente;
    private Renta renta;
    private Personal operadorLuis;
    private Personal operadorAna;
    private Traslado trasladoEntrega;

    @BeforeEach
    void setUp() {
        servicio = new TrasladoService(repositorio, personalRepositorio, avanceRenta);

        cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(cliente, "id", 1L);

        renta = RentaDePrueba.nueva("R-2046", cliente, LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 20),
                "Blvd. Rodolfo Elías Calles 1520", "Ramón Gutiérrez", "6441000001");
        ReflectionTestUtils.setField(renta, "id", 10L);

        operadorLuis = new Personal("OPT-10", "Luis", "Valenzuela", null, TipoPersonal.OP_TRASLADO);
        ReflectionTestUtils.setField(operadorLuis, "id", 100L);

        operadorAna = new Personal("OPT-11", "Ana", "Soto", null, TipoPersonal.OP_TRASLADO);
        ReflectionTestUtils.setField(operadorAna, "id", 101L);

        trasladoEntrega = new Traslado("TR-0086", renta, TipoTraslado.ENTREGA,
                LocalDateTime.of(2026, 10, 7, 8, 0),
                LocalDateTime.of(2026, 10, 7, 9, 30));
        ReflectionTestUtils.setField(trasladoEntrega, "id", 200L);
    }

    @Test
    void asignar_exitoso_cuandoNoHayEmpalmes() {
        when(repositorio.findById(200L)).thenReturn(Optional.of(trasladoEntrega));
        when(personalRepositorio.findById(101L)).thenReturn(Optional.of(operadorAna));
        when(repositorio.buscarEmpalmes(eq(101L), any(), any(), eq(200L), eq(EstadoTraslado.CANCELADO)))
                .thenReturn(List.of());
        when(repositorio.save(any(Traslado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrasladoResponse response = servicio.asignar(200L, new AsignarTrasladoRequest(101L));

        assertNotNull(response);
        assertEquals("Ana Soto", response.responsableNombre());
        assertEquals("AS", response.responsableIniciales());
        verify(repositorio).save(trasladoEntrega);
    }

    @Test
    void asignar_rechazaConConflicto_cuandoOperadorTieneEmpalmeDeHorario() {
        when(repositorio.findById(200L)).thenReturn(Optional.of(trasladoEntrega));
        when(personalRepositorio.findById(100L)).thenReturn(Optional.of(operadorLuis));

        Traslado conflicto = new Traslado("TR-0093", renta, TipoTraslado.RECOLECCION,
                LocalDateTime.of(2026, 10, 7, 8, 30),
                LocalDateTime.of(2026, 10, 7, 10, 0));
        ReflectionTestUtils.setField(conflicto, "id", 201L);

        when(repositorio.buscarEmpalmes(eq(100L), any(), any(), eq(200L), eq(EstadoTraslado.CANCELADO)))
                .thenReturn(List.of(conflicto));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                servicio.asignar(200L, new AsignarTrasladoRequest(100L)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Conflicto de horario"));
        assertTrue(ex.getReason().contains("TR-0093"));
    }

    @Test
    void asignar_rechazaConConflicto_cuandoTrasladoYaInicio() {
        trasladoEntrega.iniciar();
        when(repositorio.findById(200L)).thenReturn(Optional.of(trasladoEntrega));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                servicio.asignar(200L, new AsignarTrasladoRequest(101L)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("ya inició"));
    }

    @Test
    void consultarDisponibilidad_muestraOcupadoConDetalle_cuandoTieneEmpalme() {
        when(repositorio.findById(200L)).thenReturn(Optional.of(trasladoEntrega));
        when(personalRepositorio.findByTipoAndActivoTrue(TipoPersonal.OP_TRASLADO))
                .thenReturn(List.of(operadorLuis, operadorAna));

        Traslado conflicto = new Traslado("TR-0093", renta, TipoTraslado.RECOLECCION,
                LocalDateTime.of(2026, 10, 7, 8, 30),
                LocalDateTime.of(2026, 10, 7, 10, 0));
        ReflectionTestUtils.setField(conflicto, "id", 201L);

        when(repositorio.buscarEmpalmes(eq(100L), any(), any(), eq(200L), eq(EstadoTraslado.CANCELADO)))
                .thenReturn(List.of(conflicto));
        when(repositorio.buscarEmpalmes(eq(101L), any(), any(), eq(200L), eq(EstadoTraslado.CANCELADO)))
                .thenReturn(List.of());
        when(repositorio.buscarPorOperadorYRango(eq(101L), any(), any(), eq(EstadoTraslado.CANCELADO)))
                .thenReturn(List.of());

        List<OperadorDisponibilidadResponse> disponibilidad =
                servicio.consultarDisponibilidad(200L, null, null);

        assertEquals(2, disponibilidad.size());

        // Ana debe estar primero por disponibilidad
        OperadorDisponibilidadResponse dispAna = disponibilidad.stream()
                .filter(d -> d.id().equals(101L))
                .findFirst()
                .orElseThrow();
        assertTrue(dispAna.disponible());
        assertEquals("Sin traslados ese día", dispAna.motivo());

        OperadorDisponibilidadResponse dispLuis = disponibilidad.stream()
                .filter(d -> d.id().equals(100L))
                .findFirst()
                .orElseThrow();
        assertFalse(dispLuis.disponible());
        assertTrue(dispLuis.motivo().contains("Tiene TR-0093"));
    }

    @Test
    void listar_filtraCorrectamentePorTipoYFecha() {
        Traslado recoleccion = new Traslado("TR-0089", renta, TipoTraslado.RECOLECCION,
                LocalDateTime.of(2026, 10, 8, 9, 0),
                LocalDateTime.of(2026, 10, 8, 11, 0));
        ReflectionTestUtils.setField(recoleccion, "id", 202L);

        when(repositorio.listarTodosConRelaciones())
                .thenReturn(List.of(trasladoEntrega, recoleccion));

        // Filtro SIN_ASIGNAR (ambos sin asignar)
        List<TrasladoResponse> sinAsignar = servicio.listar("SIN_ASIGNAR", null, null);
        assertEquals(2, sinAsignar.size());

        // Filtro ENTREGAS
        List<TrasladoResponse> entregas = servicio.listar("ENTREGAS", null, null);
        assertEquals(1, entregas.size());
        assertEquals("TR-0086", entregas.get(0).folio());

        // Filtro RECOLECCIONES
        List<TrasladoResponse> recolecciones = servicio.listar("RECOLECCIONES", null, null);
        assertEquals(1, recolecciones.size());
        assertEquals("TR-0089", recolecciones.get(0).folio());

        // Filtro por fecha: solo 8 oct
        List<TrasladoResponse> porFecha = servicio.listar(null, LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 8));
        assertEquals(1, porFecha.size());
        assertEquals("TR-0089", porFecha.get(0).folio());
    }

    @Test
    void completar_marcaRealizadoYHaceAvanzarLaRenta() {
        trasladoEntrega.asignarResponsable(operadorLuis);
        when(repositorio.findById(200L)).thenReturn(Optional.of(trasladoEntrega));

        TrasladoResponse respuesta = servicio.completar(200L);

        assertEquals(EstadoTraslado.COMPLETADO, respuesta.estado());
        assertNotNull(trasladoEntrega.getFechaFinReal());
        verify(avanceRenta).trasladoRealizado(trasladoEntrega);
    }

    @Test
    void completar_rechazaConConflicto_cuandoNoTieneResponsable() {
        when(repositorio.findById(200L)).thenReturn(Optional.of(trasladoEntrega));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> servicio.completar(200L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("TR-0086"));
        verifyNoInteractions(avanceRenta);
    }

    @Test
    void completar_rechazaConConflicto_cuandoYaSeRealizo() {
        trasladoEntrega.asignarResponsable(operadorLuis);
        trasladoEntrega.completar();
        when(repositorio.findById(200L)).thenReturn(Optional.of(trasladoEntrega));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> servicio.completar(200L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(avanceRenta);
    }

    @Test
    void completar_rechazaLaRecoleccion_mientrasNoSeHagaLaEntrega() {
        Traslado recoleccion = new Traslado("TR-0087", renta, TipoTraslado.RECOLECCION,
                LocalDateTime.of(2026, 10, 20, 14, 0),
                LocalDateTime.of(2026, 10, 20, 16, 0));
        recoleccion.asignarResponsable(operadorAna);
        when(repositorio.findById(201L)).thenReturn(Optional.of(recoleccion));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> servicio.completar(201L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("R-2046"));
        assertEquals(EstadoTraslado.PROGRAMADO, recoleccion.getEstado());
        verifyNoInteractions(avanceRenta);
    }

    @Test
    void completar_responde404_cuandoNoExiste() {
        when(repositorio.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> servicio.completar(999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
