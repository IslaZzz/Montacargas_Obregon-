package mx.montacargasobregon.backend.avisos.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.avisos.modelo.AvisoOperacion;
import mx.montacargasobregon.backend.avisos.modelo.TipoAvisoOperacion;
import mx.montacargasobregon.backend.avisos.repositorio.AvisoOperacionRepository;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.usuarios.TipoUsuario;
import mx.montacargasobregon.backend.usuarios.Usuario;
import mx.montacargasobregon.backend.usuarios.repositorio.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AvisoOperacionServiceTest {

    @Mock
    private AvisoOperacionRepository avisos;

    @Mock
    private UsuarioRepository usuarios;

    @Captor
    private ArgumentCaptor<List<AvisoOperacion>> guardados;

    private AvisoOperacionService servicio;

    private final Usuario daniela = usuario(1L, "AUX-01", "daniela.felix@montacargasobregon.mx", TipoUsuario.AUXILIAR);
    private final Usuario luis = usuario(2L, "AUX-02", "luis.ochoa@montacargasobregon.mx", TipoUsuario.AUXILIAR);
    private final Usuario patricia = usuario(3L, "GER-01", "patricia.romero@montacargasobregon.mx", TipoUsuario.GERENTE);

    private Personal operador;
    private Traslado entrega;

    @BeforeEach
    void setUp() {
        servicio = new AvisoOperacionService(avisos, usuarios);

        Cliente cliente = new Cliente("Laura", "Méndez", null, "Constructora Yaqui",
                "6441000002", "obra@constructorayaqui.mx", LocalDate.of(2026, 1, 1));
        Renta renta = RentaDePrueba.nueva("R-2048", cliente, LocalDate.of(2026, 11, 24), LocalDate.of(2026, 12, 6),
                "Calle 200 s/n", "Laura Méndez", "6441000002");
        renta.agregarEquipo(new Equipo("MC-11", "SN-011", 2021, null));

        operador = new Personal("OPT-10", "Luis", "Valenzuela", null, TipoPersonal.OP_TRASLADO);
        ReflectionTestUtils.setField(operador, "id", 100L);

        entrega = new Traslado("TR-0087", renta, TipoTraslado.ENTREGA,
                LocalDateTime.of(2026, 11, 24, 10, 0), LocalDateTime.of(2026, 11, 24, 12, 0));
        entrega.asignarResponsable(operador);
    }

    @Test
    void elAvisoDeLaOficinaLlegaACadaAuxiliarActivo() {
        luis.desactivar();
        when(usuarios.findAll()).thenReturn(List.of(daniela, luis, patricia));

        servicio.trasladoIniciado(entrega);

        verify(avisos).saveAll(guardados.capture());
        List<AvisoOperacion> lista = guardados.getValue();
        assertEquals(1, lista.size());
        assertSame(daniela, lista.get(0).getUsuario());
        assertNull(lista.get(0).getPersonal());
        assertEquals(TipoAvisoOperacion.TRASLADO_INICIADO, lista.get(0).getTipo());
        assertEquals("Traslado iniciado: TR-0087", lista.get(0).getTitulo());
        assertTrue(lista.get(0).getDetalle().contains("Luis Valenzuela"));
        assertTrue(lista.get(0).getDetalle().contains("1 equipo"));
        assertTrue(lista.get(0).getDetalle().contains("R-2048"));
    }

    @Test
    void laEntregaRegistradaDiceQuienEntrego() {
        when(usuarios.findAll()).thenReturn(List.of(daniela));

        servicio.entregaRegistrada(entrega);

        verify(avisos).saveAll(guardados.capture());
        AvisoOperacion aviso = guardados.getValue().get(0);
        assertEquals(TipoAvisoOperacion.ENTREGA_REGISTRADA, aviso.getTipo());
        assertTrue(aviso.getDetalle().startsWith("Luis Valenzuela entregó e instaló 1 equipo"));
    }

    @Test
    void laReasignacionLeLlegaAlOperadorAnterior() {
        servicio.tareaReasignada(entrega, operador);

        ArgumentCaptor<AvisoOperacion> aviso = ArgumentCaptor.forClass(AvisoOperacion.class);
        verify(avisos).save(aviso.capture());
        assertSame(operador, aviso.getValue().getPersonal());
        assertNull(aviso.getValue().getUsuario());
        assertEquals(TipoAvisoOperacion.TAREA_REASIGNADA, aviso.getValue().getTipo());
        assertEquals("La tarea TR-0087 se reasignó", aviso.getValue().getTitulo());
    }

    @Test
    void listarPideUnaSesionValida() {
        when(usuarios.findByCorreoIgnoreCase(any())).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.listar("nadie@montacargasobregon.mx"));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void marcarLeidoSoloTocaLosDelUsuario() {
        when(usuarios.findByCorreoIgnoreCase(daniela.getCorreo())).thenReturn(Optional.of(daniela));

        servicio.marcarLeido(5L, daniela.getCorreo());

        verify(avisos).marcarLeidoDeUsuario(5L, 1L);
    }

    private static Usuario usuario(Long id, String numero, String correo, TipoUsuario tipo) {
        Usuario u = new Usuario(numero, "Nombre", "Apellido", null, correo, "hash", tipo);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }
}