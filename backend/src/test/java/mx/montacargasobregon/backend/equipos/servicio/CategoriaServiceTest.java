package mx.montacargasobregon.backend.equipos.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.equipos.dto.CategoriaRequest;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.repositorio.CategoriaRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository repositorio;

    private CategoriaService servicio;

    @BeforeEach
    void setUp() {
        servicio = new CategoriaService(repositorio);
    }

    @Test
    void crear_guardaElNombreSinEspaciosSobrantes() {
        when(repositorio.existsByNombreIgnoreCase("Herramienta")).thenReturn(false);
        when(repositorio.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));

        Categoria creada = servicio.crear(new CategoriaRequest("  Herramienta  "));

        assertEquals("Herramienta", creada.getNombre());
    }

    @Test
    void crear_conNombreRepetidoNoGuarda() {
        when(repositorio.existsByNombreIgnoreCase("grúa")).thenReturn(true);

        CategoriaRepetidaException ex = assertThrows(CategoriaRepetidaException.class,
                () -> servicio.crear(new CategoriaRequest("grúa")));

        assertEquals("Ya existe una categoría con ese nombre", ex.getMessage());
        verify(repositorio, never()).save(any());
    }

    @Test
    void actualizar_cambiaElNombre() {
        Categoria patin = new Categoria("Patin");
        when(repositorio.findById(3L)).thenReturn(Optional.of(patin));
        when(repositorio.existsByNombreIgnoreCaseAndIdNot("Patín", 3L)).thenReturn(false);

        Categoria actualizada = servicio.actualizar(3L, new CategoriaRequest("Patín"));

        assertEquals("Patín", actualizada.getNombre());
    }

    @Test
    void actualizar_conElNombreDeOtraCategoriaNoCambia() {
        Categoria patin = new Categoria("Patín");
        when(repositorio.findById(3L)).thenReturn(Optional.of(patin));
        when(repositorio.existsByNombreIgnoreCaseAndIdNot("Montacargas", 3L)).thenReturn(true);

        assertThrows(CategoriaRepetidaException.class,
                () -> servicio.actualizar(3L, new CategoriaRequest("Montacargas")));

        assertEquals("Patín", patin.getNombre());
    }

    @Test
    void actualizar_categoriaQueNoExisteResponde404() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.actualizar(99L, new CategoriaRequest("Grúa")));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
