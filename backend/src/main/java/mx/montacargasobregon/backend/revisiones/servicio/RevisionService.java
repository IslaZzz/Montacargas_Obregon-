package mx.montacargasobregon.backend.revisiones.servicio;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.servicio.PersonalService;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.revisiones.dto.AsignarRevisionesRequest;
import mx.montacargasobregon.backend.revisiones.dto.RegistrarResultadoRequest;
import mx.montacargasobregon.backend.revisiones.dto.RevisionesRentaResponse;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.ResultadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;

/** Reglas de negocio de las revisiones. */
@Service
public class RevisionService {

    private final RevisionRepository repositorio;
    private final PersonalService personalServicio;
    private final MantenimientoRepository mantenimientoRepositorio;
    private final AvanceRentaService avanceRenta;

    public RevisionService(RevisionRepository repositorio, PersonalService personalServicio,
                           MantenimientoRepository mantenimientoRepositorio, AvanceRentaService avanceRenta) {
        this.repositorio = repositorio;
        this.personalServicio = personalServicio;
        this.mantenimientoRepositorio = mantenimientoRepositorio;
        this.avanceRenta = avanceRenta;
    }

    /** Las revisiones agrupadas por renta y tipo, de la más próxima a la más lejana. */
    @Transactional(readOnly = true)
    public List<RevisionesRentaResponse> listarPorRenta() {
        // LinkedHashMap conserva el orden de la consulta
        Map<String, List<Revision>> grupos = repositorio.listar().stream()
                .collect(Collectors.groupingBy(
                        r -> r.getEquipoRenta().getRenta().getId() + "-" + r.getTipo(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return grupos.values().stream()
                .map(RevisionesRentaResponse::deGrupo)
                .toList();
    }

    /**
     * Asigna o cambia el mecánico de una o varias revisiones.
     * Si alguna no se puede, no se asigna ninguna. Devuelve la lista actualizada.
     */
    @Transactional
    public List<RevisionesRentaResponse> asignar(AsignarRevisionesRequest datos) {
        Personal mecanico = personalServicio.mecanicoActivo(datos.personalId());

        Set<Long> ids = new HashSet<>(datos.revisionIds());
        List<Revision> revisiones = repositorio.findAllById(ids);
        if (revisiones.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Una o más revisiones no existen");
        }

        // Primero se revisan todas y luego se asignan, para no dejar la mitad hecha
        for (Revision r : revisiones) {
            if (!r.puedeAsignarse()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La revisión de " + r.getEquipoRenta().getEquipo().getNumeroUnidad()
                                + " ya está terminada o cancelada y no se puede asignar");
            }
        }

        revisiones.forEach(r -> r.asignar(mecanico));
        return listarPorRenta();
    }

    /**
     * Registra lo que reportó el mecánico y da la revisión por terminada.
     * Requiere mantenimiento: nace un mantenimiento por asignar y el equipo queda en mantenimiento.
     * Salida sin daño: el equipo queda listo para entrega (y, si es el último, nace el traslado).
     * Entrada: el equipo queda fuera de la renta (y, si es el último, la renta se cierra).
     * Devuelve la lista actualizada.
     */
    @Transactional
    public List<RevisionesRentaResponse> registrarResultado(Long id, RegistrarResultadoRequest datos) {
        Revision revision = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Revisión no encontrada"));

        String unidad = revision.getEquipoRenta().getEquipo().getNumeroUnidad();
        if (!revision.puedeRegistrarResultado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    revision.getEstado() == EstadoRevision.PENDIENTE
                            ? "Primero asigna un mecánico a la revisión de " + unidad
                            : "La revisión de " + unidad + " ya está terminada o cancelada");
        }

        String notas = datos.notas() == null || datos.notas().isBlank() ? null : datos.notas().trim();
        boolean requiereMantenimiento = datos.resultado() == ResultadoRevision.REQUIERE_MANTENIMIENTO;
        if (requiereMantenimiento && notas == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Describe la falla en las notas para crear el mantenimiento");
        }

        revision.registrarResultado(datos.resultado(), notas);
        EquipoRenta equipoRenta = revision.getEquipoRenta();

        if (requiereMantenimiento) {
            equipoRenta.getEquipo().cambiarEstado(EstadoEquipo.EN_MANTENIMIENTO);
            // En la salida tiene que quedar antes de la entrega; en la entrada no hay fecha
            LocalDateTime limite = revision.getTipo() == TipoRevision.SALIDA ? revision.getFechaProgramada() : null;
            mantenimientoRepositorio.save(Mantenimiento.deRevision(revision, notas, limite));
        }

        if (revision.getTipo() == TipoRevision.SALIDA) {
            if (!requiereMantenimiento) {
                avanceRenta.equipoListoParaEntrega(equipoRenta);
            }
        } else {
            avanceRenta.equipoDevuelto(equipoRenta);
        }
        return listarPorRenta();
    }
}