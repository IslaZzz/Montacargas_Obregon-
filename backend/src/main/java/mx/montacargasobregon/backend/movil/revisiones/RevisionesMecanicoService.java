package mx.montacargasobregon.backend.movil.revisiones;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.equipos.modelo.CapturaCampo;
import mx.montacargasobregon.backend.equipos.modelo.DefinicionCampo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.equipos.modelo.TipoCampo;
import mx.montacargasobregon.backend.equipos.repositorio.DefinicionCampoRepository;
import mx.montacargasobregon.backend.equipos.servicio.ModeloService;
import mx.montacargasobregon.backend.movil.fotos.FotoResponse;
import mx.montacargasobregon.backend.movil.fotos.FotoService;
import mx.montacargasobregon.backend.movil.fotos.OrigenFoto;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoInspeccion;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.ValorRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;
import mx.montacargasobregon.backend.revisiones.servicio.RevisionService;

/**
 * Las revisiones del mecánico en la app móvil (OHU06 y OHU07): ver las suyas, abrir una,
 * llenar los campos de revisión de la categoría del equipo y terminarla.
 * Lo que pasa después lo decide RevisionService.
 */
@Service
public class RevisionesMecanicoService {

    private static final Set<EstadoRevision> POR_HACER = EnumSet.of(EstadoRevision.ASIGNADO, EstadoRevision.EN_PROCESO);

    private final RevisionRepository repositorio;
    private final RevisionService revisionServicio;
    private final FotoService fotoServicio;
    private final DefinicionCampoRepository campoRepositorio;

    public RevisionesMecanicoService(RevisionRepository repositorio, RevisionService revisionServicio,
                                     FotoService fotoServicio, DefinicionCampoRepository campoRepositorio) {
        this.repositorio = repositorio;
        this.revisionServicio = revisionServicio;
        this.fotoServicio = fotoServicio;
        this.campoRepositorio = campoRepositorio;
    }

    /** Sus revisiones por hacer; tipo null trae salidas y regresos. */
    @Transactional(readOnly = true)
    public List<RevisionMecanicoResponse> misRevisiones(PersonalActual personal, TipoRevision tipo) {
        return repositorio.buscarPorMecanico(personal.id(), POR_HACER).stream()
                .filter(r -> tipo == null || r.getTipo() == tipo)
                .map(RevisionMecanicoResponse::de)
                .toList();
    }

    /** Una revisión suya, por hacer o ya terminada (para el historial). */
    @Transactional(readOnly = true)
    public DetalleRevisionMecanicoResponse detalle(Long id, PersonalActual personal) {
        Revision revision = buscarMia(id, personal);
        return DetalleRevisionMecanicoResponse.de(revision, camposDe(revision));
    }

    /** El mecánico abrió la revisión: queda en proceso. */
    @Transactional
    public DetalleRevisionMecanicoResponse iniciar(Long id, PersonalActual personal) {
        Revision revision = buscarMia(id, personal);
        exigirPorHacer(revision);
        revision.iniciar();
        return DetalleRevisionMecanicoResponse.de(revision, camposDe(revision));
    }

    /**
     * Guarda lo que llenó el mecánico y termina la revisión. Pide los campos obligatorios de la categoría,
     * el detalle de las inspecciones que no están bien y las fotos obligatorias.
     * Una inspección en falla crea el mantenimiento con esas fallas.
     */
    @Transactional
    public RevisionTerminadaResponse terminar(Long id, PersonalActual personal, CapturaRevisionRequest datos) {
        Revision revision = buscarMia(id, personal);
        exigirPorHacer(revision);
        List<DefinicionCampo> campos = camposDe(revision);

        List<ValorRevision> valores = validar(campos, datos.valores());
        fotoServicio.verificarCompletas(OrigenFoto.REVISION, id, campos.stream()
                .filter(c -> c.getTipo() == TipoCampo.FOTO && c.isRequerido())
                .map(DefinicionCampo::getNombre)
                .toList());
        valores.addAll(fotosTomadas(id, campos));

        revision.registrarValores(valores, limpiar(datos.notas()));
        revisionServicio.aplicarResultado(revision, descripcionDeFallas(revision));

        return new RevisionTerminadaResponse(revision.getId(), revision.getEquipoRenta().getEquipo().getNumeroUnidad(),
                revision.getTipo(), revision.getResultado());
    }

    // ---- apoyo ----

    /** Los campos de revisión de la categoría del equipo, en el orden del formulario. */
    private List<DefinicionCampo> camposDe(Revision revision) {
        Modelo modelo = revision.getEquipoRenta().getEquipo().getModelo();
        if (modelo == null) {
            return List.of();
        }
        return campoRepositorio.findByCategoriaIdOrderByOrden(modelo.getCategoria().getId()).stream()
                .filter(c -> c.seCapturaEn(CapturaCampo.REVISION))
                .toList();
    }

    /** Revisa cada campo que no es foto y lo convierte en su valor. Las reglas de cada tipo son las del modelo. */
    private static List<ValorRevision> validar(List<DefinicionCampo> campos,
                                               List<CapturaRevisionRequest.ValorRequest> recibidos) {
        Map<Long, CapturaRevisionRequest.ValorRequest> porCampo = recibidos.stream()
                .collect(Collectors.toMap(CapturaRevisionRequest.ValorRequest::campoId, Function.identity(), (a, b) -> b));

        List<ValorRevision> valores = new ArrayList<>();
        for (DefinicionCampo campo : campos) {
            CapturaRevisionRequest.ValorRequest r = porCampo.get(campo.getId());
            if (campo.getTipo() == TipoCampo.FOTO) {
                continue;
            }
            if (campo.getTipo() == TipoCampo.INSPECCION) {
                EstadoInspeccion estado = r == null ? null : r.estado();
                if (estado == null) {
                    if (campo.isRequerido()) {
                        throw invalido("Marca cómo está " + campo.getNombre() + ".");
                    }
                    continue;
                }
                String detalle = r.detalle() == null ? null : limpiar(r.detalle());
                if (estado != EstadoInspeccion.BIEN && detalle == null) {
                    throw invalido("Describe lo que encontraste en " + campo.getNombre() + ".");
                }
                valores.add(new ValorRevision(campo, null, estado, estado == EstadoInspeccion.BIEN ? null : detalle));
                continue;
            }
            String texto = r == null ? null : limpiar(r.valor());
            if (texto == null) {
                if (campo.isRequerido()) {
                    throw invalido("Llena " + campo.getNombre() + ".");
                }
                continue;
            }
            try {
                valores.add(new ValorRevision(campo, ModeloService.normalizar(campo, texto), null, null));
            } catch (IllegalArgumentException ex) {
                throw invalido(campo.getNombre() + ": " + ex.getMessage());
            }
        }
        return valores;
    }

    /** Un valor por cada campo FOTO que ya tiene su foto; guarda el id de la foto. */
    private List<ValorRevision> fotosTomadas(Long revisionId, List<DefinicionCampo> campos) {
        Map<String, Long> porEtiqueta = new HashMap<>();
        for (FotoResponse foto : fotoServicio.listar(OrigenFoto.REVISION, revisionId)) {
            porEtiqueta.put(foto.etiqueta(), foto.id()); // Si se tomó dos veces, queda la última
        }
        return campos.stream()
                .filter(c -> c.getTipo() == TipoCampo.FOTO && porEtiqueta.containsKey(c.getNombre()))
                .map(c -> new ValorRevision(c, String.valueOf(porEtiqueta.get(c.getNombre())), null, null))
                .toList();
    }

    private Revision buscarMia(Long id, PersonalActual personal) {
        Revision revision = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La revisión no existe."));
        if (revision.getMecanico() == null || !revision.getMecanico().getId().equals(personal.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esa revisión no está asignada a ti.");
        }
        return revision;
    }

    private static void exigirPorHacer(Revision revision) {
        if (!revision.puedeRegistrarResultado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La revisión de " + revision.getEquipoRenta().getEquipo().getNumeroUnidad()
                            + " ya está terminada o cancelada.");
        }
    }

    /** "Frenos: pedal sin presión; Llantas: ..." en el largo que admite el mantenimiento. */
    private static String descripcionDeFallas(Revision revision) {
        String texto = revision.inspeccionesEn(EstadoInspeccion.FALLA).stream()
                .map(ValorRevision::describir)
                .collect(Collectors.joining("; "));
        return texto.length() <= 255 ? texto : texto.substring(0, 252) + "...";
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}