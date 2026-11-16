package mx.montacargasobregon.backend.movil.traslados;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.cartas.modelo.CartaResponsiva;
import mx.montacargasobregon.backend.cartas.modelo.EstadoCarta;
import mx.montacargasobregon.backend.cartas.repositorio.CartaResponsivaRepository;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

/**
 * Las tareas del operador de traslado en la app móvil: ver las suyas, abrir el detalle,
 * iniciar el traslado y marcarlo como realizado.
 */
@Service
public class TareasTrasladoService {

    private final TrasladoRepository trasladoRepositorio;
    private final CartaResponsivaRepository cartaRepositorio;
    private final AvanceRentaService avanceRenta;

    public TareasTrasladoService(TrasladoRepository trasladoRepositorio,
                                 CartaResponsivaRepository cartaRepositorio,
                                 AvanceRentaService avanceRenta) {
        this.trasladoRepositorio = trasladoRepositorio;
        this.cartaRepositorio = cartaRepositorio;
        this.avanceRenta = avanceRenta;
    }

    /**
     * Las tareas pendientes y en camino del operador, del tipo que pida (entregas o recolecciones).
     * Las ya realizadas salen de la lista.
     */
    @Transactional(readOnly = true)
    public List<TareaResponse> misTareas(PersonalActual personal, TipoTraslado tipo) {
        List<Traslado> tareas = trasladoRepositorio.buscarPorOperador(personal.id()).stream()
                .filter(t -> t.getTipo() == tipo)
                .filter(t -> t.getEstado() == EstadoTraslado.PROGRAMADO || t.getEstado() == EstadoTraslado.EN_CURSO)
                .toList();

        Map<Long, CartaResponsiva> cartas = cartasDe(tareas);
        return tareas.stream()
                .map(t -> TareaResponse.de(t, firmaEnSitio(t, cartas.get(t.getRenta().getId()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public DetalleTareaResponse detalle(Long id, PersonalActual personal) {
        return detalleDe(buscarMia(id, personal));
    }

    /**
     * El operador sale con los equipos. Pide que todos estén preparados: para una entrega,
     * con su revisión de salida hecha; para una recolección, que sigan con el cliente.
     */
    @Transactional
    public DetalleTareaResponse iniciar(Long id, PersonalActual personal) {
        Traslado traslado = buscarMia(id, personal);

        if (traslado.getEstado() == EstadoTraslado.EN_CURSO) {
            throw conflicto("El traslado " + traslado.getFolio() + " ya está en curso.");
        }
        if (traslado.getEstado() != EstadoTraslado.PROGRAMADO) {
            throw conflicto("El traslado " + traslado.getFolio() + " ya no se puede iniciar.");
        }
        String bloqueo = motivoBloqueo(traslado);
        if (bloqueo != null) {
            throw conflicto(bloqueo);
        }

        traslado.iniciar();
        avanceRenta.trasladoIniciado(traslado);
        return detalleDe(traslado);
    }

    /** El operador llegó: los equipos quedan con el cliente, o regresan a revisión de entrada. */
    @Transactional
    public DetalleTareaResponse completar(Long id, PersonalActual personal) {
        Traslado traslado = buscarMia(id, personal);

        if (traslado.getEstado() != EstadoTraslado.EN_CURSO) {
            throw conflicto("Primero inicia el traslado " + traslado.getFolio() + ".");
        }

        traslado.completar();
        avanceRenta.trasladoRealizado(traslado);
        return detalleDe(traslado);
    }

    // ---- apoyo ----

    /** El traslado existe y es de quien lo pide; si no, no es su tarea. */
    private Traslado buscarMia(Long id, PersonalActual personal) {
        Traslado traslado = trasladoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La tarea no existe."));

        if (traslado.getResponsable() == null || !traslado.getResponsable().getId().equals(personal.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esa tarea no está asignada a ti.");
        }
        return traslado;
    }

    private DetalleTareaResponse detalleDe(Traslado traslado) {
        String bloqueo = motivoBloqueo(traslado);
        boolean programado = traslado.getEstado() == EstadoTraslado.PROGRAMADO;
        CartaResponsiva carta = cartaRepositorio.findByRentaId(traslado.getRenta().getId()).orElse(null);

        return DetalleTareaResponse.de(traslado, firmaEnSitio(traslado, carta), faseRequerida(traslado.getTipo()),
                programado && bloqueo == null,
                traslado.getEstado() == EstadoTraslado.EN_CURSO,
                programado ? bloqueo : null);
    }

    /** Qué fase deben tener los equipos para poder salir. */
    static FaseEquipoRenta faseRequerida(TipoTraslado tipo) {
        return tipo == TipoTraslado.ENTREGA ? FaseEquipoRenta.LISTO_ENTREGA : FaseEquipoRenta.EN_OPERACION;
    }

    /** Por qué no se puede iniciar todavía, o null si ya se puede. */
    private static String motivoBloqueo(Traslado traslado) {
        Renta renta = traslado.getRenta();

        if (traslado.getTipo() == TipoTraslado.RECOLECCION && renta.getEstado() != EstadoRenta.EN_CURSO) {
            return "La renta " + renta.getFolio() + " todavía no está en curso.";
        }

        FaseEquipoRenta requerida = faseRequerida(traslado.getTipo());
        List<String> faltan = renta.getEquipos().stream()
                .filter(er -> er.getFase() != FaseEquipoRenta.CANCELADO)
                .filter(er -> er.getFase() != requerida && er.getFase() != FaseEquipoRenta.EN_TRASLADO)
                .map(er -> er.getEquipo().getNumeroUnidad())
                .toList();

        if (faltan.isEmpty()) {
            return null;
        }
        if (traslado.getTipo() == TipoTraslado.ENTREGA) {
            return "Faltan equipos por preparar: " + String.join(", ", faltan) + ".";
        }
        return "Estos equipos ya no están con el cliente: " + String.join(", ", faltan) + ".";
    }

    /** En una entrega con carta responsiva sin firmar, el cliente la firma al recibir. */
    private static boolean firmaEnSitio(Traslado traslado, CartaResponsiva carta) {
        return traslado.getTipo() == TipoTraslado.ENTREGA
                && carta != null
                && carta.getEstado() != EstadoCarta.FIRMADA;
    }

    private Map<Long, CartaResponsiva> cartasDe(List<Traslado> tareas) {
        Set<Long> rentas = tareas.stream().map(t -> t.getRenta().getId()).collect(Collectors.toSet());
        if (rentas.isEmpty()) {
            return Map.of();
        }
        return cartaRepositorio.findByRentaIdIn(rentas).stream()
                .collect(Collectors.toMap(c -> c.getRenta().getId(), Function.identity(), (a, b) -> a));
    }

    private static ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }
}