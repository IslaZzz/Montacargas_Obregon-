package mx.montacargasobregon.backend.movil.revisiones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import mx.montacargasobregon.backend.equipos.repositorio.DefinicionCampoRepository;
import mx.montacargasobregon.backend.movil.fotos.FotoResponse;
import mx.montacargasobregon.backend.movil.fotos.FotoService;
import mx.montacargasobregon.backend.movil.fotos.OrigenFoto;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoInspeccion;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.ResultadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.ValorRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;
import mx.montacargasobregon.backend.revisiones.servicio.RevisionService;

@ExtendWith(MockitoExtension.class)
class RevisionesMecanicoServiceTest {

    @Mock
    private RevisionRepository repositorio;

    @Mock
    private RevisionService revisionServicio;

    @Mock
    private FotoService fotoServicio;

    @Mock
    private DefinicionCampoRepository campoRepositorio;

    private RevisionesMecanicoService servicio;

    private final Personal rafael = personal(1L, "MEC-10");
    private final PersonalActual actual = PersonalActual.de(rafael);
    private Revision revision;

    @BeforeEach
    void setUp() {
        servicio = new RevisionesMecanicoService(repositorio, revisionServicio, fotoServicio, campoRepositorio);

        Categoria montacargas = new Categoria("Montacargas");
        ReflectionTestUtils.setField(montacargas, "id", 1L);
        // Un campo del modelo (no debe salir en la revisión) y cinco de revisión
        lenient().when(campoRepositorio.findByCategoriaIdOrderByOrden(1L)).thenReturn(List.of(
                campo(montacargas, 9L, "Capacidad de carga", TipoCampo.ENTERO, "kg", List.of(), CapturaCampo.MODELO),
                campo(montacargas, 10L, "Horómetro", TipoCampo.DECIMAL, "h", List.of(), CapturaCampo.REVISION),
                campo(montacargas, 11L, "Combustible o batería", TipoCampo.LISTA, null,
                        List.of("1/4", "1/2", "3/4", "Lleno"), CapturaCampo.REVISION),
                campo(montacargas, 12L, "Llantas", TipoCampo.INSPECCION, null, List.of(), CapturaCampo.REVISION),
                campo(montacargas, 13L, "Frenos y dirección", TipoCampo.INSPECCION, null, List.of(), CapturaCampo.REVISION),
                campo(montacargas, 14L, "Foto de frente", TipoCampo.FOTO, null, List.of(), CapturaCampo.REVISION)));
        lenient().when(fotoServicio.listar(OrigenFoto.REVISION, 30L))
                .thenReturn(List.of(new FotoResponse(77L, "Foto de frente", "/api/movil/fotos/77", LocalDateTime.now())));

        Modelo modelo = new Modelo("8FGU25", "Toyota", "TOY-8FGU25", null, new BigDecimal("1500"), montacargas);
        Cliente cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        Renta renta = RentaDePrueba.nueva("R-2070", cliente, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 30),
                "Blvd. Rodolfo Elías Calles 1520", "Ramón Gutiérrez", "6441000001");
        revision = new Revision(renta.agregarEquipo(new Equipo("EQ-014", "SN-014", 2021, modelo)),
                TipoRevision.SALIDA, LocalDateTime.of(2026, 11, 20, 8, 0));
        ReflectionTestUtils.setField(revision, "id", 30L);
        revision.asignar(rafael);
    }

    @Test
    void abrirLaRevisionLaDejaEnProcesoConLosCamposDeSuCategoria() {
        when(repositorio.findById(30L)).thenReturn(Optional.of(revision));

        DetalleRevisionMecanicoResponse detalle = servicio.iniciar(30L, actual);

        assertEquals(EstadoRevision.EN_PROCESO, revision.getEstado());
        assertEquals(5, detalle.campos().size());
        assertEquals("Horómetro", detalle.campos().get(0).nombre());
        assertEquals("Montacargas", detalle.tipoEquipo());
    }

    @Test
    void terminarSinNovedadGuardaUnValorPorCampo() {
        when(repositorio.findById(30L)).thenReturn(Optional.of(revision));

        RevisionTerminadaResponse r = servicio.terminar(30L, actual, captura("1,284.5", EstadoInspeccion.BIEN, null));

        assertEquals(ResultadoRevision.SIN_NOVEDAD, r.resultado());
        assertEquals(EstadoRevision.COMPLETADA, revision.getEstado());
        List<ValorRevision> valores = revision.getValores();
        assertEquals(5, valores.size());
        assertEquals("1284.5", valores.get(0).getValor());
        assertEquals("3/4", valores.get(1).getValor());
        assertEquals(EstadoInspeccion.BIEN, valores.get(2).getEstado());
        assertNull(valores.get(2).getDetalle());
        assertEquals("77", valores.get(4).getValor());
        verify(revisionServicio).aplicarResultado(revision, "");
    }

    @Test
    void unaFallaPideMantenimientoYLaDescribe() {
        when(repositorio.findById(30L)).thenReturn(Optional.of(revision));
        CapturaRevisionRequest datos = captura("1284.5", EstadoInspeccion.BIEN, null);
        datos.valores().set(3, new CapturaRevisionRequest.ValorRequest(13L, null, EstadoInspeccion.FALLA,
                "Pedal de freno sin presión"));

        RevisionTerminadaResponse r = servicio.terminar(30L, actual, datos);

        assertEquals(ResultadoRevision.REQUIERE_MANTENIMIENTO, r.resultado());
        verify(revisionServicio).aplicarResultado(revision, "Frenos y dirección: Pedal de freno sin presión");
    }

    @Test
    void unaInspeccionEnAtencionPideDetalle() {
        when(repositorio.findById(30L)).thenReturn(Optional.of(revision));
        CapturaRevisionRequest datos = captura("1284.5", EstadoInspeccion.BIEN, null);
        datos.valores().set(2, new CapturaRevisionRequest.ValorRequest(12L, null, EstadoInspeccion.ATENCION, " "));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.terminar(30L, actual, datos));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Describe lo que encontraste en Llantas.", error.getReason());
        verify(revisionServicio, never()).aplicarResultado(any(), any());
    }

    @Test
    void unNumeroMalEscritoSeRechazaConLasReglasDelModelo() {
        when(repositorio.findById(30L)).thenReturn(Optional.of(revision));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.terminar(30L, actual, captura("mil", EstadoInspeccion.BIEN, null)));

        assertEquals("Horómetro: Escribe un número, por ejemplo 4.5", error.getReason());
    }

    @Test
    void unCampoObligatorioVacioNoDejaTerminar() {
        when(repositorio.findById(30L)).thenReturn(Optional.of(revision));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.terminar(30L, actual, captura(" ", EstadoInspeccion.BIEN, null)));

        assertEquals("Llena Horómetro.", error.getReason());
    }

    @Test
    void sinLasFotosObligatoriasNoSeTermina() {
        when(repositorio.findById(30L)).thenReturn(Optional.of(revision));
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Faltan fotos: Foto de frente."))
                .when(fotoServicio).verificarCompletas(eq(OrigenFoto.REVISION), eq(30L), eq(List.of("Foto de frente")));

        assertThrows(ResponseStatusException.class,
                () -> servicio.terminar(30L, actual, captura("1284.5", EstadoInspeccion.BIEN, null)));

        assertEquals(EstadoRevision.ASIGNADO, revision.getEstado());
    }

    @Test
    void noSePuedeAbrirLaRevisionDeOtroMecanico() {
        when(repositorio.findById(30L)).thenReturn(Optional.of(revision));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.detalle(30L, PersonalActual.de(personal(2L, "MEC-11"))));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    /** Horómetro, combustible 3/4 y las dos inspecciones con el mismo estado. */
    private static CapturaRevisionRequest captura(String horometro, EstadoInspeccion estado, String detalle) {
        List<CapturaRevisionRequest.ValorRequest> valores = new ArrayList<>(List.of(
                new CapturaRevisionRequest.ValorRequest(10L, horometro, null, null),
                new CapturaRevisionRequest.ValorRequest(11L, "3/4", null, null),
                new CapturaRevisionRequest.ValorRequest(12L, null, estado, detalle),
                new CapturaRevisionRequest.ValorRequest(13L, null, estado, detalle)));
        return new CapturaRevisionRequest(valores, null);
    }

    private static DefinicionCampo campo(Categoria categoria, Long id, String nombre, TipoCampo tipo, String unidad,
                                         List<String> opciones, CapturaCampo captura) {
        DefinicionCampo c = new DefinicionCampo(categoria, nombre, tipo, unidad, true, id.intValue(), opciones, captura);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private static Personal personal(Long id, String codigo) {
        Personal p = new Personal(codigo, "Rafael", "Amaya", null, TipoPersonal.MECANICO);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }
}