package mx.montacargasobregon.backend.movil.traslados;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import mx.montacargasobregon.backend.cartas.servicio.CartaResponsivaService.ArchivoCarta;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;

/** Las tareas del operador de traslado en la app móvil. */
@RestController
@RequestMapping("/api/movil/traslados")
public class TareasTrasladoController {

    private final TareasTrasladoService servicio;

    public TareasTrasladoController(TareasTrasladoService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/movil/traslados?tipo=ENTREGA: mis entregas o mis recolecciones. */
    @GetMapping
    public List<TareaResponse> misTareas(@RequestParam TipoTraslado tipo, PersonalActual personal) {
        personal.exigir(TipoPersonal.OP_TRASLADO);
        return servicio.misTareas(personal, tipo);
    }

    /** GET /api/movil/traslados/{id}: el detalle de una tarea mía. */
    @GetMapping("/{id}")
    public DetalleTareaResponse detalle(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.OP_TRASLADO);
        return servicio.detalle(id, personal);
    }

    /** POST /api/movil/traslados/{id}/iniciar: salí con los equipos. */
    @PostMapping("/{id}/iniciar")
    public DetalleTareaResponse iniciar(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.OP_TRASLADO);
        return servicio.iniciar(id, personal);
    }

    /** POST /api/movil/traslados/{id}/completar: ya entregué o ya recogí. */
    @PostMapping("/{id}/completar")
    public DetalleTareaResponse completar(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.OP_TRASLADO);
        return servicio.completar(id, personal);
    }

    /** POST /api/movil/traslados/{id}/carta (multipart: firmante, archivo): el cliente firmó al recibir. */
    @PostMapping(value = "/{id}/carta", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DetalleTareaResponse adjuntarCarta(@PathVariable Long id, @RequestParam(required = false) String firmante,
                                              @RequestParam(required = false) MultipartFile archivo,
                                              PersonalActual personal) {
        personal.exigir(TipoPersonal.OP_TRASLADO);
        return servicio.adjuntarCarta(id, firmante, archivo, personal);
    }

    /** GET /api/movil/traslados/{id}/evidencia: las fotos de la recolección por equipo. */
    @GetMapping("/{id}/evidencia")
    public EvidenciaResponse evidencia(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.OP_TRASLADO);
        return servicio.evidencia(id, personal);
    }

    /** POST /api/movil/traslados/{id}/evidencia (multipart: equipoId, vista, archivo): sube o cambia una vista. */
    @PostMapping(value = "/{id}/evidencia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EvidenciaResponse subirEvidencia(@PathVariable Long id, @RequestParam Long equipoId,
                                            @RequestParam String vista, @RequestParam MultipartFile archivo,
                                            PersonalActual personal) {
        personal.exigir(TipoPersonal.OP_TRASLADO);
        return servicio.subirEvidencia(id, equipoId, vista, archivo, personal);
    }

    /** GET /api/movil/traslados/{id}/carta: la carta firmada de la renta. */
    @GetMapping("/{id}/carta")
    public ResponseEntity<byte[]> cartaFirmada(@PathVariable Long id, PersonalActual personal) {
        personal.exigir(TipoPersonal.OP_TRASLADO);
        ArchivoCarta archivo = servicio.cartaFirmada(id, personal);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + archivo.nombre() + "\"")
                .body(archivo.contenido());
    }
}