package mx.montacargasobregon.backend.cartas.controlador;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;

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
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.cartas.dto.CartaResponse;
import mx.montacargasobregon.backend.cartas.dto.EnviarCartaRequest;
import mx.montacargasobregon.backend.cartas.modelo.TipoFirma;
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
        // Sin caché (en archivo()): al regenerarla cambia el archivo y el navegador no debe mostrar la anterior
        return archivo(servicio.archivo(rentaId), descargar);
    }

    /**
     * POST /api/rentas/5/carta/firma (multipart): registra que el cliente firmó la carta (OHU-05).
     * Partes: tipo (ELECTRONICA | FISICA), fecha (2026-10-09T12:30), firmante y archivo (la carta firmada).
     * Si falta algo o el archivo no sirve responde 400 con el motivo y no registra nada.
     */
    @PostMapping(value = "/firma", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CartaResponse registrarFirma(@PathVariable Long rentaId,
                                        @RequestParam(required = false) TipoFirma tipo,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fecha,
                                        @RequestParam(required = false) String firmante,
                                        @RequestParam(required = false) MultipartFile archivo) throws IOException {
        byte[] contenido = archivo == null || archivo.isEmpty() ? null : archivo.getBytes();
        return servicio.registrarFirma(rentaId, tipo, fecha, firmante, contenido);
    }

    /** GET /api/rentas/5/carta/firmada: la carta firmada para verla; con ?descargar=true se descarga. */
    @GetMapping("/firmada")
    public ResponseEntity<byte[]> firmada(@PathVariable Long rentaId,
                                          @RequestParam(defaultValue = "false") boolean descargar) {
        return archivo(servicio.archivoFirmado(rentaId), descargar);
    }

    private static ResponseEntity<byte[]> archivo(ArchivoCarta archivo, boolean descargar) {
        ContentDisposition disposicion = (descargar ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(archivo.nombre())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(archivo.tipo()))
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
