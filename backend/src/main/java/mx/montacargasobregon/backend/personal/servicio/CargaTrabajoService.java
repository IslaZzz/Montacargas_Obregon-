package mx.montacargasobregon.backend.personal.servicio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.personal.dto.CargaPorMecanico;
import mx.montacargasobregon.backend.personal.dto.MecanicoCargaResponse;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;

/**
 * Carga de trabajo de los mecánicos. Suma revisiones y mantenimientos,
 * porque es la misma persona la que atiende los dos.
 */
@Service
public class CargaTrabajoService {

    // Lo que cuenta como trabajo pendiente de un mecánico
    private static final List<EstadoMantenimiento> MANTENIMIENTOS_SIN_TERMINAR =
            List.of(EstadoMantenimiento.ASIGNADO, EstadoMantenimiento.EN_PROCESO);
    private static final List<EstadoRevision> REVISIONES_SIN_TERMINAR =
            List.of(EstadoRevision.ASIGNADO, EstadoRevision.EN_PROCESO);

    private final PersonalRepository personalRepositorio;
    private final MantenimientoRepository mantenimientoRepositorio;
    private final RevisionRepository revisionRepositorio;

    public CargaTrabajoService(PersonalRepository personalRepositorio,
                               MantenimientoRepository mantenimientoRepositorio,
                               RevisionRepository revisionRepositorio) {
        this.personalRepositorio = personalRepositorio;
        this.mantenimientoRepositorio = mantenimientoRepositorio;
        this.revisionRepositorio = revisionRepositorio;
    }

    /** Mecánicos activos con su carga total, para elegir a quién asignar. */
    @Transactional(readOnly = true)
    public List<MecanicoCargaResponse> mecanicosConCarga() {
        // id del mecánico -> trabajos sin terminar
        Map<Long, Long> carga = new HashMap<>();
        sumar(carga, mantenimientoRepositorio.contarPorMecanico(MANTENIMIENTOS_SIN_TERMINAR));
        sumar(carga, revisionRepositorio.contarPorMecanico(REVISIONES_SIN_TERMINAR));

        return personalRepositorio.findByTipoAndActivoTrue(TipoPersonal.MECANICO).stream()
                .map(p -> new MecanicoCargaResponse(
                        p.getId(),
                        p.getCodigo(),
                        p.getNombres() + " " + p.getApellidoPaterno(),
                        carga.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    private static void sumar(Map<Long, Long> carga, List<CargaPorMecanico> conteos) {
        conteos.forEach(c -> carga.merge(c.mecanicoId(), c.total(), Long::sum));
    }
}