package mx.montacargasobregon.backend.rentas.captura;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.clientes.repositorio.ClienteRepository;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.equipos.repositorio.EquipoRepository;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

/**
 * Captura manual de rentas y confirmación del horario de entrega.
 * Es temporal: cuando comercial genere la renta desde la cotización, este paquete se puede borrar.
 */
@Service
public class CapturaRentaService {

    // Rentas que todavía ocupan sus equipos
    private static final List<EstadoRenta> VIGENTES = List.of(EstadoRenta.PROGRAMADA, EstadoRenta.EN_CURSO);

    private final CapturaRentaRepository repositorio;
    private final ClienteRepository clienteRepositorio;
    private final EquipoRepository equipoRepositorio;
    private final RevisionRepository revisionRepositorio;
    private final TrasladoRepository trasladoRepositorio;

    public CapturaRentaService(CapturaRentaRepository repositorio, ClienteRepository clienteRepositorio,
                               EquipoRepository equipoRepositorio, RevisionRepository revisionRepositorio,
                               TrasladoRepository trasladoRepositorio) {
        this.repositorio = repositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.equipoRepositorio = equipoRepositorio;
        this.revisionRepositorio = revisionRepositorio;
        this.trasladoRepositorio = trasladoRepositorio;
    }

    /** Equipos operativos que no están en otra renta en esas fechas. */
    @Transactional(readOnly = true)
    public List<EquipoDisponibleResponse> equiposDisponibles(LocalDate inicio, LocalDate fin) {
        validarPeriodo(inicio, fin);
        Set<Long> ocupados = new HashSet<>(
                repositorio.equiposOcupados(inicio, fin, VIGENTES, FaseEquipoRenta.CANCELADO));

        return equipoRepositorio.findAll().stream()
                .filter(e -> e.getEstado() == EstadoEquipo.OPERATIVO && !ocupados.contains(e.getId()))
                .sorted(Comparator.comparing(Equipo::getNumeroUnidad))
                .map(EquipoDisponibleResponse::deEquipo)
                .toList();
    }

    /**
     * Crea la renta en PROGRAMADA con sus equipos en RESERVADO.
     * Si trae horario de entrega, lo confirma en el mismo paso.
     */
    @Transactional
    public RentaCapturadaResponse crear(CrearRentaRequest datos) {
        validarPeriodo(datos.fechaInicio(), datos.fechaFin());
        boolean conEntrega = datos.entregaDesde() != null || datos.entregaHasta() != null;
        if (conEntrega) {
            validarHorario(datos.entregaDesde(), datos.entregaHasta(), datos.fechaFin());
        }

        Cliente cliente = clienteRepositorio.findById(datos.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        // Sin repetidos, por si el front manda el mismo equipo dos veces
        Set<Long> ids = new LinkedHashSet<>(datos.equipoIds());
        List<Equipo> equipos = equipoRepositorio.findAllById(ids);
        if (equipos.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uno o más equipos no existen");
        }

        List<String> noOperativos = equipos.stream()
                .filter(e -> e.getEstado() != EstadoEquipo.OPERATIVO)
                .map(Equipo::getNumeroUnidad)
                .sorted()
                .toList();
        if (!noOperativos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Estos equipos no están operativos: " + String.join(", ", noOperativos));
        }

        Set<Long> ocupados = new HashSet<>(repositorio.equiposOcupados(
                datos.fechaInicio(), datos.fechaFin(), VIGENTES, FaseEquipoRenta.CANCELADO));
        List<String> enOtraRenta = equipos.stream()
                .filter(e -> ocupados.contains(e.getId()))
                .map(Equipo::getNumeroUnidad)
                .sorted()
                .toList();
        if (!enOtraRenta.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Estos equipos ya están en otra renta en esas fechas: " + String.join(", ", enOtraRenta));
        }

        Renta renta = new Renta(siguienteFolio(), cliente, datos.fechaInicio(), datos.fechaFin(),
                datos.direccionObra().trim(), datos.contactoObra().trim(), datos.telefonoContacto());
        equipos.forEach(renta::agregarEquipo);
        renta = repositorio.save(renta);

        if (conEntrega) {
            programarEntrega(renta, datos.entregaDesde(), datos.entregaHasta());
        }
        return RentaCapturadaResponse.deRenta(renta);
    }

    /** Confirma el horario de entrega de una renta que todavía no lo tiene. */
    @Transactional
    public RentaCapturadaResponse confirmarEntrega(Long id, HorarioRequest datos) {
        Renta renta = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Renta no encontrada"));

        if (renta.getEstado() != EstadoRenta.PROGRAMADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede confirmar la entrega de una renta programada");
        }
        if (renta.getEntregaDesde() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La renta " + renta.getFolio() + " ya tiene horario de entrega");
        }
        validarHorario(datos.desde(), datos.hasta(), renta.getFechaFin());

        programarEntrega(renta, datos.desde(), datos.hasta());
        return RentaCapturadaResponse.deRenta(renta);
    }

    /**
     * Guarda el horario y crea lo que nace con él:
     * una revisión de salida por equipo y el traslado de entrega, todos pendientes.
     */
    private void programarEntrega(Renta renta, LocalDateTime desde, LocalDateTime hasta) {
        renta.confirmarEntrega(desde, hasta);

        for (EquipoRenta equipoRenta : renta.getEquipos()) {
            revisionRepositorio.save(new Revision(equipoRenta, TipoRevision.SALIDA, desde));
        }
        trasladoRepositorio.save(new Traslado(siguienteFolioTraslado(), renta, TipoTraslado.ENTREGA, desde, hasta));
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

    private static void validarHorario(LocalDateTime desde, LocalDateTime hasta, LocalDate fechaFin) {
        if (desde == null || hasta == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indica cuándo empieza y cuándo termina el horario de entrega");
        }
        if (!desde.isBefore(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El horario de entrega debe terminar después de empezar");
        }
        if (desde.toLocalDate().isAfter(fechaFin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La entrega no puede ser después de que termine la renta");
        }
    }

    // R-2090 -> R-2091
    private String siguienteFolio() {
        Integer ultimo = repositorio.ultimoNumeroFolio();
        return String.format("R-%04d", ultimo == null ? 1 : ultimo + 1);
    }

    // TR-0086 -> TR-0087
    private String siguienteFolioTraslado() {
        Integer ultimo = repositorio.ultimoNumeroFolioTraslado();
        return String.format("TR-%04d", ultimo == null ? 1 : ultimo + 1);
    }
}