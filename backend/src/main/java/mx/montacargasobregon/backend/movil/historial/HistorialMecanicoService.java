package mx.montacargasobregon.backend.movil.historial;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;

/** Lo que el mecánico ya terminó, revisiones y mantenimientos juntos, lo más reciente primero. */
@Service
public class HistorialMecanicoService {

    /** Hasta dónde se ve hacia atrás, para que la lista no crezca sin fin. */
    static final int DIAS = 30;

    private final RevisionRepository revisionRepositorio;
    private final MantenimientoRepository mantenimientoRepositorio;

    public HistorialMecanicoService(RevisionRepository revisionRepositorio,
                                    MantenimientoRepository mantenimientoRepositorio) {
        this.revisionRepositorio = revisionRepositorio;
        this.mantenimientoRepositorio = mantenimientoRepositorio;
    }

    @Transactional(readOnly = true)
    public List<HistorialMecanicoResponse> miHistorial(PersonalActual personal) {
        LocalDateTime desde = LocalDateTime.now().minusDays(DIAS);

        Stream<HistorialMecanicoResponse> revisiones = revisionRepositorio
                .buscarPorMecanico(personal.id(), EnumSet.of(EstadoRevision.COMPLETADA)).stream()
                .map(HistorialMecanicoResponse::de);
        Stream<HistorialMecanicoResponse> mantenimientos = mantenimientoRepositorio
                .buscarPorMecanico(personal.id(), EnumSet.of(EstadoMantenimiento.COMPLETADO)).stream()
                .map(HistorialMecanicoResponse::de);

        return Stream.concat(revisiones, mantenimientos)
                .filter(h -> h.terminado() != null && h.terminado().isAfter(desde))
                .sorted(Comparator.comparing(HistorialMecanicoResponse::terminado).reversed())
                .toList();
    }
}