package mx.montacargasobregon.backend.equipos.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mx.montacargasobregon.backend.equipos.dto.EtiquetaRequest;
import mx.montacargasobregon.backend.equipos.modelo.Etiqueta;
import mx.montacargasobregon.backend.equipos.repositorio.EtiquetaRepository;

/** «+ Nueva etiqueta» del formulario de equipo (HU-02). */
@ExtendWith(MockitoExtension.class)
class EtiquetaServiceTest {

    @Mock
    private EtiquetaRepository repositorio;

    private EtiquetaService servicio;

    @BeforeEach
    void setUp() {
        servicio = new EtiquetaService(repositorio);
    }

    @Test
    void crear_guardaElNombreSinEspaciosDeMas() {
        when(repositorio.save(any(Etiqueta.class))).thenAnswer(i -> i.getArgument(0));

        Etiqueta etiqueta = servicio.crear(new EtiquetaRequest("  Llanta sólida  "));

        assertEquals("Llanta sólida", etiqueta.getNombre());
    }

    @Test
    void crear_rechazaElNombreRepetido() {
        when(repositorio.existsByNombreIgnoreCase("uso en interiores")).thenReturn(true);

        var error = assertThrows(EtiquetaRepetidaException.class,
                () -> servicio.crear(new EtiquetaRequest("uso en interiores")));

        assertEquals("Ya existe una etiqueta con ese nombre", error.getMessage());
        verify(repositorio, never()).save(any());
    }
}
