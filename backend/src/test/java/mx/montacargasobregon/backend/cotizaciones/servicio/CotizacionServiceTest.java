package mx.montacargasobregon.backend.cotizaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;
import mx.montacargasobregon.backend.cotizaciones.repositorio.CotizacionRepository;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.rentas.servicio.RentaDeCotizacionService;

/** Cambios de estado de la cotización y la renta que nace al aceptarla (HU-20 y HU-37). */
@ExtendWith(MockitoExtension.class)
class CotizacionServiceTest {

    @Mock
    private CotizacionRepository repositorio;

    @Mock
    private RentaDeCotizacionService rentas;

    private CotizacionService servicio;
    private Cotizacion cotizacion;

    @BeforeEach
    void setUp() {
        servicio = new CotizacionService(repositorio, rentas);

        Cliente cliente = new Cliente("Carlos", "Ramírez", "Soto", "Persona física", "6445551234",
                "carlos.ramirez@correo.com", LocalDate.of(2026, 11, 1));
        cotizacion = new Cotizacion("COT-0003", cliente, null,
                LocalDateTime.of(2026, 11, 18, 8, 0), LocalDateTime.of(2026, 11, 20, 18, 0),
                new DatosDireccion("Calle Jalisco", "312", "Centro", "Navojoa", "Sonora", "85800"),
                new DatosContactoSitio("Carlos Ramírez", "6445551234"),
                new BigDecimal("1500.00"), new BigDecimal("240.00"), new BigDecimal("1740.00"));
        ReflectionTestUtils.setField(cotizacion, "id", 3L);
        cotizacion.agregarEquipo(new EquipoCotizacion(new Equipo("MC-07", "SER-0007", 2022, null),
                new BigDecimal("500.00"), new BigDecimal("1500.00")));
        when(repositorio.findById(3L)).thenReturn(Optional.of(cotizacion));
    }

    @Test
    void aceptarCreaLaRenta() {
        cotizacion.cambiarEstado(EstadoCotizacion.ENVIADA, LocalDateTime.of(2026, 11, 12, 10, 0));

        servicio.cambiarEstado(3L, EstadoCotizacion.ACEPTADA);

        assertEquals(EstadoCotizacion.ACEPTADA, cotizacion.getEstado());
        verify(rentas).crear(cotizacion);
    }

    @Test
    void noAceptadaNoCreaRenta() {
        cotizacion.cambiarEstado(EstadoCotizacion.ENVIADA, LocalDateTime.of(2026, 11, 12, 10, 0));

        servicio.cambiarEstado(3L, EstadoCotizacion.NO_ACEPTADA);

        assertEquals(EstadoCotizacion.NO_ACEPTADA, cotizacion.getEstado());
        verifyNoInteractions(rentas);
    }

    @Test
    void enviarNoCreaRenta() {
        servicio.cambiarEstado(3L, EstadoCotizacion.ENVIADA);

        assertEquals(EstadoCotizacion.ENVIADA, cotizacion.getEstado());
        verifyNoInteractions(rentas);
    }
}
