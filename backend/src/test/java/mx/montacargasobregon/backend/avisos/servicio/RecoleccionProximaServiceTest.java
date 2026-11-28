package mx.montacargasobregon.backend.avisos.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.avisos.dto.RecoleccionProximaResponse;
import mx.montacargasobregon.backend.avisos.repositorio.RecoleccionProximaRepository;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;

@ExtendWith(MockitoExtension.class)
class RecoleccionProximaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 24);

    @Mock
    private RecoleccionProximaRepository repositorio;

    private RecoleccionProximaService servicio;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        servicio = new RecoleccionProximaService(repositorio);
        cliente = new Cliente("Ana", "Castro", null, "Agrícola del Valle",
                "6441000004", "ana@agricolavalle.mx", LocalDate.of(2026, 1, 1));
    }

    @Test
    void buscaLasRentasEnCursoQueTerminanAMasTardarEnLosDiasIndicados() {
        when(repositorio.listarQueTerminan(EstadoRenta.EN_CURSO, HOY.plusDays(3))).thenReturn(List.of());

        assertTrue(servicio.listar(3, HOY).isEmpty());
        verify(repositorio, never()).recoleccionesDe(any());
    }

    @Test
    void sinHorarioMuestraLaRentaConSusDatos() {
        Renta renta = renta(1L, "R-2039", HOY);
        when(repositorio.listarQueTerminan(EstadoRenta.EN_CURSO, HOY.plusDays(3))).thenReturn(List.of(renta));
        when(repositorio.recoleccionesDe(List.of(1L))).thenReturn(List.of());

        List<RecoleccionProximaResponse> lista = servicio.listar(3, HOY);

        assertEquals(1, lista.size());
        RecoleccionProximaResponse r = lista.get(0);
        assertEquals("R-2039", r.folio());
        assertEquals("Agrícola del Valle", r.cliente());
        assertEquals("Carretera a Esperanza km 6", r.domicilio());
        assertEquals(List.of("MC-021"), r.equipos());
        assertEquals(0, r.diasParaFin());
        assertFalse(r.vencida());
        assertNull(r.recoleccionDesde());
    }

    @Test
    void laQueYaTerminoSinRecoleccionConfirmadaSeMarcaVencida() {
        Renta renta = renta(1L, "R-2039", HOY.minusDays(2));
        Traslado programada = recoleccion(renta);
        when(repositorio.listarQueTerminan(any(), any())).thenReturn(List.of(renta));
        when(repositorio.recoleccionesDe(List.of(1L))).thenReturn(List.of(programada));

        RecoleccionProximaResponse r = servicio.listar(3, HOY).get(0);

        assertTrue(r.vencida());
        assertEquals(-2, r.diasParaFin());
        assertEquals("TR-0093", r.trasladoFolio());
    }

    @Test
    void desapareceCuandoSeConfirmaLaRecoleccion() {
        Renta renta = renta(1L, "R-2039", HOY);
        Traslado confirmada = recoleccion(renta);
        confirmada.iniciar();
        when(repositorio.listarQueTerminan(any(), any())).thenReturn(List.of(renta));
        when(repositorio.recoleccionesDe(List.of(1L))).thenReturn(List.of(confirmada));

        assertTrue(servicio.listar(3, HOY).isEmpty());
    }

    @Test
    void laAnticipacionDebeSerDeUnoATreintaDias() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.listar(0, HOY));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    private Renta renta(Long id, String folio, LocalDate fin) {
        Renta renta = RentaDePrueba.nueva(folio, cliente, fin.minusDays(10), fin,
                "Carretera a Esperanza km 6", "Ana Castro", "6441000004");
        ReflectionTestUtils.setField(renta, "id", id);
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        renta.agregarEquipo(new Equipo("MC-021", "SN-021", 2021, null));
        return renta;
    }

    private static Traslado recoleccion(Renta renta) {
        return new Traslado("TR-0093", renta, TipoTraslado.RECOLECCION,
                LocalDateTime.of(2026, 11, 24, 16, 0), LocalDateTime.of(2026, 11, 24, 17, 30));
    }
}