package mx.montacargasobregon.backend.sesion;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import mx.montacargasobregon.backend.sesion.controlador.SesionController;
import mx.montacargasobregon.backend.sesion.servicio.SesionService;
import mx.montacargasobregon.backend.usuarios.TipoUsuario;
import mx.montacargasobregon.backend.usuarios.Usuario;
import mx.montacargasobregon.backend.usuarios.repositorio.UsuarioRepository;

/** Iniciar y cerrar sesión y proteger las pantallas internas (HU-28). */
@WebMvcTest(SesionController.class)
@Import({SeguridadConfig.class, SesionService.class})
class SesionControllerTest {

    private static final String CORREO = "daniela.felix@montacargasobregon.mx";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioRepository usuarios;

    private Usuario daniela;

    @BeforeEach
    void setUp() {
        daniela = new Usuario("AUX-01", "Daniela", "Félix", "Moreno", CORREO,
                new BCryptPasswordEncoder(4).encode("Clave-de-prueba1"), TipoUsuario.AUXILIAR);
        when(usuarios.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(usuarios.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(daniela));
        when(usuarios.findByCorreoIgnoreCase(CORREO.toUpperCase())).thenReturn(Optional.of(daniela));
    }

    private ResultActions entrar(String correo, String contrasena) throws Exception {
        return mvc.perform(post("/api/sesion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}"));
    }

    @Test
    void conDatosCorrectosEntraYMuestraNombreYRol() throws Exception {
        MockHttpSession anterior = new MockHttpSession();
        String idAnterior = anterior.getId();

        MvcResult r = mvc.perform(post("/api/sesion").session(anterior).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\" " + CORREO.toUpperCase() + " \",\"contrasena\":\"Clave-de-prueba1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Daniela Félix"))
                .andExpect(jsonPath("$.rol").value("AUXILIAR"))
                .andExpect(jsonPath("$.correo").value(CORREO))
                .andReturn();

        MockHttpSession sesion = (MockHttpSession) r.getRequest().getSession(false);
        assertNotEquals(idAnterior, sesion.getId());

        mvc.perform(get("/api/sesion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Daniela Félix"));
    }

    @Test
    void contrasenaIncorrectaNoEntra() throws Exception {
        entrar(CORREO, "otra")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    void correoQueNoExisteDaElMismoMensaje() throws Exception {
        entrar("nadie@montacargasobregon.mx", "Clave-de-prueba1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    void usuarioDeBajaNoEntra() throws Exception {
        daniela.desactivar();

        entrar(CORREO, "Clave-de-prueba1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    void camposVaciosMarcanCadaCampo() throws Exception {
        mvc.perform(post("/api/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\" \",\"contrasena\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").value("Escribe tu correo"))
                .andExpect(jsonPath("$.errores.contrasena").value("Escribe tu contraseña"));
    }

    @Test
    void sinSesionNoSePuedeUsarLaApi() throws Exception {
        mvc.perform(get("/api/cotizaciones"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Inicia sesión para continuar"));
        mvc.perform(get("/api/sesion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void alCerrarSesionYaNoPuedeEntrar() throws Exception {
        MockHttpSession sesion = (MockHttpSession) entrar(CORREO, "Clave-de-prueba1")
                .andReturn().getRequest().getSession(false);

        mvc.perform(delete("/api/sesion").session(sesion))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/sesion").session(sesion))
                .andExpect(status().isUnauthorized());
    }
}
