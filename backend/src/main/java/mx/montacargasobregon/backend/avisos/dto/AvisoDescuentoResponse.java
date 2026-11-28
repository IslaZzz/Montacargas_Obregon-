package mx.montacargasobregon.backend.avisos.dto;

import java.time.LocalDateTime;

import mx.montacargasobregon.backend.avisos.modelo.AvisoDescuento;
import mx.montacargasobregon.backend.cotizaciones.modelo.AutorizacionDescuento;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.ResultadoAutorizacion;
import mx.montacargasobregon.backend.usuarios.Usuario;

/** Un aviso de respuesta de gerencia a un descuento, como lo ve el auxiliar. */
public record AvisoDescuentoResponse(
        Long id,
        Long cotizacionId,
        String folio,
        String cliente,
        ResultadoAutorizacion resultado,
        String comentario,      // Vacío si gerencia no escribió ninguno
        String respondidaPor,
        LocalDateTime fecha,
        boolean leido) {

    public static AvisoDescuentoResponse de(AvisoDescuento aviso) {
        Cotizacion c = aviso.getCotizacion();
        Usuario respondidaPor = aviso.getRespondidaPor();
        String comentario = aviso.getComentario();

        // Avisos de antes de HU-32: no guardaban la respuesta, se lee de la cotización si sigue ahí.
        // Si ya se corrigió, esa solicitud cambió y el aviso sale sin comentario
        if (respondidaPor == null) {
            AutorizacionDescuento a = c.getAutorizacion();
            boolean esLaMisma = a != null && a.getResultado() == aviso.getResultado() && a.getRespondidaPor() != null;
            respondidaPor = esLaMisma ? a.getRespondidaPor() : null;
            comentario = esLaMisma ? a.getComentario() : null;
        }

        return new AvisoDescuentoResponse(
                aviso.getId(),
                c.getId(),
                c.getFolio(),
                ApartadoProximoResponse.nombreCliente(c.getCliente()),
                aviso.getResultado(),
                comentario,
                respondidaPor == null ? "Gerencia" : nombre(respondidaPor),
                aviso.getFecha(),
                aviso.isLeido());
    }

    private static String nombre(Usuario u) {
        return u.getNombres() + " " + u.getApellidoPaterno();
    }
}