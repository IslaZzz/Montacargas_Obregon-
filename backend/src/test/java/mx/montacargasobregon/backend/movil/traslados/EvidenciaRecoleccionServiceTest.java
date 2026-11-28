package mx.montacargasobregon.backend.movil.traslados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.movil.fotos.FotoResponse;
import mx.montacargasobregon.backend.movil.fotos.FotoService;
import mx.montacargasobregon.backend.movil.fotos.OrigenFoto;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;

@ExtendWith(MockitoExtension.class)
class EvidenciaRecoleccionServiceTest {

    @Mock
    private FotoService fotos;

    private EvidenciaRecoleccionService servicio;
    private Traslado recoleccion;

    @BeforeEach
    void setUp() {
        servicio = new EvidenciaRecoleccionService(fotos);

        Cliente cliente = new Cliente("Ana", "Castro", null, "Agrícola del Valle",
                "6441000004", "ana@agricolavalle.mx", LocalDate.of(2026, 1, 1));
        Renta renta = RentaDePrueba.nueva("R-2039", cliente, LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 24),
                "Carretera a Esperanza km 6", "Ana Castro", "6441000004");
        renta.agregarEquipo(new Equipo("MC-021", "SN-021", 2021, null));
        renta.agregarEquipo(new Equipo("MC-022", "SN-022", 2021, null));
        renta.agregarEquipo(new Equipo("MC-023", "SN-023", 2021, null)).cambiarFase(FaseEquipoRenta.CANCELADO);

        recoleccion = new Traslado("TR-0093", renta, TipoTraslado.RECOLECCION,
                LocalDateTime.of(2026, 11, 24, 16, 0), LocalDateTime.of(2026, 11, 24, 17, 30));
        ReflectionTestUtils.setField(recoleccion, "id", 93L);
    }

    @Test
    void cadaEquipoPideSusCuatroVistasYElCanceladoNoCuenta() {
        when(fotos.listar(OrigenFoto.TRASLADO, 93L)).thenReturn(List.of(
                foto(1L, "MC-021 · Frente"), foto(2L, "MC-021 · Parte trasera")));

        EvidenciaResponse evidencia = servicio.de(recoleccion);

        assertEquals(8, evidencia.requeridas());
        assertEquals(2, evidencia.tomadas());
        assertTrue(evidencia.editable());
        assertEquals(2, evidencia.equipos().size());
        assertEquals("MC-021", evidencia.equipos().get(0).numeroUnidad());
        assertEquals(2, evidencia.equipos().get(0).tomadas());
        assertNotNull(evidencia.equipos().get(0).vistas().get(0).foto());
        assertNull(evidencia.equipos().get(0).vistas().get(2).foto());
        assertEquals(0, evidencia.equipos().get(1).tomadas());
    }

    @Test
    void elMotivoDiceCuantasFaltan() {
        when(fotos.listar(OrigenFoto.TRASLADO, 93L)).thenReturn(List.of(foto(1L, "MC-021 · Frente")));

        assertEquals("Primero registra las 8 fotografías de evidencia (faltan 7). "
                + "Después podrás confirmar la recolección.", servicio.motivo(recoleccion));
    }

    @Test
    void conTodasLasFotosNoHayMotivo() {
        when(fotos.listar(OrigenFoto.TRASLADO, 93L)).thenReturn(EvidenciaRecoleccionService.VISTAS.stream()
                .flatMap(v -> List.of("MC-021 · " + v, "MC-022 · " + v).stream())
                .map(e -> foto(1L, e))
                .toList());

        assertNull(servicio.motivo(recoleccion));
    }

    @Test
    void despuesDeConfirmarYaNoEsEditable() {
        recoleccion.iniciar();
        when(fotos.listar(OrigenFoto.TRASLADO, 93L)).thenReturn(List.of());

        assertFalse(servicio.de(recoleccion).editable());
    }

    private static FotoResponse foto(Long id, String etiqueta) {
        return new FotoResponse(id, etiqueta, LocalDateTime.of(2026, 11, 24, 15, 0));
    }
}