package mx.montacargasobregon.backend.contratos.controlador;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.montacargasobregon.backend.contratos.dto.ContratoResponse;
import mx.montacargasobregon.backend.contratos.servicio.ContratoPdfService;
import mx.montacargasobregon.backend.contratos.servicio.ContratoService;
import mx.montacargasobregon.backend.cotizaciones.servicio.CotizacionPdfService.ArchivoPdf;

/** Rutas HTTP de los contratos (HU-23). El contrato se genera al aceptar la cotización. */
@RestController
@RequestMapping("/api/contratos")
public class ContratoController {

    private final ContratoService servicio;
    private final ContratoPdfService pdfServicio;

    public ContratoController(ContratoService servicio, ContratoPdfService pdfServicio) {
        this.servicio = servicio;
        this.pdfServicio = pdfServicio;
    }

    /** GET /api/contratos/5: el detalle del contrato con id 5; si no existe, responde 404. */
    @GetMapping("/{id}")
    public ContratoResponse obtener(@PathVariable Long id) {
        return ContratoResponse.toContratoResponse(servicio.obtener(id));
    }

    /** GET /api/contratos/5/pdf: el PDF del contrato, para verlo en el navegador. Con ?descargar=true se descarga. */
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean descargar) {
        ArchivoPdf archivo = pdfServicio.generar(id);
        ContentDisposition disposicion = (descargar ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(archivo.nombre())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(archivo.contenido());
    }
}
