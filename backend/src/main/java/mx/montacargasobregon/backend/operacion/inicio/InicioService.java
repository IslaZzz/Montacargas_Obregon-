package mx.montacargasobregon.backend.operacion.inicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.operacion.inicio.InicioResponse.Aviso;
import mx.montacargasobregon.backend.operacion.inicio.InicioResponse.Operacion;
import mx.montacargasobregon.backend.operacion.inicio.InicioResponse.Pendientes;
import mx.montacargasobregon.backend.operacion.inicio.InicioResponse.TipoAviso;
import mx.montacargasobregon.backend.operacion.inicio.InicioResponse.TipoOperacion;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.rentas.repositorio.RentaRepository;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

/**
 * Pantalla de inicio: pendientes de hoy, avisos y próximas operaciones.
 * Junta traslados, revisiones y mantenimientos; solo lee, no cambia nada.
 */
@Service
public class InicioService {

    // Hasta cuántos días adelante se muestran las operaciones
    private static final int DIAS_PROXIMAS = 60;
    // Con cuántos días de anticipación se avisa que falta confirmar la recolección
    private static final int DIAS_AVISO_RECOLECCION = 2;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");

    private final TrasladoRepository trasladoRepositorio;
    private final RevisionRepository revisionRepositorio;
    private final MantenimientoRepository mantenimientoRepositorio;
    private final RentaRepository rentaRepositorio;

    public InicioService(TrasladoRepository trasladoRepositorio, RevisionRepository revisionRepositorio,
                         MantenimientoRepository mantenimientoRepositorio, RentaRepository rentaRepositorio) {
        this.trasladoRepositorio = trasladoRepositorio;
        this.revisionRepositorio = revisionRepositorio;
        this.mantenimientoRepositorio = mantenimientoRepositorio;
        this.rentaRepositorio = rentaRepositorio;
    }

    @Transactional(readOnly = true)
    public InicioResponse consultar() {
        return consultar(LocalDateTime.now());
    }

    // Recibe la hora para poder probarlo con una fecha fija
    InicioResponse consultar(LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();

        List<Traslado> traslados = trasladoRepositorio.listarTodosConRelaciones().stream()
                .filter(t -> t.getEstado() == EstadoTraslado.PROGRAMADO || t.getEstado() == EstadoTraslado.EN_CURSO)
                .toList();
        List<Revision> revisiones = revisionRepositorio.listar().stream()
                .filter(Revision::puedeAsignarse)
                .toList();
        List<Mantenimiento> mantenimientos = mantenimientoRepositorio.listar().stream()
                .filter(m -> m.getEstado() == EstadoMantenimiento.PENDIENTE
                        || m.getEstado() == EstadoMantenimiento.ASIGNADO
                        || m.getEstado() == EstadoMantenimiento.EN_PROCESO)
                .toList();
        List<Renta> rentas = rentaRepositorio.findAll();

        Pendientes pendientes = new Pendientes(
                contarDeHoy(traslados, TipoTraslado.ENTREGA, hoy),
                contarDeHoy(traslados, TipoTraslado.RECOLECCION, hoy),
                (int) revisiones.stream()
                        .filter(r -> r.getFechaProgramada() != null && !r.getFechaProgramada().toLocalDate().isAfter(hoy))
                        .count(),
                mantenimientos.size());

        List<Aviso> avisos = new ArrayList<>();
        avisos.addAll(trasladosAtrasados(traslados, ahora));
        avisos.addAll(trasladosSinOperador(traslados, ahora));
        avisos.addAll(entregasEnRiesgo(rentas, hoy));
        avisos.addAll(mantenimientosVencidos(mantenimientos, hoy));
        avisos.addAll(recoleccionesSinConfirmar(rentas, hoy));

        return new InicioResponse(hoy, pendientes, avisos, proximas(traslados, revisiones, mantenimientos, hoy));
    }

    private static int contarDeHoy(List<Traslado> traslados, TipoTraslado tipo, LocalDate hoy) {
        return (int) traslados.stream()
                .filter(t -> t.getTipo() == tipo && t.getFechaHoraInicio().toLocalDate().equals(hoy))
                .count();
    }

    // ===== Avisos =====

    // Ya pasó su horario y nadie lo marcó realizado
    private static List<Aviso> trasladosAtrasados(List<Traslado> traslados, LocalDateTime ahora) {
        return traslados.stream()
                .filter(t -> t.getFechaHoraFin().isBefore(ahora))
                .map(t -> new Aviso(TipoAviso.TRASLADO_ATRASADO,
                        nombreTraslado(t) + " sin marcar realizada - " + t.getFolio(),
                        "Renta " + t.getRenta().getFolio() + " · era "
                                + (t.getFechaHoraInicio().toLocalDate().equals(ahora.toLocalDate())
                                ? "hoy"
                                : "el " + t.getFechaHoraInicio().format(DIA))
                                + " de " + horario(t.getFechaHoraInicio(), t.getFechaHoraFin()),
                        null, "/traslados"))
                .toList();
    }

    // Es hoy, todavía no pasa y no tiene quién lo haga
    private static List<Aviso> trasladosSinOperador(List<Traslado> traslados, LocalDateTime ahora) {
        return traslados.stream()
                .filter(t -> t.getResponsable() == null
                        && t.getFechaHoraInicio().toLocalDate().equals(ahora.toLocalDate())
                        && !t.getFechaHoraFin().isBefore(ahora))
                .map(t -> new Aviso(TipoAviso.TRASLADO_SIN_OPERADOR,
                        nombreTraslado(t) + " de hoy sin operador - " + t.getFolio(),
                        "Renta " + t.getRenta().getFolio() + " · " + horario(t.getFechaHoraInicio(), t.getFechaHoraFin()),
                        null, "/traslados"))
                .toList();
    }

    // La entrega es hoy o mañana y todavía hay equipos sin estar listos
    private static List<Aviso> entregasEnRiesgo(List<Renta> rentas, LocalDate hoy) {
        List<Aviso> avisos = new ArrayList<>();
        for (Renta renta : rentas) {
            if (renta.getEstado() != EstadoRenta.PROGRAMADA || renta.getEntregaDesde() == null) {
                continue;
            }
            LocalDate dia = renta.getEntregaDesde().toLocalDate();
            if (dia.isBefore(hoy) || dia.isAfter(hoy.plusDays(1))) {
                continue;
            }
            long total = renta.getEquipos().stream().filter(er -> er.getFase() != FaseEquipoRenta.CANCELADO).count();
            long listos = renta.getEquipos().stream().filter(er -> er.getFase() == FaseEquipoRenta.LISTO_ENTREGA).count();
            long faltan = total - listos;
            if (faltan > 0) {
                avisos.add(new Aviso(TipoAviso.ENTREGA_EN_RIESGO,
                        "Entrega " + (dia.equals(hoy) ? "hoy" : "mañana") + " - Renta " + renta.getFolio(),
                        (faltan == 1 ? "Falta 1" : "Faltan " + faltan) + " de " + total + " equipos por preparar · "
                                + horario(renta.getEntregaDesde(), renta.getEntregaHasta()),
                        renta.getId(), null));
            }
        }
        return avisos;
    }

    // Su fecha límite es hoy o ya pasó
    private static List<Aviso> mantenimientosVencidos(List<Mantenimiento> mantenimientos, LocalDate hoy) {
        return mantenimientos.stream()
                .filter(m -> m.getFechaLimite() != null && !m.getFechaLimite().toLocalDate().isAfter(hoy))
                .map(m -> new Aviso(TipoAviso.MANTENIMIENTO_VENCIDO,
                        (m.getFechaLimite().toLocalDate().isBefore(hoy) ? "Mantenimiento vencido - " : "Mantenimiento vence hoy - ")
                                + m.getEquipo().getNumeroUnidad(),
                        m.getDescripcionFalla() + (m.getMecanico() == null ? " · Sin mecánico" : ""),
                        null, "/mantenimientos"))
                .toList();
    }

    // La renta está por terminar y no se ha acordado cuándo recoger
    private static List<Aviso> recoleccionesSinConfirmar(List<Renta> rentas, LocalDate hoy) {
        return rentas.stream()
                .filter(r -> r.getEstado() == EstadoRenta.EN_CURSO
                        && r.getRecoleccionDesde() == null
                        && !r.getFechaFin().isAfter(hoy.plusDays(DIAS_AVISO_RECOLECCION)))
                .map(r -> new Aviso(TipoAviso.FALTA_RECOLECCION,
                        "Falta confirmar la recolección - Renta " + r.getFolio(),
                        (r.getFechaFin().isBefore(hoy)
                                ? "Terminó el " + r.getFechaFin().format(DIA)
                                : r.getFechaFin().equals(hoy) ? "Termina hoy" : "Termina el " + r.getFechaFin().format(DIA))
                                + ". Acuerda con el cliente cuándo recoger.",
                        r.getId(), null))
                .toList();
    }

    // ===== Próximas operaciones =====

    private static List<Operacion> proximas(List<Traslado> traslados, List<Revision> revisiones,
                                            List<Mantenimiento> mantenimientos, LocalDate hoy) {
        LocalDate limite = hoy.plusDays(DIAS_PROXIMAS);
        Predicate<LocalDateTime> enRango = f -> f != null
                && !f.toLocalDate().isBefore(hoy) && !f.toLocalDate().isAfter(limite);
        List<Operacion> operaciones = new ArrayList<>();

        traslados.stream()
                .filter(t -> enRango.test(t.getFechaHoraInicio()))
                .forEach(t -> operaciones.add(new Operacion(
                        t.getTipo() == TipoTraslado.ENTREGA ? TipoOperacion.ENTREGA : TipoOperacion.RECOLECCION,
                        nombreTraslado(t),
                        "Renta " + t.getRenta().getFolio() + " · " + t.getFolio()
                                + (t.getResponsable() == null ? " · Sin operador" : ""),
                        t.getFechaHoraInicio(), t.getFechaHoraFin(), t.getRenta().getId(), "/traslados")));

        // Las revisiones de una renta del mismo tipo y día van juntas
        Map<String, List<Revision>> grupos = new LinkedHashMap<>();
        revisiones.stream()
                .filter(r -> enRango.test(r.getFechaProgramada()))
                .forEach(r -> grupos.computeIfAbsent(
                        r.getEquipoRenta().getRenta().getId() + "-" + r.getTipo() + "-" + r.getFechaProgramada().toLocalDate(),
                        k -> new ArrayList<>()).add(r));
        grupos.values().forEach(grupo -> {
            Revision primera = grupo.get(0);
            Renta renta = primera.getEquipoRenta().getRenta();
            operaciones.add(new Operacion(TipoOperacion.REVISION,
                    primera.getTipo() == TipoRevision.SALIDA ? "Revisión de salida" : "Revisión de entrada",
                    "Renta " + renta.getFolio() + " · " + grupo.size() + (grupo.size() == 1 ? " equipo" : " equipos"),
                    grupo.stream().map(Revision::getFechaProgramada).min(Comparator.naturalOrder()).orElseThrow(),
                    null, renta.getId(), "/revisiones"));
        });

        mantenimientos.stream()
                .filter(m -> enRango.test(m.getFechaLimite()))
                .forEach(m -> operaciones.add(new Operacion(TipoOperacion.MANTENIMIENTO,
                        "Mantenimiento",
                        m.getEquipo().getNumeroUnidad() + " · " + m.getDescripcionFalla(),
                        m.getFechaLimite(), null,
                        m.getRevision() == null ? null : m.getRevision().getEquipoRenta().getRenta().getId(),
                        "/mantenimientos")));

        operaciones.sort(Comparator.comparing(Operacion::desde));
        return operaciones;
    }

    private static String nombreTraslado(Traslado t) {
        return t.getTipo() == TipoTraslado.ENTREGA ? "Entrega" : "Recolección";
    }

    private static String horario(LocalDateTime desde, LocalDateTime hasta) {
        return desde.format(HORA) + " a " + hasta.format(HORA);
    }
}