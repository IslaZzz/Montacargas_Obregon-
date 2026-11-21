package mx.montacargasobregon.backend.cotizaciones.controlador;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.cotizaciones.dto.AutorizacionDescuentoRequest;
import mx.montacargasobregon.backend.cotizaciones.dto.AutorizacionResumenResponse;
import mx.montacargasobregon.backend.cotizaciones.dto.RechazarDescuentoRequest;
import mx.montacargasobregon.backend.cotizaciones.servicio.AutorizacionDescuentoService;

/** Rutas de las autorizaciones de descuento. Solo gerencia: la regla está en SeguridadConfig. */
@RestController
@RequestMapping("/api/autorizaciones")
public class AutorizacionDescuentoController {

    private final AutorizacionDescuentoService servicio;

    public AutorizacionDescuentoController(AutorizacionDescuentoService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/autorizaciones: todas las solicitudes; con ?soloPendientes=true, las que esperan respuesta. */
    @GetMapping
    public List<AutorizacionResumenResponse> listar(@RequestParam(defaultValue = "false") boolean soloPendientes) {
        return servicio.listar(soloPendientes).stream()
                .map(AutorizacionResumenResponse::toAutorizacionResumenResponse)
                .toList();
    }

    /** GET /api/autorizaciones/pendientes/total: el contador del menú. */
    @GetMapping("/pendientes/total")
    public long pendientes() {
        return servicio.contarPendientes();
    }

    /** PATCH /api/autorizaciones/5/autorizar: 5 es el id de la cotización; si ya no está pendiente, responde 409. */
    @PatchMapping("/{cotizacionId}/autorizar")
    public AutorizacionResumenResponse autorizar(@PathVariable Long cotizacionId,
            @Valid @RequestBody AutorizacionDescuentoRequest datos, Authentication usuario) {
        return AutorizacionResumenResponse.toAutorizacionResumenResponse(
                servicio.autorizar(cotizacionId, usuario.getName(), datos.comentario()));
    }

    /** PATCH /api/autorizaciones/5/rechazar: con comentario obligatorio; si ya no está pendiente, responde 409. */
    @PatchMapping("/{cotizacionId}/rechazar")
    public AutorizacionResumenResponse rechazar(@PathVariable Long cotizacionId,
            @Valid @RequestBody RechazarDescuentoRequest datos, Authentication usuario) {
        return AutorizacionResumenResponse.toAutorizacionResumenResponse(
                servicio.rechazar(cotizacionId, usuario.getName(), datos.comentario()));
    }
}