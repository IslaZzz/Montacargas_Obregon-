package mx.montacargasobregon.backend.equipos.consulta;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.equipos.repositorio.EquipoRepository;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;

/**
 * Consulta de equipos por estado.
 * Sin fechas muestra cómo está cada equipo hoy; con fechas, cómo estará en ese periodo.
 */
@Service
public class ConsultaEquiposService {

    private static final List<EstadoRenta> VIGENTES = List.of(EstadoRenta.PROGRAMADA, EstadoRenta.EN_CURSO);
    private static final List<FaseEquipoRenta> LIBRES = List.of(FaseEquipoRenta.CANCELADO, FaseEquipoRenta.COMPLETADO);

    private final EquipoRepository equipoRepositorio;
    private final ConsultaEquiposRepository repositorio;

    public ConsultaEquiposService(EquipoRepository equipoRepositorio, ConsultaEquiposRepository repositorio) {
        this.equipoRepositorio = equipoRepositorio;
        this.repositorio = repositorio;
    }

    /** Todos los equipos, menos los dados de baja, con su estado. Las fechas van juntas o ninguna. */
    @Transactional(readOnly = true)
    public List<EquipoEstadoResponse> consultar(LocalDate inicio, LocalDate fin) {
        boolean conPeriodo = inicio != null || fin != null;
        if (conPeriodo) {
            validarPeriodo(inicio, fin);
        }

        List<EquipoRenta> enRentas = conPeriodo
                ? repositorio.enRentasDelPeriodo(FaseEquipoRenta.EN_REVISION_ENTRADA, LIBRES, VIGENTES, inicio, fin)
                : repositorio.enRentasVigentes(LIBRES, VIGENTES);

        // id del equipo -> la renta que define su estado
        Map<Long, EquipoRenta> rentaDe = new HashMap<>();
        enRentas.forEach(er -> rentaDe.merge(er.getEquipo().getId(), er, ConsultaEquiposService::masRelevante));

        return equipoRepositorio.findAll().stream()
                .filter(e -> e.getEstado() != EstadoEquipo.BAJA)
                .sorted(Comparator.comparing(Equipo::getNumeroUnidad))
                .map(e -> {
                    EquipoRenta enRenta = rentaDe.get(e.getId());
                    return EquipoEstadoResponse.de(e, estadoDe(e, enRenta), enRenta);
                })
                .toList();
    }

    // Mantenimiento y fuera de servicio son del equipo y mandan sobre la renta
    private static EstadoConsultaEquipo estadoDe(Equipo equipo, EquipoRenta enRenta) {
        if (equipo.getEstado() == EstadoEquipo.FUERA_SERVICIO) {
            return EstadoConsultaEquipo.FUERA_SERVICIO;
        }
        if (equipo.getEstado() == EstadoEquipo.EN_MANTENIMIENTO) {
            return EstadoConsultaEquipo.EN_MANTENIMIENTO;
        }
        if (enRenta == null) {
            return EstadoConsultaEquipo.DISPONIBLE;
        }
        return enRenta.getFase() == FaseEquipoRenta.EN_OPERACION
                ? EstadoConsultaEquipo.EN_RENTA
                : EstadoConsultaEquipo.EN_PREPARACION;
    }

    // Si está en dos rentas, cuenta la que tiene el cliente; si no, la que empieza antes
    private static EquipoRenta masRelevante(EquipoRenta a, EquipoRenta b) {
        boolean aConCliente = a.getFase() == FaseEquipoRenta.EN_OPERACION;
        boolean bConCliente = b.getFase() == FaseEquipoRenta.EN_OPERACION;
        if (aConCliente != bConCliente) {
            return aConCliente ? a : b;
        }
        return a.getRenta().getFechaInicio().isAfter(b.getRenta().getFechaInicio()) ? b : a;
    }

    private static void validarPeriodo(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica la fecha de inicio y la de fin");
        }
        if (fin.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de fin no puede ser anterior a la de inicio");
        }
    }
}