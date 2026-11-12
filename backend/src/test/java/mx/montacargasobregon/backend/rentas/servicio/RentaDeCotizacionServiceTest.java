package mx.montacargasobregon.backend.rentas.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.comun.DatosContactoSitio;
import mx.montacargasobregon.backend.comun.DatosDireccion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EquipoCotizacion;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.rentas.repositorio.RentaRepository;

/** HU-37: al aceptar la cotización nace su renta. */
@ExtendWith(MockitoExtension.class)
class RentaDeCotizacionServiceTest {

    @Mock
    private RentaRepository repositorio;

    @Mock
    private AvanceRentaService avanceRenta;

    private RentaDeCotizacionService servicio;
    private Cliente cliente;
    private Cotizacion cotizacion;

    @BeforeEach
    void setUp() {
        servicio = new RentaDeCotizacionService(repositorio, avanceRenta);

        cliente = new Cliente("María", "López", "Ruiz", "Obras del Mayo", "6449876543",
                "compras@obrasdelmayo.mx", LocalDate.of(2026, 11, 1));
        cotizacion = new Cotizacion("COT-0005", cliente, null,
                LocalDateTime.of(2026, 11, 17, 8, 0), LocalDateTime.of(2026, 11, 21, 18, 0),
                new DatosDireccion("Carretera Internacional", "km 4.5", "Parque Industrial", "Cd. Obregón",
                        "Sonora", "85065"),
                new DatosContactoSitio("Arq. Laura Medina", "6441112233"),
                new BigDecimal("8500.00"), new BigDecimal("1360.00"), new BigDecimal("9860.00"));
        ReflectionTestUtils.setField(cotizacion, "id", 5L);
        cotizacion.agregarEquipo(new EquipoCotizacion(new Equipo("MC-07", "SER-0007", 2022, null),
                new BigDecimal("500.00"), new BigDecimal("2500.00")));
        cotizacion.agregarEquipo(new EquipoCotizacion(new Equipo("GR-02", "SER-0102", 2021, null),
                new BigDecimal("1200.00"), new BigDecimal("6000.00")));
    }

    @Test
    void creaLaRentaConLosDatosDeLaCotizacion() {
        when(repositorio.findByCotizacionId(5L)).thenReturn(Optional.empty());
        when(repositorio.ultimoNumeroFolio()).thenReturn(2090);
        when(repositorio.save(any(Renta.class))).thenAnswer(inv -> inv.getArgument(0));

        Renta renta = servicio.crear(cotizacion);

        assertEquals("R-2091", renta.getFolio());
        assertSame(cotizacion, renta.getCotizacion());
        assertSame(cliente, renta.getCliente());
        assertEquals(EstadoRenta.PROGRAMADA, renta.getEstado());
        assertEquals(LocalDate.of(2026, 11, 17), renta.getFechaInicio());
        assertEquals(LocalDate.of(2026, 11, 21), renta.getFechaFin());
        assertEquals("Carretera Internacional km 4.5, Col. Parque Industrial, Cd. Obregón, Sonora, C.P. 85065",
                renta.getDireccionObra());
        assertEquals("Arq. Laura Medina", renta.getContactoObra());
        assertEquals("6441112233", renta.getTelefonoContacto());
        // Un registro por equipo, todos reservados
        assertEquals(List.of("MC-07", "GR-02"),
                renta.getEquipos().stream().map(e -> e.getEquipo().getNumeroUnidad()).toList());
        assertEquals(List.of(FaseEquipoRenta.RESERVADO, FaseEquipoRenta.RESERVADO),
                renta.getEquipos().stream().map(EquipoRenta::getFase).toList());
        // Rangos de un solo momento con la hora de la cotización; Operativo los ajusta
        assertEquals(LocalDateTime.of(2026, 11, 17, 8, 0), renta.getEntregaDesde());
        assertEquals(LocalDateTime.of(2026, 11, 17, 8, 0), renta.getEntregaHasta());
        assertEquals(LocalDateTime.of(2026, 11, 21, 18, 0), renta.getRecoleccionDesde());
        assertEquals(LocalDateTime.of(2026, 11, 21, 18, 0), renta.getRecoleccionHasta());
        // Con la entrega programada nacen sus revisiones de salida
        verify(avanceRenta).programarRevisionesDeSalida(renta);
    }

    @Test
    void usaElPrimerFolioCuandoNoHayRentas() {
        when(repositorio.findByCotizacionId(5L)).thenReturn(Optional.empty());
        when(repositorio.ultimoNumeroFolio()).thenReturn(null);
        when(repositorio.save(any(Renta.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("R-0001", servicio.crear(cotizacion).getFolio());
    }

    @Test
    void noCreaOtraSiLaCotizacionYaTieneRenta() {
        Renta existente = new Renta("R-2091", cotizacion);
        when(repositorio.findByCotizacionId(5L)).thenReturn(Optional.of(existente));

        assertSame(existente, servicio.crear(cotizacion));
        verify(repositorio, never()).save(any());
        verify(avanceRenta, never()).programarRevisionesDeSalida(any());
    }
}
