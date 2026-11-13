package mx.montacargasobregon.backend.movil.fotos;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import mx.montacargasobregon.backend.movil.sesion.PersonalActual;

/** Fotos de revisiones, traslados y mantenimientos tomadas desde la app móvil. */
@RestController
@RequestMapping("/api/movil/fotos")
public class FotoController {

    private final FotoService servicio;

    public FotoController(FotoService servicio) {
        this.servicio = servicio;
    }

    /** POST /api/movil/fotos (multipart): origen, referenciaId, etiqueta y archivo. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FotoResponse subir(@RequestParam OrigenFoto origen, @RequestParam Long referenciaId,
                              @RequestParam String etiqueta, @RequestParam MultipartFile archivo,
                              PersonalActual personal) {
        return servicio.subir(origen, referenciaId, etiqueta, archivo, personal);
    }

    /** GET /api/movil/fotos?origen=REVISION&referenciaId=15 */
    @GetMapping
    public List<FotoResponse> listar(@RequestParam OrigenFoto origen, @RequestParam Long referenciaId) {
        return servicio.listar(origen, referenciaId);
    }

    /**
     * GET /api/movil/fotos/{id}: la imagen. No pide X-Personal-Id porque la carga un <img>,
     * que no puede mandar encabezados. Una foto nunca cambia (reemplazarla crea otra), así que se guarda en caché.
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> imagen(@PathVariable Long id) {
        Foto foto = servicio.obtener(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getTipoContenido()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                .body(foto.getContenido());
    }

    /** DELETE /api/movil/fotos/{id}: solo quien la subió. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@PathVariable Long id, PersonalActual personal) {
        servicio.borrar(id, personal);
    }
}