package mx.montacargasobregon.backend.cotizaciones.servicio;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.ResultadoAutorizacion;
import mx.montacargasobregon.backend.cotizaciones.repositorio.CotizacionRepository;
import mx.montacargasobregon.backend.usuarios.Usuario;
import mx.montacargasobregon.backend.usuarios.repositorio.UsuarioRepository;

/** Lo que gerencia hace con los descuentos que piden autorización: verlos, autorizarlos o rechazarlos. */
@Service
public class AutorizacionDescuentoService {

    private final CotizacionRepository cotizaciones;
    private final CotizacionService cotizacionService;
    private final UsuarioRepository usuarios;

    public AutorizacionDescuentoService(CotizacionRepository cotizaciones, CotizacionService cotizacionService,
            UsuarioRepository usuarios) {
        this.cotizaciones = cotizaciones;
        this.cotizacionService = cotizacionService;
        this.usuarios = usuarios;
    }

    /** Las solicitudes, las pendientes primero y dentro de cada grupo la más reciente primero. */
    @Transactional(readOnly = true)
    public List<Cotizacion> listar(boolean soloPendientes) {
        return cotizaciones.findByAutorizacionIsNotNull().stream()
                .filter(c -> !soloPendientes || c.getEstado() == EstadoCotizacion.EN_AUTORIZACION)
                .sorted(Comparator
                        .comparing((Cotizacion c) -> c.getAutorizacion().getResultado() != ResultadoAutorizacion.PENDIENTE)
                        .thenComparing(c -> c.getAutorizacion().getFechaSolicitud(), Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarPendientes() {
        return cotizaciones.countByEstado(EstadoCotizacion.EN_AUTORIZACION);
    }

    /** Gerencia autoriza el descuento: la cotización vuelve a Borrador, lista para enviarse. */
    @Transactional
    public Cotizacion autorizar(Long cotizacionId, String correoGerente, String comentario) {
        Cotizacion c = cotizacionService.obtener(cotizacionId);
        exigirQuePuedaCambiarA(c, EstadoCotizacion.BORRADOR);

        String nota = comentario == null || comentario.isBlank() ? null : comentario.trim();
        c.autorizarDescuento(gerente(correoGerente), nota);
        return c;
    }

    /** Gerencia rechaza el descuento con un comentario obligatorio: la cotización queda Rechazada. */
    @Transactional
    public Cotizacion rechazar(Long cotizacionId, String correoGerente, String comentario) {
        if (comentario == null || comentario.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe el motivo del rechazo");
        }
        Cotizacion c = cotizacionService.obtener(cotizacionId);
        exigirQuePuedaCambiarA(c, EstadoCotizacion.RECHAZADA);

        c.rechazarDescuento(gerente(correoGerente), comentario.trim());
        return c;
    }

    // Si la cotización ya no espera respuesta de gerencia, responde 409
    private void exigirQuePuedaCambiarA(Cotizacion c, EstadoCotizacion destino) {
        if (!c.getEstado().puedeCambiarA(destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cotización ya no está esperando autorización");
        }
    }

    private Usuario gerente(String correo) {
        return usuarios.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar"));
    }
}