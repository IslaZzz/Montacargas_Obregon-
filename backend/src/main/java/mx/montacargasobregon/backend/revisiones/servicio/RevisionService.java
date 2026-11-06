package mx.montacargasobregon.backend.revisiones.servicio;

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

import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;
import mx.montacargasobregon.backend.revisiones.dto.AsignarRevisionesRequest;
import mx.montacargasobregon.backend.revisiones.dto.MecanicoCargaResponse;
import mx.montacargasobregon.backend.revisiones.dto.RevisionesRentaResponse;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;

/** Reglas de negocio de las revisiones. */
@Service
public class RevisionService {

    // Lo que cuenta como trabajo pendiente de un mecánico
    private static final List<EstadoRevision> SIN_TERMINAR =
            List.of(EstadoRevision.ASIGNADO, EstadoRevision.EN_PROCESO);

    private final RevisionRepository repositorio;
    private final PersonalRepository personalRepositorio;

    public RevisionService(RevisionRepository repositorio, PersonalRepository personalRepositorio) {
        this.repositorio = repositorio;
        this.personalRepositorio = personalRepositorio;
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

    /** Mecánicos activos con su carga de revisiones, para elegir a quién asignar. */
    @Transactional(readOnly = true)
    public List<MecanicoCargaResponse> mecanicosConCarga() {
        Map<Long, Long> carga = repositorio.findByEstadoIn(SIN_TERMINAR).stream()
                .filter(r -> r.getMecanico() != null)
                .collect(Collectors.groupingBy(r -> r.getMecanico().getId(), Collectors.counting()));

        return personalRepositorio.findByTipoAndActivoTrue(TipoPersonal.MECANICO).stream()
                .map(p -> new MecanicoCargaResponse(
                        p.getId(),
                        p.getCodigo(),
                        p.getNombres() + " " + p.getApellidoPaterno(),
                        carga.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    /**
     * Asigna o cambia el mecánico de una o varias revisiones.
     * Si alguna no se puede, no se asigna ninguna. Devuelve la lista actualizada.
     */
    @Transactional
    public List<RevisionesRentaResponse> asignar(AsignarRevisionesRequest datos) {
        Personal mecanico = personalRepositorio.findById(datos.personalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mecánico no encontrado"));

        if (!mecanico.isActivo() || mecanico.getTipo() != TipoPersonal.MECANICO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La persona elegida no es un mecánico activo");
        }

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
}