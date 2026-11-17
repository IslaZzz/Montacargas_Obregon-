package mx.montacargasobregon.backend.equipos.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import mx.montacargasobregon.backend.equipos.dto.ModeloRequest;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.DefinicionCampo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.equipos.modelo.TipoCampo;
import mx.montacargasobregon.backend.equipos.repositorio.CategoriaRepository;
import mx.montacargasobregon.backend.equipos.repositorio.DefinicionCampoRepository;
import mx.montacargasobregon.backend.equipos.repositorio.ModeloRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModeloServiceTest {

    @Mock
    private ModeloRepository repositorio;
    @Mock
    private CategoriaRepository categorias;
    @Mock
    private DefinicionCampoRepository campos;

    private ModeloService servicio;
    private Categoria montacargas;
    private DefinicionCampo capacidad;
    private DefinicionCampo altura;
    private DefinicionCampo energia;

    @BeforeEach
    void setUp() {
        servicio = new ModeloService(repositorio, categorias, campos);

        montacargas = new Categoria("Montacargas");
        ReflectionTestUtils.setField(montacargas, "id", 1L);
        capacidad = campo(10L, "Capacidad de carga", TipoCampo.ENTERO, "kg", 1, List.of());
        altura = campo(11L, "Altura del mástil", TipoCampo.DECIMAL, "m", 2, List.of());
        energia = campo(12L, "Tipo de energía", TipoCampo.LISTA, null, 3, List.of("Eléctrico", "Diésel", "Gas LP"));

        when(categorias.findById(1L)).thenReturn(Optional.of(montacargas));
        when(campos.findByCategoriaIdOrderByOrden(1L)).thenReturn(List.of(capacidad, altura, energia));
        when(repositorio.save(any(Modelo.class))).thenAnswer(i -> i.getArgument(0));
    }

    private DefinicionCampo campo(Long id, String nombre, TipoCampo tipo, String unidad, int orden,
                                  List<String> opciones) {
        DefinicionCampo c = new DefinicionCampo(montacargas, nombre, tipo, unidad, false, orden, opciones);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private static ModeloRequest solicitud(String nombre, String marcaModelo, Map<Long, String> especificaciones) {
        return new ModeloRequest(1L, nombre, marcaModelo, "Para uso en interiores.", especificaciones);
    }

    @Test
    void crear_separaMarcaYCodigoYQuedaSinTarifa() {
        Modelo creado = servicio.crear(solicitud("  Montacargas eléctrico 3 t ", " Toyota  8FBE30 ", Map.of()));

        assertEquals("Montacargas eléctrico 3 t", creado.getNombre());
        assertEquals("Toyota", creado.getMarca());
        assertEquals("8FBE30", creado.getCodigo());
        assertEquals(montacargas, creado.getCategoria());
        assertNull(creado.getTarifaDia());
    }

    @Test
    void crear_guardaLasEspecificacionesNormalizadas() {
        Modelo creado = servicio.crear(solicitud("Montacargas eléctrico 3 t", "Toyota 8FBE30",
                Map.of(10L, "3,000", 11L, "4.50", 12L, "Eléctrico")));

        Map<String, String> valores = new HashMap<>();
        creado.getEspecificaciones().forEach(v -> valores.put(v.getCampo().getNombre(), v.getValor()));
        assertEquals(Map.of("Capacidad de carga", "3000", "Altura del mástil", "4.5",
                "Tipo de energía", "Eléctrico"), valores);
    }

    @Test
    void crear_sinEspecificacionesLasDejaVacias() {
        Modelo creado = servicio.crear(solicitud("Patín 2.5 t", "Crown PTH50", null));

        assertEquals(0, creado.getEspecificaciones().size());
    }

    @Test
    void crear_conNombreRepetidoNoGuarda() {
        when(repositorio.existsByNombreIgnoreCase("Montacargas eléctrico 3 t")).thenReturn(true);

        DatosModeloException ex = assertThrows(DatosModeloException.class,
                () -> servicio.crear(solicitud("Montacargas eléctrico 3 t", "Toyota 8FBE30", Map.of())));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        assertEquals("Ya existe un modelo con ese nombre", ex.getErrores().get("nombre"));
        verify(repositorio, never()).save(any());
    }

    @Test
    void crear_conMarcaSinModeloNoGuarda() {
        DatosModeloException ex = assertThrows(DatosModeloException.class,
                () -> servicio.crear(solicitud("Montacargas", "Toyota", Map.of())));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getEstado());
        assertEquals(ModeloService.MARCA_MODELO_INCOMPLETO, ex.getErrores().get("marcaModelo"));
        verify(repositorio, never()).save(any());
    }

    @Test
    void crear_conEspecificacionInvalidaMarcaEseCampo() {
        DatosModeloException ex = assertThrows(DatosModeloException.class,
                () -> servicio.crear(solicitud("Montacargas", "Toyota 8FBE30",
                        Map.of(10L, "tres mil", 12L, "Nuclear"))));

        assertEquals("Escribe un número entero, por ejemplo 3000", ex.getErrores().get("especificaciones.10"));
        assertEquals("Elige una opción de la lista", ex.getErrores().get("especificaciones.12"));
        verify(repositorio, never()).save(any());
    }

    @Test
    void actualizar_cambiaDatosYEspecificacionesSinTocarLaTarifa() {
        Modelo modelo = new Modelo("Montacargas eléctrico 3 t", "Toyota", "8FBE30", null,
                new BigDecimal("500.00"), montacargas);
        modelo.cambiarEspecificaciones(Map.of(capacidad, "3000", altura, "4.5"));
        when(repositorio.findById(7L)).thenReturn(Optional.of(modelo));

        servicio.actualizar(7L, solicitud("Montacargas eléctrico 3 t", "Toyota 8FBE30",
                Map.of(10L, "3200", 12L, "Eléctrico")));

        Map<String, String> valores = new HashMap<>();
        modelo.getEspecificaciones().forEach(v -> valores.put(v.getCampo().getNombre(), v.getValor()));
        assertEquals(Map.of("Capacidad de carga", "3200", "Tipo de energía", "Eléctrico"), valores);
        assertEquals("Para uso en interiores.", modelo.getDescripcion());
        assertEquals(new BigDecimal("500.00"), modelo.getTarifaDia());
    }

    @Test
    void actualizar_conElNombreDeOtroModeloNoCambia() {
        Modelo modelo = new Modelo("Montacargas diésel 2.5 t", "Hyster", "H50FT", null, null, montacargas);
        when(repositorio.findById(7L)).thenReturn(Optional.of(modelo));
        when(repositorio.existsByNombreIgnoreCaseAndIdNot("Montacargas eléctrico 3 t", 7L)).thenReturn(true);

        assertThrows(DatosModeloException.class,
                () -> servicio.actualizar(7L, solicitud("Montacargas eléctrico 3 t", "Hyster H50FT", Map.of())));

        assertEquals("Montacargas diésel 2.5 t", modelo.getNombre());
    }
}
