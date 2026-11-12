package mx.montacargasobregon.backend.avisos.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.avisos.dto.ApartadoProximoResponse;
import mx.montacargasobregon.backend.avisos.repositorio.ApartadoProximoRepository;
import mx.montacargasobregon.backend.cartas.modelo.CartaResponsiva;
import mx.montacargasobregon.backend.cartas.modelo.EstadoCarta;
import mx.montacargasobregon.backend.cartas.repositorio.CartaResponsivaRepository;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

@ExtendWith(MockitoExtension.class)
class ApartadoProximoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 7);

    @Mock
    private ApartadoProximoRepository rentas;
    @Mock
    private CartaResponsivaRepository cartas;

    private ApartadoProximoService servicio;
    private Cliente hotel;

    @BeforeEach
    void preparar() {
        servicio = new ApartadoProximoService(rentas, cartas);
        hotel = new Cliente("Juan", "Pérez", "López", "Hotel Valle Grande",
                "6440000000", "juan.perez@ejemplo.com", LocalDate.of(2026, 9, 1));
    }

    @Test
    void buscaRentasProgramadasEntreHoyYLosDiasIndicados() {
        when(rentas.listarQueInician(EstadoRenta.PROGRAMADA, HOY, HOY.plusDays(7))).thenReturn(List.of());

        assertTrue(servicio.listar(7, HOY).isEmpty());
        // Sin rentas no hace falta buscar cartas
        verifyNoInteractions(cartas);
    }

    @Test
    void sinRegistroDeCartaQuedaSinGenerarYTraeLosDatosDelAviso() {
        Renta renta = renta(1040L, "R-1040", HOY.plusDays(1));
        renta.agregarEquipo(equipo("MTC-08"));
        renta.agregarEquipo(equipo("MTC-02"));
        when(rentas.listarQueInician(EstadoRenta.PROGRAMADA, HOY, HOY.plusDays(7))).thenReturn(List.of(renta));
        when(cartas.findByRentaIdIn(List.of(1040L))).thenReturn(List.of());

        List<ApartadoProximoResponse> avisos = servicio.listar(7, HOY);

        assertEquals(1, avisos.size());
        ApartadoProximoResponse aviso = avisos.get(0);
        assertEquals("R-1040", aviso.folio());
        assertEquals("Hotel Valle Grande", aviso.cliente());
        assertEquals(EstadoCarta.SIN_GENERAR, aviso.estadoCarta());
        assertEquals(1, aviso.diasParaInicio());
        assertNull(aviso.entregaDesde());
        // Ordenados por número de unidad
        assertEquals("MTC-02", aviso.equipos().get(0).numeroUnidad());
        assertEquals("Toyota 8FGU25", aviso.equipos().get(0).modelo());
    }

    @Test
    void noAvisaRentasConLaCartaFirmada() {
        Renta firmada = renta(1L, "R-0001", HOY.plusDays(2));
        Renta enviada = renta(2L, "R-0002", HOY.plusDays(6));
        when(rentas.listarQueInician(EstadoRenta.PROGRAMADA, HOY, HOY.plusDays(7)))
                .thenReturn(List.of(firmada, enviada));
        when(cartas.findByRentaIdIn(anyCollection()))
                .thenReturn(List.of(carta(firmada, EstadoCarta.FIRMADA), carta(enviada, EstadoCarta.ENVIADA)));

        List<ApartadoProximoResponse> avisos = servicio.listar(7, HOY);

        assertEquals(1, avisos.size());
        assertEquals("R-0002", avisos.get(0).folio());
        assertEquals(EstadoCarta.ENVIADA, avisos.get(0).estadoCarta());
    }

    @Test
    void noMuestraEquiposCanceladosDeLaRenta() {
        Renta renta = renta(1L, "R-0001", HOY);
        renta.agregarEquipo(equipo("MC-01"));
        renta.agregarEquipo(equipo("MC-02")).cambiarFase(FaseEquipoRenta.CANCELADO);
        when(rentas.listarQueInician(EstadoRenta.PROGRAMADA, HOY, HOY.plusDays(1))).thenReturn(List.of(renta));
        when(cartas.findByRentaIdIn(anyCollection())).thenReturn(List.of());

        ApartadoProximoResponse aviso = servicio.listar(1, HOY).get(0);

        assertEquals(1, aviso.equipos().size());
        assertEquals("MC-01", aviso.equipos().get(0).numeroUnidad());
        assertEquals(0, aviso.diasParaInicio());
    }

    @Test
    void usaElNombreDeLaPersonaSiElClienteNoTieneEmpresa() {
        Cliente persona = new Cliente("Ana", "Castro", "Ruiz", null,
                "6441000004", "ana@ejemplo.com", LocalDate.of(2026, 9, 1));
        Renta renta = RentaDePrueba.nueva("R-0003", persona, HOY.plusDays(3), HOY.plusDays(10),
                "Calle 200", "Ana Castro", "6441000004");
        ReflectionTestUtils.setField(renta, "id", 3L);
        when(rentas.listarQueInician(EstadoRenta.PROGRAMADA, HOY, HOY.plusDays(3))).thenReturn(List.of(renta));
        when(cartas.findByRentaIdIn(anyCollection())).thenReturn(List.of());

        assertEquals("Ana Castro", servicio.listar(3, HOY).get(0).cliente());
    }

    @Test
    void rechazaUnaAnticipacionFueraDeRango() {
        ResponseStatusException cero = assertThrows(ResponseStatusException.class, () -> servicio.listar(0, HOY));
        assertEquals(HttpStatus.BAD_REQUEST, cero.getStatusCode());
        assertThrows(ResponseStatusException.class, () -> servicio.listar(31, HOY));
        verifyNoInteractions(rentas);
    }

    @Test
    void elLimiteDeDiasIncluyeElUltimoDia() {
        when(rentas.listarQueInician(EstadoRenta.PROGRAMADA, HOY, HOY.plusDays(10))).thenReturn(List.of());

        servicio.listar(10, HOY);

        verify(rentas).listarQueInician(EstadoRenta.PROGRAMADA, HOY, HOY.plusDays(10));
    }

    private Renta renta(Long id, String folio, LocalDate inicio) {
        Renta renta = RentaDePrueba.nueva(folio, hotel, inicio, inicio.plusDays(7),
                "Calle Ejemplo 123", "Juan Pérez", "6440000000");
        ReflectionTestUtils.setField(renta, "id", id);
        return renta;
    }

    private static Equipo equipo(String numeroUnidad) {
        Modelo modelo = new Modelo("8FGU25", "Toyota", "COD-" + numeroUnidad, null,
                new BigDecimal("1200.00"), new Categoria("Montacargas"));
        return new Equipo(numeroUnidad, "SERIE-" + numeroUnidad, 2022, modelo);
    }

    private static CartaResponsiva carta(Renta renta, EstadoCarta estado) {
        CartaResponsiva carta = new CartaResponsiva(renta, null, null);
        carta.cambiarEstado(estado);
        return carta;
    }
}
