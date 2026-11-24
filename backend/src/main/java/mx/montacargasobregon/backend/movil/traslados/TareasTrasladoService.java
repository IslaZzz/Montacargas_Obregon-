package mx.montacargasobregon.backend.movil.traslados;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.cartas.modelo.CartaResponsiva;
import mx.montacargasobregon.backend.cartas.modelo.EstadoCarta;
import mx.montacargasobregon.backend.cartas.modelo.TipoFirma;
import mx.montacargasobregon.backend.cartas.repositorio.CartaResponsivaRepository;
import mx.montacargasobregon.backend.cartas.servicio.CartaResponsivaService;
import mx.montacargasobregon.backend.cartas.servicio.CartaResponsivaService.ArchivoCarta;
import mx.montacargasobregon.backend.avisos.servicio.AvisoOperacionService;
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
 * iniciar el traslado y marcarlo como realizado. Una entrega solo se marca realizada
 * con la carta responsiva firmada; si el cliente la firma al recibir, el operador la adjunta ahí.
 */
@Service
public class TareasTrasladoService {

    static final String SIN_CARTA_FIRMADA = "Obligatoria: sin la carta firmada no se puede registrar la entrega.";
    static final String ROL_OPERADOR = "Operador de traslado";

    private final TrasladoRepository trasladoRepositorio;
    private final CartaResponsivaRepository cartaRepositorio;
    private final CartaResponsivaService cartaServicio;
    private final AvanceRentaService avanceRenta;
    private final AvisoOperacionService avisos;

    public TareasTrasladoService(TrasladoRepository trasladoRepositorio,
                                 CartaResponsivaRepository cartaRepositorio,
                                 CartaResponsivaService cartaServicio,
                                 AvanceRentaService avanceRenta,
                                 AvisoOperacionService avisos) {
        this.trasladoRepositorio = trasladoRepositorio;
        this.cartaRepositorio = cartaRepositorio;
        this.cartaServicio = cartaServicio;
        this.avanceRenta = avanceRenta;
        this.avisos = avisos;
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
        if (traslado.getTipo() == TipoTraslado.ENTREGA) {
            avisos.trasladoIniciado(traslado);
        } else {
            avisos.recoleccionConfirmada(traslado);
        }
        return detalleDe(traslado);
    }

    /**
     * El operador llegó: los equipos quedan con el cliente, o regresan a revisión de entrada.
     * Una entrega pide la carta responsiva firmada.
     */
    @Transactional
    public DetalleTareaResponse completar(Long id, PersonalActual personal) {
        Traslado traslado = buscarMia(id, personal);

        if (traslado.getEstado() != EstadoTraslado.EN_CURSO) {
            throw conflicto("Primero inicia el traslado " + traslado.getFolio() + ".");
        }
        if (traslado.getTipo() == TipoTraslado.ENTREGA && !estaFirmada(cartaDe(traslado))) {
            throw conflicto(SIN_CARTA_FIRMADA);
        }

        traslado.completar();
        avanceRenta.trasladoRealizado(traslado);
        if (traslado.getTipo() == TipoTraslado.ENTREGA) {
            avisos.entregaRegistrada(traslado);
        } else {
            avisos.equiposRecibidos(traslado);
        }
        return detalleDe(traslado);
    }

    /**
     * El cliente firmó la carta impresa al recibir: el operador sube la foto y queda Firmada.
     * Las validaciones de la firma son las mismas que en la oficina.
     */
    @Transactional
    public DetalleTareaResponse adjuntarCarta(Long id, String firmante, MultipartFile archivo,
                                              PersonalActual personal) {
        Traslado traslado = buscarMia(id, personal);

        if (traslado.getTipo() != TipoTraslado.ENTREGA) {
            throw conflicto("La carta responsiva se firma en la entrega.");
        }
        if (traslado.getEstado() != EstadoTraslado.PROGRAMADO && traslado.getEstado() != EstadoTraslado.EN_CURSO) {
            throw conflicto("El traslado " + traslado.getFolio() + " ya terminó.");
        }

        cartaServicio.registrarFirma(traslado.getRenta().getId(), TipoFirma.FISICA,
                LocalDateTime.now(CartaResponsiva.ZONA_HORARIA), firmante, leer(archivo),
                personal.nombre(), ROL_OPERADOR);
        return detalleDe(traslado);
    }

    /** La carta firmada de la renta de la tarea, para que el operador la vea. */
    @Transactional(readOnly = true)
    public ArchivoCarta cartaFirmada(Long id, PersonalActual personal) {
        Traslado traslado = buscarMia(id, personal);
        return cartaServicio.archivoFirmado(traslado.getRenta().getId());
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
        boolean entrega = traslado.getTipo() == TipoTraslado.ENTREGA;
        CartaResponsiva carta = cartaDe(traslado);

        return DetalleTareaResponse.de(traslado, firmaEnSitio(traslado, carta), faseRequerida(traslado.getTipo()),
                entrega ? estadoCarta(carta) : null,
                programado && bloqueo == null,
                traslado.getEstado() == EstadoTraslado.EN_CURSO && (!entrega || estaFirmada(carta)),
                programado ? bloqueo : null);
    }

    private CartaResponsiva cartaDe(Traslado traslado) {
        return cartaRepositorio.findByRentaId(traslado.getRenta().getId()).orElse(null);
    }

    private static boolean estaFirmada(CartaResponsiva carta) {
        return carta != null && carta.getEstado() == EstadoCarta.FIRMADA;
    }

    /** Cómo ve el operador la carta: firmada, pendiente de firma, o sin generar todavía en la oficina. */
    static CartaTarea estadoCarta(CartaResponsiva carta) {
        if (estaFirmada(carta)) {
            return CartaTarea.FIRMADA;
        }
        if (carta == null || !carta.tieneArchivo()) {
            return CartaTarea.SIN_GENERAR;
        }
        return CartaTarea.PENDIENTE;
    }

    private static byte[] leer(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null; // registrarFirma responde con el mensaje de que falta la carta
        }
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la foto. Intenta de nuevo.");
        }
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