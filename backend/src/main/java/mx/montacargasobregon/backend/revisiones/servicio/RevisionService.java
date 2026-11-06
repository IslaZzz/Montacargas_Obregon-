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
import mx.montacargasobregon.backend.personal.servicio.PersonalService;
import mx.montacargasobregon.backend.revisiones.dto.AsignarRevisionesRequest;
import mx.montacargasobregon.backend.revisiones.dto.RevisionesRentaResponse;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;

/** Reglas de negocio de las revisiones. */
@Service
public class RevisionService {

    private final RevisionRepository repositorio;
    private final PersonalService personalServicio;

    public RevisionService(RevisionRepository repositorio, PersonalService personalServicio) {
        this.repositorio = repositorio;
        this.personalServicio = personalServicio;
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
}