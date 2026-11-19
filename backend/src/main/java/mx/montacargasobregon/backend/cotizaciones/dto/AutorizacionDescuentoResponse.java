package mx.montacargasobregon.backend.cotizaciones.dto;

import java.time.LocalDateTime;

import mx.montacargasobregon.backend.cotizaciones.modelo.AutorizacionDescuento;
import mx.montacargasobregon.backend.usuarios.Usuario;

/** La solicitud de autorización de un descuento y la respuesta de gerencia. */
public record AutorizacionDescuentoResponse(
        String resultado, // PENDIENTE, AUTORIZADA o RECHAZADA
        String solicitadaPor,
        LocalDateTime fechaSolicitud,
        String respondidaPor, // null mientras esté pendiente
        LocalDateTime fechaRespuesta,
        String comentario) { // Solo al rechazar

    /** null si la cotización no tiene solicitud de autorización. */
    public static AutorizacionDescuentoResponse toAutorizacionDescuentoResponse(AutorizacionDescuento a) {
        if (a == null) {
            return null;
        }
        return new AutorizacionDescuentoResponse(
                a.getResultado().name(),
                nombre(a.getSolicitadaPor()),
                a.getFechaSolicitud(),
                a.getRespondidaPor() == null ? null : nombre(a.getRespondidaPor()),
                a.getFechaRespuesta(),
                a.getComentario());
    }

    private static String nombre(Usuario u) {
        return u.getNombres() + " " + u.getApellidoPaterno();
    }
}