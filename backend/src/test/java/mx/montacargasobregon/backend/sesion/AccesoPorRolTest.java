package mx.montacargasobregon.backend.sesion;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
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

import mx.montacargasobregon.backend.sesion.controlador.SesionController;
import mx.montacargasobregon.backend.sesion.servicio.SesionService;
import mx.montacargasobregon.backend.usuarios.TipoUsuario;
import mx.montacargasobregon.backend.usuarios.Usuario;
import mx.montacargasobregon.backend.usuarios.repositorio.UsuarioRepository;

/** Acceso por rol (HU-29): las rutas de gerencia no se pueden usar con la sesión de un auxiliar. */
@WebMvcTest(SesionController.class)
@Import({SeguridadConfig.class, SesionService.class})
class AccesoPorRolTest {

    private static final String AUXILIAR = "daniela.felix@montacargasobregon.mx";
    private static final String GERENTE = "patricia.romero@montacargasobregon.mx";
    private static final String CLAVE = "Clave-de-prueba1";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioRepository usuarios;

    @BeforeEach
    void setUp() {
        String cifrada = new BCryptPasswordEncoder(4).encode(CLAVE);
        when(usuarios.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(usuarios.findByCorreoIgnoreCase(AUXILIAR)).thenReturn(Optional.of(new Usuario(
                "AUX-01", "Daniela", "Félix", "Moreno", AUXILIAR, cifrada, TipoUsuario.AUXILIAR)));
        when(usuarios.findByCorreoIgnoreCase(GERENTE)).thenReturn(Optional.of(new Usuario(
                "GER-01", "Patricia", "Romero", "Soto", GERENTE, cifrada, TipoUsuario.GERENTE)));
    }

    private MockHttpSession entrar(String correo) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void auxiliarNoPuedeUsarLasRutasDeGerencia() throws Exception {
        MockHttpSession sesion = entrar(AUXILIAR);

        for (String ruta : new String[] {"/api/autorizaciones", "/api/autorizaciones/3", "/api/ingresos"}) {
            mvc.perform(get(ruta).session(sesion))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.mensaje").value("No tienes permiso para hacer esto"));
        }
    }

    @Test
    void auxiliarNoPuedeRegistrarEquiposNiEtiquetasPeroSiConsultarlos() throws Exception {
        MockHttpSession sesion = entrar(AUXILIAR);

        for (String ruta : new String[] {"/api/equipos", "/api/etiquetas"}) {
            mvc.perform(post(ruta).session(sesion).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.mensaje").value("No tienes permiso para hacer esto"));
            // Consultar pasa la seguridad; 404 porque en esta prueba no están esos controladores
            mvc.perform(get(ruta).session(sesion)).andExpect(status().isNotFound());
        }
    }

    @Test
    void gerenciaSiPuedeRegistrarEquiposYEtiquetas() throws Exception {
        MockHttpSession sesion = entrar(GERENTE);

        for (String ruta : new String[] {"/api/equipos", "/api/etiquetas"}) {
            mvc.perform(post(ruta).session(sesion).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void gerenciaSiPasaLaRevisionDeRol() throws Exception {
        MockHttpSession sesion = entrar(GERENTE);

        // Pasa la seguridad; responde 404 porque en esta prueba no hay controlador de autorizaciones
        mvc.perform(get("/api/autorizaciones").session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    void auxiliarSigueUsandoLoDemas() throws Exception {
        MockHttpSession sesion = entrar(AUXILIAR);

        mvc.perform(get("/api/sesion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("AUXILIAR"));
    }

    @Test
    void sinSesionLasRutasDeGerenciaPidenIniciarSesion() throws Exception {
        mvc.perform(get("/api/autorizaciones"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Inicia sesión para continuar"));
    }
}
