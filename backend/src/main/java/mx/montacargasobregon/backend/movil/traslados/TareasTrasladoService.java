package mx.montacargasobregon.backend.movil.traslados;

import java.io.IOException;
import java.time.LocalDate;
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

import mx.montacargasobregon.backend.avisos.servicio.AvisoOperacionService;
import mx.montacargasobregon.backend.cartas.modelo.CartaResponsiva;
import mx.montacargasobregon.backend.cartas.modelo.EstadoCarta;
import mx.montacargasobregon.backend.cartas.modelo.TipoFirma;
import mx.montacargasobregon.backend.cartas.repositorio.CartaResponsivaRepository;
import mx.montacargasobregon.backend.cartas.servicio.CartaResponsivaService;
import mx.montacargasobregon.backend.cartas.servicio.CartaResponsivaService.ArchivoCarta;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
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
 * Entrega: iniciar es salir con los equipos; realizarla pide la carta responsiva firmada,
 * y si el cliente la firma al recibir, el operador la adjunta ahí (OHU10 y OHU11).
 * Recolección: iniciar es confirmar la recolección, con las fotos de evidencia completas;
 * realizarla es recibir los equipos en la oficina (OHU13, OHU14 y OHU15).
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
    private final EvidenciaRecoleccionService evidencia;

    public TareasTrasladoService(TrasladoRepository trasladoRepositorio,
                                 CartaResponsivaRepository cartaRepositorio,
                                 CartaResponsivaService cartaServicio,
                                 AvanceRentaService avanceRenta,
                                 AvisoOperacionService avisos,
                                 EvidenciaRecoleccionService evidencia) {
        this.trasladoRepositorio = trasladoRepositorio;
        this.cartaRepositorio = cartaRepositorio;
        this.cartaServicio = cartaServicio;
        this.avanceRenta = avanceRenta;
        this.avisos = avisos;
        this.evidencia = evidencia;
    }

    /**
     * Las tareas pendientes y en camino del operador, del tipo que pida (entregas o recolecciones).
     * Las realizadas salen de la lista, salvo las recolecciones recibidas hoy en la oficina,
     * que se quedan con la etiqueta "Recibida" (OHU09).
     */
    @Transactional(readOnly = true)
    public List<TareaResponse> misTareas(PersonalActual personal, TipoTraslado tipo) {
        LocalDate hoy = LocalDate.now();
        List<Traslado> tareas = trasladoRepositorio.buscarPorOperador(personal.id()).stream()
                .filter(t -> t.getTipo() == tipo)
                .filter(t -> t.getEstado() == EstadoTraslado.PROGRAMADO
                        || t.getEstado() == EstadoTraslado.EN_CURSO
                        || recibidaHoy(t, hoy))
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
        if (traslado.getTipo() == TipoTraslado.RECOLECCION) {
            String faltanFotos = evidencia.motivo(traslado);
            if (faltanFotos != null) {
                throw conflicto(faltanFotos);
            }
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

    /** OHU13: las fotos de evidencia de la recolección, por equipo. */
    @Transactional(readOnly = true)
    public EvidenciaResponse evidencia(Long id, PersonalActual personal) {
        return evidencia.de(recoleccionMia(id, personal));
    }

    /**
     * OHU13: sube o cambia una de las cuatro fotos de un equipo. Solo antes de confirmar
     * la recolección y con los equipos todavía en sitio.
     */
    @Transactional
    public EvidenciaResponse subirEvidencia(Long id, Long equipoId, String vista, MultipartFile archivo,
                                            PersonalActual personal) {
        Traslado traslado = recoleccionMia(id, personal);

        if (traslado.getEstado() != EstadoTraslado.PROGRAMADO) {
            throw conflicto("Las fotografías ya no se pueden cambiar: la recolección ya se confirmó.");
        }
        if (traslado.getRenta().getEstado() != EstadoRenta.EN_CURSO) {
            throw conflicto("La renta " + traslado.getRenta().getFolio() + " todavía no está en curso.");
        }
        if (vista == null || !EvidenciaRecoleccionService.VISTAS.contains(vista)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige una de las cuatro vistas del equipo.");
        }
        EquipoRenta equipo = EvidenciaRecoleccionService.equiposDe(traslado).stream()
                .filter(er -> er.getId().equals(equipoId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ese equipo no es de esta recolección."));

        return evidencia.subir(traslado, equipo, vista, archivo, personal);
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

    private Traslado recoleccionMia(Long id, PersonalActual personal) {
        Traslado traslado = buscarMia(id, personal);
        if (traslado.getTipo() != TipoTraslado.RECOLECCION) {
            throw conflicto("La evidencia fotográfica se registra en las recolecciones.");
        }
        return traslado;
    }

    private DetalleTareaResponse detalleDe(Traslado traslado) {
        boolean programado = traslado.getEstado() == EstadoTraslado.PROGRAMADO;
        boolean entrega = traslado.getTipo() == TipoTraslado.ENTREGA;
        CartaResponsiva carta = entrega ? cartaDe(traslado) : null;

        String bloqueo = motivoBloqueo(traslado);
        EvidenciaResponse fotos = entrega ? null : evidencia.de(traslado);
        if (bloqueo == null && fotos != null && programado && fotos.tomadas() < fotos.requeridas()) {
            bloqueo = evidencia.motivo(traslado);
        }

        return DetalleTareaResponse.de(traslado, firmaEnSitio(traslado, carta), faseRequerida(traslado.getTipo()),
                entrega ? estadoCarta(carta) : null,
                fotos == null ? null : fotos.tomadas(),
                fotos == null ? null : fotos.requeridas(),
                programado && bloqueo == null,
                traslado.getEstado() == EstadoTraslado.EN_CURSO && (!entrega || estaFirmada(carta)),
                programado ? bloqueo : null);
    }

    private static boolean recibidaHoy(Traslado traslado, LocalDate hoy) {
        return traslado.getTipo() == TipoTraslado.RECOLECCION
                && traslado.getEstado() == EstadoTraslado.COMPLETADO
                && traslado.getFechaFinReal() != null
                && traslado.getFechaFinReal().toLocalDate().equals(hoy);
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