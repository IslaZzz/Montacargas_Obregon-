package mx.montacargasobregon.backend.cartas.controlador;

import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.cartas.dto.CartaResponse;
import mx.montacargasobregon.backend.cartas.dto.EnviarCartaRequest;
import mx.montacargasobregon.backend.cartas.servicio.CartaResponsivaService;
import mx.montacargasobregon.backend.cartas.servicio.CartaResponsivaService.ArchivoCarta;
import mx.montacargasobregon.backend.cartas.servicio.DatosFaltantesException;

/** Carta responsiva de una renta (OHU-04). */
@RestController
@RequestMapping("/api/rentas/{rentaId}/carta")
public class CartaResponsivaController {

    private final CartaResponsivaService servicio;

    public CartaResponsivaController(CartaResponsivaService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/rentas/5/carta: en qué va la carta de la renta 5. */
    @GetMapping
    public CartaResponse consultar(@PathVariable Long rentaId) {
        return servicio.consultar(rentaId);
    }

    /**
     * POST /api/rentas/5/carta: genera la carta, o la regenera si ya existía.
     * Si faltan datos responde 422 con { mensaje, errores: { dato: "Cliente" | "Renta" | "Equipo" } }.
     */
    @PostMapping
    public CartaResponse generar(@PathVariable Long rentaId) {
        return servicio.generar(rentaId);
    }

    /**
     * POST /api/rentas/5/carta/envio { mensaje }: envía la carta al correo del cliente y pasa a Enviada.
     * Si el correo no está configurado responde 503; si falla el servidor de correo, 502.
     */
    @PostMapping("/envio")
    public CartaResponse enviar(@PathVariable Long rentaId,
                                @Valid @RequestBody(required = false) EnviarCartaRequest datos) {
        return servicio.enviar(rentaId, datos == null ? null : datos.mensaje());
    }

    /** GET /api/rentas/5/carta/pdf: el PDF vigente para verlo o imprimirlo; con ?descargar=true se descarga. */
    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable Long rentaId,
                                      @RequestParam(defaultValue = "false") boolean descargar) {
        ArchivoCarta archivo = servicio.archivo(rentaId);
        ContentDisposition disposicion = (descargar ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(archivo.nombre())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                // Al regenerarla cambia el archivo: que el navegador no muestre la versión anterior
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .body(archivo.contenido());
    }

    /** Mismo formato que ManejadorErrores, con los datos que faltan en "errores". */
    @ExceptionHandler(DatosFaltantesException.class)
    public ResponseEntity<Map<String, Object>> datosFaltantes(DatosFaltantesException ex) {
        return ResponseEntity.status(422).body(Map.of(
                "mensaje", "Faltan datos obligatorios.",
                "errores", ex.getFaltantes()));
    }
}
