package mx.montacargasobregon.backend.equipos.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.servicio.CategoriaRepetidaException;
import mx.montacargasobregon.backend.equipos.servicio.CategoriaService;

@WebMvcTest(CategoriaController.class)
@WithMockUser
class CategoriaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CategoriaService servicio;

    private static String cuerpo(String nombre) {
        return "{\"nombre\":\"" + nombre + "\"}";
    }

    @Test
    void crear_respondeLaCategoriaCreada() throws Exception {
        Categoria herramienta = new Categoria("Herramienta");
        ReflectionTestUtils.setField(herramienta, "id", 5L);
        when(servicio.crear(any())).thenReturn(herramienta);

        mvc.perform(post("/api/categorias").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("Herramienta")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nombre").value("Herramienta"));
    }

    @Test
    void crear_vacioMarcaElNombre() throws Exception {
        mvc.perform(post("/api/categorias").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("  ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").value("Captura el nombre"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_conMasDe50CaracteresMarcaElNombre() throws Exception {
        mvc.perform(post("/api/categorias").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("a".repeat(51))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").value("El nombre admite máximo 50 caracteres"));
    }

    @Test
    void crear_repetidaMarcaElNombre() throws Exception {
        when(servicio.crear(any())).thenThrow(new CategoriaRepetidaException());

        mvc.perform(post("/api/categorias").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("Grúa")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.nombre").value("Ya existe una categoría con ese nombre"));
    }

    @Test
    void actualizar_respondeLaCategoriaConSuNuevoNombre() throws Exception {
        Categoria patin = new Categoria("Patín");
        ReflectionTestUtils.setField(patin, "id", 3L);
        when(servicio.actualizar(eq(3L), any())).thenReturn(patin);

        mvc.perform(put("/api/categorias/3").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("Patín")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Patín"));
    }
}
