package mx.montacargasobregon.backend.movil.mantenimientos;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.TareaMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.mantenimientos.servicio.MantenimientoService;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;

/**
 * Los mantenimientos del mecánico en la app móvil (OHU18 y OHU19): ver los suyos, atender cada tarea,
 * agregar lo que encontró, avisar si algo no se pudo y finalizar. Al finalizar,
 * MantenimientoService deja el equipo operativo y avanza la renta.
 */
@Service
public class MantenimientosMecanicoService {

    /** Las fotos que se pueden tomar en cada tarea. Son opcionales. */
    public static final List<String> FOTOS_POR_TAREA = List.of("Antes", "Después", "Refacción", "Detalle");

    private static final Set<EstadoMantenimiento> ABIERTOS =
            EnumSet.of(EstadoMantenimiento.ASIGNADO, EstadoMantenimiento.EN_PROCESO);

    private final MantenimientoRepository repositorio;
    private final MantenimientoService mantenimientoServicio;

    public MantenimientosMecanicoService(MantenimientoRepository repositorio,
                                         MantenimientoService mantenimientoServicio) {
        this.repositorio = repositorio;
        this.mantenimientoServicio = mantenimientoServicio;
    }

    /** Sus mantenimientos abiertos: nuevos (asignados) y en curso. */
    @Transactional(readOnly = true)
    public List<MantenimientoMecanicoResponse> misMantenimientos(PersonalActual personal) {
        return repositorio.buscarPorMecanico(personal.id(), ABIERTOS).stream()
                .map(MantenimientoMecanicoResponse::de)
                .toList();
    }

    /** Un mantenimiento suyo, abierto o ya terminado (para el historial). */
    @Transactional(readOnly = true)
    public DetalleMantenimientoMecanicoResponse detalle(Long id, PersonalActual personal) {
        return detalleDe(buscarMio(id, personal));
    }

    /** El mecánico lo abrió: queda en curso y con sus tareas. */
    @Transactional
    public DetalleMantenimientoMecanicoResponse iniciar(Long id, PersonalActual personal) {
        Mantenimiento m = buscarAbierto(id, personal);
        m.iniciar();
        repositorio.flush(); // Para que las tareas nuevas ya traigan su id
        return detalleDe(m);
    }

    /** Registra (o corrige) lo que se hizo en una tarea. */
    @Transactional
    public DetalleMantenimientoMecanicoResponse atender(Long id, Long tareaId, PersonalActual personal,
                                                        AtenderTareaRequest datos) {
        Mantenimiento m = buscarAbierto(id, personal);
        m.iniciar();
        tareaDe(m, tareaId).atender(datos.trabajoRealizado().trim(), limpiar(datos.refacciones()),
                limpiar(datos.tiempoInvertido()), limpiar(datos.observaciones()));
        return detalleDe(m);
    }

    /** Agrega lo que el mecánico encontró y no venía en el mantenimiento. */
    @Transactional
    public DetalleMantenimientoMecanicoResponse agregar(Long id, PersonalActual personal, AgregarTareaRequest datos) {
        Mantenimiento m = buscarAbierto(id, personal);
        m.iniciar();
        m.agregarTarea(datos.categoria().trim(), limpiar(datos.componente()), limpiar(datos.accionSugerida()));
        repositorio.flush();
        return detalleDe(m);
    }

    /** No se pudo completar la tarea: queda pendiente con el aviso y el mantenimiento sigue abierto. */
    @Transactional
    public DetalleMantenimientoMecanicoResponse reportarIncidencia(Long id, Long tareaId, PersonalActual personal,
                                                                   IncidenciaRequest datos) {
        Mantenimiento m = buscarAbierto(id, personal);
        TareaMantenimiento tarea = tareaDe(m, tareaId);
        if (tarea.atendida()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esa tarea ya está atendida.");
        }
        tarea.reportarIncidencia(datos.mensaje().trim());
        return detalleDe(m);
    }

    /** Todas las tareas atendidas: el mantenimiento termina y el equipo vuelve a estar operativo. */
    @Transactional
    public DetalleMantenimientoMecanicoResponse finalizar(Long id, PersonalActual personal) {
        Mantenimiento m = buscarAbierto(id, personal);
        if (!m.tareasCompletas()) {
            long faltan = m.getTareas().stream().filter(t -> !t.atendida()).count();
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    faltan == 1 ? "Falta atender 1 tarea." : "Faltan atender " + faltan + " tareas.");
        }
        mantenimientoServicio.terminar(id);
        return detalleDe(m);
    }

    // ---- apoyo ----

    private Mantenimiento buscarMio(Long id, PersonalActual personal) {
        Mantenimiento m = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El mantenimiento no existe."));
        if (m.getMecanico() == null || !m.getMecanico().getId().equals(personal.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ese mantenimiento no está asignado a ti.");
        }
        return m;
    }

    private Mantenimiento buscarAbierto(Long id, PersonalActual personal) {
        Mantenimiento m = buscarMio(id, personal);
        if (!ABIERTOS.contains(m.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El mantenimiento de " + m.getEquipo().getNumeroUnidad() + " ya está terminado o cancelado.");
        }
        return m;
    }

    private static TareaMantenimiento tareaDe(Mantenimiento m, Long tareaId) {
        return m.getTareas().stream()
                .filter(t -> t.getId() != null && t.getId().equals(tareaId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La tarea no existe."));
    }

    private static DetalleMantenimientoMecanicoResponse detalleDe(Mantenimiento m) {
        return DetalleMantenimientoMecanicoResponse.de(m, FOTOS_POR_TAREA);
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}