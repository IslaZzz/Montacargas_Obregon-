package mx.montacargasobregon.backend.equipos.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.equipos.dto.EquipoRequest;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.equipos.modelo.Etiqueta;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.equipos.repositorio.EquipoRepository;
import mx.montacargasobregon.backend.equipos.repositorio.EtiquetaRepository;
import mx.montacargasobregon.backend.equipos.repositorio.ModeloRepository;

/** Registrar equipo (HU-02). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EquipoServiceTest {

    @Mock
    private EquipoRepository repositorio;
    @Mock
    private ModeloRepository modelos;
    @Mock
    private EtiquetaRepository etiquetas;

    private EquipoService servicio;
    private Modelo modelo;
    private Etiqueta interiores;

    @BeforeEach
    void setUp() {
        servicio = new EquipoService(repositorio, modelos, etiquetas);

        modelo = new Modelo("Montacargas eléctrico 3 t", "Toyota", "8FBE30", null,
                new BigDecimal("500"), new Categoria("Montacargas"));
        interiores = new Etiqueta("Uso en interiores");
        ReflectionTestUtils.setField(interiores, "id", 2L);

        when(modelos.findById(1L)).thenReturn(Optional.of(modelo));
        when(etiquetas.findAllById(Set.of(2L))).thenReturn(List.of(interiores));
        when(repositorio.save(any(Equipo.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registrar_dejaElEquipoEnRevisionDeEntradaConSusDatos() {
        Equipo equipo = servicio.registrar(new EquipoRequest(" mc-20 ", " ty8fbe-000020 ", 2024, 1L, Set.of(2L)));

        assertEquals(EstadoEquipo.EN_REVISION_ENTRADA, equipo.getEstado());
        assertEquals("MC-20", equipo.getNumeroUnidad());
        assertEquals("TY8FBE-000020", equipo.getNumeroSerie());
        assertEquals(2024, equipo.getAnioCompra());
        assertEquals(modelo, equipo.getModelo());
        assertEquals(Set.of(interiores), equipo.getEtiquetas());
    }

    @Test
    void registrar_aceptaElEquipoSinAnioNiEtiquetas() {
        Equipo equipo = servicio.registrar(new EquipoRequest("MC-20", "TY8FBE-000020", null, 1L, null));

        assertNull(equipo.getAnioCompra());
        assertTrue(equipo.getEtiquetas().isEmpty());
    }

    @Test
    void registrar_rechazaElNumeroDeUnidadRepetido() {
        when(repositorio.existsByNumeroUnidad("MC-07")).thenReturn(true);

        var error = assertThrows(EquipoRepetidoException.class,
                () -> servicio.registrar(new EquipoRequest("mc-07", "TY8FBE-000020", null, 1L, null)));

        assertEquals("numeroUnidad", error.getCampo());
        assertEquals("Ya existe un equipo con ese número de unidad", error.getMessage());
        verify(repositorio, never()).save(any());
    }

    @Test
    void registrar_rechazaElNumeroDeSerieRepetido() {
        when(repositorio.existsByNumeroSerie("TY8FBE-000007")).thenReturn(true);

        var error = assertThrows(EquipoRepetidoException.class,
                () -> servicio.registrar(new EquipoRequest("MC-20", "ty8fbe-000007", null, 1L, null)));

        assertEquals("numeroSerie", error.getCampo());
        assertEquals("Ya existe un equipo con ese número de serie", error.getMessage());
        verify(repositorio, never()).save(any());
    }

    @Test
    void registrar_rechazaUnaEtiquetaQueNoExiste() {
        when(etiquetas.findAllById(Set.of(2L, 99L))).thenReturn(List.of(interiores));

        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(new EquipoRequest("MC-20", "TY8FBE-000020", null, 1L, Set.of(2L, 99L))));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void registrar_rechazaUnModeloQueNoExiste() {
        when(modelos.findById(9L)).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(new EquipoRequest("MC-20", "TY8FBE-000020", null, 9L, null)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void actualizar_noCambiaElEstadoDelEquipo() {
        Equipo mc07 = new Equipo("MC-07", "TY8FBE-000007", 2022, modelo);
        mc07.cambiarEstado(EstadoEquipo.OPERATIVO);
        when(repositorio.findById(7L)).thenReturn(Optional.of(mc07));

        Equipo equipo = servicio.actualizar(7L, new EquipoRequest("MC-07", "TY8FBE-000007", null, 1L, Set.of(2L)));

        assertEquals(EstadoEquipo.OPERATIVO, equipo.getEstado());
        assertNull(equipo.getAnioCompra());
        assertEquals(Set.of(interiores), equipo.getEtiquetas());
    }
}
