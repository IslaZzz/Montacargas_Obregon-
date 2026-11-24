package mx.montacargasobregon.backend.avisos.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.avisos.dto.AvisoOperacionResponse;
import mx.montacargasobregon.backend.avisos.modelo.AvisoOperacion;
import mx.montacargasobregon.backend.avisos.modelo.TipoAvisoOperacion;
import mx.montacargasobregon.backend.avisos.repositorio.AvisoOperacionRepository;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.usuarios.TipoUsuario;
import mx.montacargasobregon.backend.usuarios.Usuario;
import mx.montacargasobregon.backend.usuarios.repositorio.UsuarioRepository;

/**
 * Avisos de la operación. La oficina (cada auxiliar activo) se entera de cómo van los traslados,
 * y el operador se entera cuando le quitan una tarea. Cada destinatario marca los suyos como leídos.
 */
@Service
public class AvisoOperacionService {

    private final AvisoOperacionRepository avisos;
    private final UsuarioRepository usuarios;

    public AvisoOperacionService(AvisoOperacionRepository avisos, UsuarioRepository usuarios) {
        this.avisos = avisos;
        this.usuarios = usuarios;
    }

    // ---- lo que pasa en campo (se llama desde los servicios de traslados) ----

    /** OHU10: el operador salió con los equipos de la entrega. */
    @Transactional
    public void trasladoIniciado(Traslado traslado) {
        avisarOficina(TipoAvisoOperacion.TRASLADO_INICIADO, traslado,
                "Traslado iniciado: " + traslado.getFolio(),
                operador(traslado) + " salió con " + equipos(traslado.getRenta()) + ". " + renta(traslado));
    }

    /** OHU11: los equipos quedaron entregados e instalados con el cliente. */
    @Transactional
    public void entregaRegistrada(Traslado traslado) {
        avisarOficina(TipoAvisoOperacion.ENTREGA_REGISTRADA, traslado,
                "Entrega registrada: " + traslado.getFolio(),
                operador(traslado) + " entregó e instaló " + equipos(traslado.getRenta()) + ". " + renta(traslado));
    }

    /** OHU14: el operador recogió los equipos y van de regreso. */
    @Transactional
    public void recoleccionConfirmada(Traslado traslado) {
        avisarOficina(TipoAvisoOperacion.RECOLECCION_CONFIRMADA, traslado,
                "Equipos de regreso: " + traslado.getFolio(),
                operador(traslado) + " recogió " + equipos(traslado.getRenta()) + ". " + renta(traslado));
    }

    /** OHU15: los equipos llegaron a la oficina; falta asignar su revisión de entrada. */
    @Transactional
    public void equiposRecibidos(Traslado traslado) {
        avisarOficina(TipoAvisoOperacion.EQUIPOS_RECIBIDOS, traslado,
                "Equipos recibidos en oficina: " + traslado.getFolio(),
                "Asigna la revisión de entrada de " + equipos(traslado.getRenta()) + ". " + renta(traslado));
    }

    /** OHU09: la tarea pasó a otro operador; el anterior se entera en su app. */
    @Transactional
    public void tareaReasignada(Traslado traslado, Personal anterior) {
        avisos.save(AvisoOperacion.paraPersonal(anterior, TipoAvisoOperacion.TAREA_REASIGNADA,
                "La tarea " + traslado.getFolio() + " se reasignó",
                "Ya no está en tu lista. Renta " + traslado.getRenta().getFolio() + ", "
                        + cliente(traslado.getRenta().getCliente()) + ".",
                traslado.getRenta(), LocalDateTime.now()));
    }

    // ---- oficina ----

    @Transactional(readOnly = true)
    public List<AvisoOperacionResponse> listar(String correo) {
        return avisos.findTop20ByUsuarioIdOrderByFechaDesc(usuario(correo).getId()).stream()
                .map(AvisoOperacionResponse::de)
                .toList();
    }

    /** Marca todos los avisos del usuario como leídos y devuelve la lista ya actualizada. */
    @Transactional
    public List<AvisoOperacionResponse> marcarLeidos(String correo) {
        avisos.marcarLeidosDeUsuario(usuario(correo).getId());
        return listar(correo);
    }

    /** Marca un aviso como leído y devuelve la lista ya actualizada. Un aviso ajeno no se modifica. */
    @Transactional
    public List<AvisoOperacionResponse> marcarLeido(Long id, String correo) {
        avisos.marcarLeidoDeUsuario(id, usuario(correo).getId());
        return listar(correo);
    }

    // ---- app móvil ----

    @Transactional(readOnly = true)
    public List<AvisoOperacionResponse> pendientesDe(Long personalId) {
        return avisos.findByPersonalIdAndLeidoFalseOrderByFechaDesc(personalId).stream()
                .map(AvisoOperacionResponse::de)
                .toList();
    }

    @Transactional
    public void marcarLeidosDe(Long personalId) {
        avisos.marcarLeidosDePersonal(personalId);
    }

    // ---- apoyo ----

    /** Un aviso para cada auxiliar activo: cada quien lo marca como leído por su lado. */
    private void avisarOficina(TipoAvisoOperacion tipo, Traslado traslado, String titulo, String detalle) {
        LocalDateTime ahora = LocalDateTime.now();
        List<AvisoOperacion> nuevos = usuarios.findAll().stream()
                .filter(u -> u.getTipo() == TipoUsuario.AUXILIAR && u.isActivo())
                .map(u -> AvisoOperacion.paraUsuario(u, tipo, titulo, detalle, traslado.getRenta(), ahora))
                .toList();
        avisos.saveAll(nuevos);
    }

    private Usuario usuario(String correo) {
        return usuarios.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar"));
    }

    private static String operador(Traslado traslado) {
        Personal p = traslado.getResponsable();
        return p == null ? "El operador" : (p.getNombres() + " " + p.getApellidoPaterno()).trim();
    }

    private static String equipos(Renta renta) {
        long total = renta.getEquipos().stream().filter(er -> er.getFase() != FaseEquipoRenta.CANCELADO).count();
        return total + (total == 1 ? " equipo" : " equipos");
    }

    private static String renta(Traslado traslado) {
        return "Renta " + traslado.getRenta().getFolio() + ", " + cliente(traslado.getRenta().getCliente()) + ".";
    }

    private static String cliente(Cliente c) {
        return c.getEmpresa() != null && !c.getEmpresa().isBlank()
                ? c.getEmpresa()
                : (c.getNombre() + " " + c.getApellidoPaterno()).trim();
    }
}