package mx.montacargasobregon.backend.codigospostales.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.codigospostales.dto.CodigoPostalResponse;
import mx.montacargasobregon.backend.codigospostales.modelo.CodigoPostal;
import mx.montacargasobregon.backend.codigospostales.repositorio.CodigoPostalRepository;

@ExtendWith(MockitoExtension.class)
class CodigoPostalServiceTest {

    @Mock
    private CodigoPostalRepository repositorio;

    private CodigoPostalService servicio;

    @BeforeEach
    void setUp() {
        servicio = new CodigoPostalService(repositorio);
    }

    private static CodigoPostal asentamiento(String cp, String colonia, String ciudad, String municipio) {
        return new CodigoPostal(cp, colonia, ciudad, municipio, "Sonora");
    }

    @Test
    void buscar_regresaLaColoniaLaCiudadYElEstado() {
        when(repositorio.findByCodigoPostalOrderByColoniaAsc("85000")).thenReturn(List.of(
                asentamiento("85000", "Centro", "Ciudad Obregón", "Cajeme")));

        CodigoPostalResponse respuesta = servicio.buscar("85000");

        assertEquals("85000", respuesta.codigoPostal());
        assertEquals(List.of("Centro"), respuesta.colonias());
        assertEquals("Ciudad Obregón", respuesta.ciudad());
        assertEquals("Sonora", respuesta.estado());
    }

    @Test
    void buscar_regresaTodasLasColoniasSinRepetir() {
        when(repositorio.findByCodigoPostalOrderByColoniaAsc("85150")).thenReturn(List.of(
                asentamiento("85150", "Cajeme", "Ciudad Obregón", "Cajeme"),
                asentamiento("85150", "Libertad", "Ciudad Obregón", "Cajeme"),
                asentamiento("85150", "Libertad", "Ciudad Obregón", "Cajeme"),
                asentamiento("85150", "Real del Sol", "Ciudad Obregón", "Cajeme")));

        CodigoPostalResponse respuesta = servicio.buscar("85150");

        assertEquals(List.of("Cajeme", "Libertad", "Real del Sol"), respuesta.colonias());
    }

    @Test
    void buscar_sinCiudadUsaElMunicipio() {
        when(repositorio.findByCodigoPostalOrderByColoniaAsc("85203")).thenReturn(List.of(
                asentamiento("85203", "Cócorit", null, "Cajeme"),
                asentamiento("85203", "Pueblo Yaqui", "", "Cajeme")));

        assertEquals("Cajeme", servicio.buscar("85203").ciudad());
    }

    @Test
    void buscar_tomaLaCiudadAunqueAlgunaColoniaNoLaTenga() {
        when(repositorio.findByCodigoPostalOrderByColoniaAsc("85010")).thenReturn(List.of(
                asentamiento("85010", "Campestre", null, "Cajeme"),
                asentamiento("85010", "Zona Norte", "Ciudad Obregón", "Cajeme")));

        assertEquals("Ciudad Obregón", servicio.buscar("85010").ciudad());
    }

    @Test
    void buscar_codigoQueNoExisteResponde404() {
        when(repositorio.findByCodigoPostalOrderByColoniaAsc("99999")).thenReturn(List.of());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.buscar("99999"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals("No encontramos ese código postal", error.getReason());
    }
}
