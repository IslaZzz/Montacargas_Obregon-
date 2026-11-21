package mx.montacargasobregon.backend.codigospostales.controlador;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.codigospostales.dto.CodigoPostalResponse;
import mx.montacargasobregon.backend.codigospostales.servicio.CodigoPostalService;

@WebMvcTest(CodigoPostalController.class)
@WithMockUser
class CodigoPostalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CodigoPostalService servicio;

    @Test
    void buscar_respondeLosDatosDelCodigoPostal() throws Exception {
        when(servicio.buscar("85150")).thenReturn(new CodigoPostalResponse(
                "85150", List.of("Cajeme", "Libertad"), "Ciudad Obregón", "Sonora"));

        mvc.perform(get("/api/codigos-postales/85150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoPostal").value("85150"))
                .andExpect(jsonPath("$.colonias[0]").value("Cajeme"))
                .andExpect(jsonPath("$.colonias[1]").value("Libertad"))
                .andExpect(jsonPath("$.ciudad").value("Ciudad Obregón"))
                .andExpect(jsonPath("$.estado").value("Sonora"));
    }

    @Test
    void buscar_codigoQueNoExisteResponde404() throws Exception {
        when(servicio.buscar("99999"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese código postal"));

        mvc.perform(get("/api/codigos-postales/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscar_codigoSinCincoDigitosResponde404SinBuscar() throws Exception {
        mvc.perform(get("/api/codigos-postales/8500"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/codigos-postales/8500a"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(servicio);
    }
}
