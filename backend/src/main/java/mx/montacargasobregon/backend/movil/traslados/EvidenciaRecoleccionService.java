package mx.montacargasobregon.backend.movil.traslados;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import mx.montacargasobregon.backend.movil.fotos.FotoResponse;
import mx.montacargasobregon.backend.movil.fotos.FotoService;
import mx.montacargasobregon.backend.movil.fotos.OrigenFoto;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;

/**
 * OHU13: las cuatro fotos de cada equipo que el operador toma en el domicilio del cliente
 * antes de confirmar la recolección. Se guardan como fotos del traslado, con la etiqueta
 * "MC-021 · Frente"; quién y cuándo las tomó lo guarda la foto.
 */
@Service
public class EvidenciaRecoleccionService {

    public static final List<String> VISTAS = List.of("Frente", "Parte trasera", "Lateral izquierdo", "Lateral derecho");

    private final FotoService fotos;

    public EvidenciaRecoleccionService(FotoService fotos) {
        this.fotos = fotos;
    }

    @Transactional(readOnly = true)
    public EvidenciaResponse de(Traslado traslado) {
        Map<String, FotoResponse> tomadas = fotos.listar(OrigenFoto.TRASLADO, traslado.getId()).stream()
                .collect(Collectors.toMap(FotoResponse::etiqueta, Function.identity(), (a, b) -> b));

        List<EvidenciaResponse.EquipoEvidencia> equipos = equiposDe(traslado).stream()
                .map(er -> {
                    List<EvidenciaResponse.VistaEvidencia> vistas = VISTAS.stream()
                            .map(v -> new EvidenciaResponse.VistaEvidencia(v, tomadas.get(etiqueta(er, v))))
                            .toList();
                    int listas = (int) vistas.stream().filter(v -> v.foto() != null).count();
                    return new EvidenciaResponse.EquipoEvidencia(er.getId(), er.getEquipo().getNumeroUnidad(),
                            listas, vistas);
                })
                .toList();

        int total = equipos.stream().mapToInt(EvidenciaResponse.EquipoEvidencia::tomadas).sum();
        return new EvidenciaResponse(traslado.getFolio(), traslado.getRenta().getFolio(), total,
                equipos.size() * VISTAS.size(), traslado.getEstado() == EstadoTraslado.PROGRAMADO, equipos);
    }

    /** Cuántas fotos faltan para poder confirmar la recolección. */
    @Transactional(readOnly = true)
    public int faltantes(Traslado traslado) {
        List<String> tomadas = fotos.listar(OrigenFoto.TRASLADO, traslado.getId()).stream()
                .map(FotoResponse::etiqueta)
                .toList();
        return (int) equiposDe(traslado).stream()
                .flatMap(er -> VISTAS.stream().map(v -> etiqueta(er, v)))
                .filter(e -> !tomadas.contains(e))
                .count();
    }

    /** El mensaje de la app mientras falten fotos, o null si ya están todas. */
    public String motivo(Traslado traslado) {
        int faltan = faltantes(traslado);
        if (faltan == 0) {
            return null;
        }
        return "Primero registra las " + (equiposDe(traslado).size() * VISTAS.size())
                + " fotografías de evidencia (faltan " + faltan + "). Después podrás confirmar la recolección.";
    }

    /** Sube o reemplaza una vista. Quien llama ya revisó que la recolección se puede documentar. */
    @Transactional
    public EvidenciaResponse subir(Traslado traslado, EquipoRenta equipo, String vista, MultipartFile archivo,
                                   PersonalActual personal) {
        fotos.subir(OrigenFoto.TRASLADO, traslado.getId(), etiqueta(equipo, vista), archivo, personal);
        return de(traslado);
    }

    /** Los equipos de la renta que se recogen, en orden. */
    static List<EquipoRenta> equiposDe(Traslado traslado) {
        return traslado.getRenta().getEquipos().stream()
                .filter(er -> er.getFase() != FaseEquipoRenta.CANCELADO)
                .sorted(Comparator.comparing(er -> er.getEquipo().getNumeroUnidad()))
                .toList();
    }

    static String etiqueta(EquipoRenta equipo, String vista) {
        return equipo.getEquipo().getNumeroUnidad() + " · " + vista;
    }
}