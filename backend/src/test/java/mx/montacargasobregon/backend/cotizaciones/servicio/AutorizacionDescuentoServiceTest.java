package mx.montacargasobregon.backend.cotizaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.avisos.servicio.AvisoDescuentoService;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.comun.DatosContactoSitio;
import mx.montacargasobregon.backend.comun.DatosDireccion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Descuento;
import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.TipoDescuento;
import mx.montacargasobregon.backend.cotizaciones.repositorio.CotizacionRepository;
import mx.montacargasobregon.backend.usuarios.TipoUsuario;
import mx.montacargasobregon.backend.usuarios.Usuario;
import mx.montacargasobregon.backend.usuarios.repositorio.UsuarioRepository;

/** Gerencia solo responde las cotizaciones que esperan autorización (HU-16). */
@ExtendWith(MockitoExtension.class)
class AutorizacionDescuentoServiceTest {

    private static final String CORREO_GERENTE = "patricia.romero@montacargasobregon.mx";

    @Mock
    private CotizacionRepository repositorio;
    @Mock
    private CotizacionService cotizacionService;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private AvisoDescuentoService avisos;

    private AutorizacionDescuentoService servicio;
    private Cotizacion rechazada;

    @BeforeEach
    void preparar() {
        servicio = new AutorizacionDescuentoService(repositorio, cotizacionService, usuarios, avisos);

        Usuario auxiliar = new Usuario("AUX-01", "Daniela", "Félix", "Moreno", "daniela.felix@montacargasobregon.mx",
                "hash", TipoUsuario.AUXILIAR);
        Usuario gerente = new Usuario("GER-01", "Patricia", "Romero", "Soto", CORREO_GERENTE, "hash",
                TipoUsuario.GERENTE);
        Cliente cliente = new Cliente("Carlos", "Ramírez", "Soto", "Persona física", "6445551234",
                "carlos.ramirez@correo.com", LocalDate.of(2026, 11, 1));
        rechazada = new Cotizacion("COT-0003", cliente, null,
                LocalDateTime.of(2026, 11, 18, 8, 0), LocalDateTime.of(2026, 11, 20, 18, 0),
                new DatosDireccion("Calle Jalisco", "312", "Centro", "Navojoa", "Sonora", "85800"),
                new DatosContactoSitio("Carlos Ramírez", "6445551234"),
                new BigDecimal("1500.00"), new BigDecimal("240.00"), new BigDecimal("1740.00"));
        rechazada.aplicarDescuento(new Descuento(TipoDescuento.PORCENTAJE, new BigDecimal("10"), "Cliente frecuente"));
        rechazada.enviarAAutorizacion(auxiliar);
        rechazada.rechazarDescuento(gerente, "Máximo 8%");
        when(cotizacionService.obtener(3L)).thenReturn(rechazada);
    }

    @Test
    void unaRechazadaNoSeAutoriza() {
        // Una Rechazada ya puede pasar a Borrador, pero solo al corregirla (HU-32), no desde aquí
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> servicio.autorizar(3L, CORREO_GERENTE, null));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertEquals(EstadoCotizacion.RECHAZADA, rechazada.getEstado());
        verifyNoInteractions(avisos);
    }

    @Test
    void unaRechazadaNoSeVuelveARechazar() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> servicio.rechazar(3L, CORREO_GERENTE, "Otra vez"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verifyNoInteractions(avisos);
    }
}
