package mx.montacargasobregon.backend.cotizaciones.controlador;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.cotizaciones.dto.CambiarEstadoCotizacionRequest;
import mx.montacargasobregon.backend.cotizaciones.dto.CotizacionResponse;
import mx.montacargasobregon.backend.cotizaciones.dto.CotizacionResumenResponse;
import mx.montacargasobregon.backend.cotizaciones.servicio.CotizacionPdfService;
import mx.montacargasobregon.backend.cotizaciones.servicio.CotizacionPdfService.ArchivoPdf;
import mx.montacargasobregon.backend.cotizaciones.servicio.CotizacionService;

/** Rutas HTTP de las cotizaciones. */
@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionController {

    private final CotizacionService servicio;
    private final CotizacionPdfService pdfServicio;

    public CotizacionController(CotizacionService servicio, CotizacionPdfService pdfServicio) {
        this.servicio = servicio;
        this.pdfServicio = pdfServicio;
    }

    /** GET /api/cotizaciones: lista todas las cotizaciones, la más nueva primero. */
    @GetMapping
    public List<CotizacionResumenResponse> listar() {
        return servicio.listar().stream()
                .map(CotizacionResumenResponse::toCotizacionResumenResponse)
                .toList();
    }

    /** GET /api/cotizaciones/5: obtiene el detalle de la cotización con id 5; si no existe, responde 404. */
    @GetMapping("/{id}")
    public CotizacionResponse obtener(@PathVariable Long id) {
        return CotizacionResponse.toCotizacionResponse(servicio.obtener(id));
    }

    /**
     * GET /api/cotizaciones/5/pdf: el PDF de la cotización, para verlo en el navegador.
     * Con ?descargar=true se descarga. No cambia el estado: eso lo hace el PATCH de abajo.
     */
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

    /** PATCH /api/cotizaciones/5/estado: cambia solo el estado; si el cambio no se permite, responde 409. */
    @PatchMapping("/{id}/estado")
    public CotizacionResponse cambiarEstado(@PathVariable Long id,
                                            @Valid @RequestBody CambiarEstadoCotizacionRequest datos) {
        return CotizacionResponse.toCotizacionResponse(servicio.cambiarEstado(id, datos.estado()));
    }
}
