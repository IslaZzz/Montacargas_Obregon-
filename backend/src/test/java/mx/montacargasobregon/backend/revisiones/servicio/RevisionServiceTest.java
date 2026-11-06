package mx.montacargasobregon.backend.revisiones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.servicio.PersonalService;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.revisiones.dto.AsignarRevisionesRequest;
import mx.montacargasobregon.backend.revisiones.dto.RevisionesRentaResponse;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;

@ExtendWith(MockitoExtension.class)
class RevisionServiceTest {

    @Mock
    private RevisionRepository repositorio;

    @Mock
    private PersonalService personalServicio;

    private RevisionService servicio;

    private Personal rafael;
    private Renta renta;
    private Revision salidaMc030;
    private Revision salidaMc031;

    @BeforeEach
    void setUp() {
        servicio = new RevisionService(repositorio, personalServicio);

        rafael = new Personal("MEC-10", "Rafael", "Amaya", null, TipoPersonal.MECANICO);
        ReflectionTestUtils.setField(rafael, "id", 1L);

        Categoria categoria = new Categoria("Montacargas");
        Modelo modelo = new Modelo("8FGU25", "Toyota", "TOY-8FGU25", null, new BigDecimal("1500"), categoria);

        Cliente cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        renta = new Renta("R-2070", cliente, LocalDate.of(2026, 10, 22), LocalDate.of(2026, 11, 5),
                "Blvd. Rodolfo Elías Calles 1520", "Ramón Gutiérrez", "6441000001");
        ReflectionTestUtils.setField(renta, "id", 20L);

        LocalDateTime salida = LocalDateTime.of(2026, 10, 22, 7, 0);
        salidaMc030 = new Revision(renta.agregarEquipo(new Equipo("MC-030", "SN-030", 2021, modelo)),
                TipoRevision.SALIDA, salida);
        ReflectionTestUtils.setField(salidaMc030, "id", 30L);

        salidaMc031 = new Revision(renta.agregarEquipo(new Equipo("MC-031", "SN-031", 2021, modelo)),
                TipoRevision.SALIDA, salida);
        ReflectionTestUtils.setField(salidaMc031, "id", 31L);
    }

    @Test
    void listarPorRenta_agrupaPorRentaYTipo() {
        Revision entradaMc030 = new Revision(salidaMc030.getEquipoRenta(), TipoRevision.ENTRADA,
                LocalDateTime.of(2026, 11, 5, 18, 0));
        ReflectionTestUtils.setField(entradaMc030, "id", 32L);
        when(repositorio.listar()).thenReturn(List.of(salidaMc030, salidaMc031, entradaMc030));

        List<RevisionesRentaResponse> grupos = servicio.listarPorRenta();

        assertEquals(2, grupos.size());
        assertEquals("20-SALIDA", grupos.get(0).clave());
        assertEquals(2, grupos.get(0).total());
        assertEquals("Industrias Cajeme", grupos.get(0).cliente());
        assertEquals("20-ENTRADA", grupos.get(1).clave());
        assertEquals(1, grupos.get(1).total());
    }

    @Test
    void asignar_exitoso_cuandoSeAsignaLaRentaCompleta() {
        when(personalServicio.mecanicoActivo(1L)).thenReturn(rafael);
        when(repositorio.findAllById(Set.of(30L, 31L))).thenReturn(List.of(salidaMc030, salidaMc031));
        when(repositorio.listar()).thenReturn(List.of(salidaMc030, salidaMc031));

        List<RevisionesRentaResponse> grupos =
                servicio.asignar(new AsignarRevisionesRequest(List.of(30L, 31L), 1L));

        assertEquals(1, grupos.size());
        RevisionesRentaResponse grupo = grupos.get(0);
        assertEquals("ASIGNADO", grupo.estado());
        assertEquals("Rafael Amaya", grupo.revisiones().get(0).mecanicoNombre());
        assertEquals("Rafael Amaya", grupo.revisiones().get(1).mecanicoNombre());
    }

    @Test
    void asignar_responde404_cuandoUnaRevisionNoExiste() {
        when(personalServicio.mecanicoActivo(1L)).thenReturn(rafael);
        when(repositorio.findAllById(Set.of(30L, 99L))).thenReturn(List.of(salidaMc030));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.asignar(new AsignarRevisionesRequest(List.of(30L, 99L), 1L)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNull(salidaMc030.getMecanico());
        verify(repositorio, never()).listar();
    }

    @Test
    void asignar_rechazaConConflictoYNoAsignaNinguna_cuandoUnaYaTermino() {
        ReflectionTestUtils.setField(salidaMc031, "estado", EstadoRevision.COMPLETADA);
        when(personalServicio.mecanicoActivo(1L)).thenReturn(rafael);
        when(repositorio.findAllById(Set.of(30L, 31L))).thenReturn(List.of(salidaMc030, salidaMc031));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.asignar(new AsignarRevisionesRequest(List.of(30L, 31L), 1L)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("MC-031"));
        // La que sí se podía asignar tampoco se asignó
        assertEquals(EstadoRevision.PENDIENTE, salidaMc030.getEstado());
        assertNull(salidaMc030.getMecanico());
    }
}