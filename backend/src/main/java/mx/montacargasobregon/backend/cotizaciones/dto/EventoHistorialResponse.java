package mx.montacargasobregon.backend.cotizaciones.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import mx.montacargasobregon.backend.cotizaciones.modelo.AutorizacionDescuento;
import mx.montacargasobregon.backend.cotizaciones.modelo.CorreccionCotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.ResultadoAutorizacion;
import mx.montacargasobregon.backend.usuarios.Usuario;

/**
 * Un paso del historial del descuento de la cotización (HU-32).
 * tipo: RECHAZADA, REENVIADA (corregida y otra vez a autorización), CORREGIDA (corregida sin
 * descuento, a Borrador) o AUTORIZADA.
 */
public record EventoHistorialResponse(
        String tipo,
        LocalDateTime fecha,
        String usuario, // Quién rechazó, corrigió o autorizó
        String comentario) { // El de gerencia; null en las correcciones

    /** Cada rechazo con su corrección y, al final, la última respuesta de gerencia. La más antigua primero. */
    public static List<EventoHistorialResponse> historialDe(Cotizacion c) {
        List<EventoHistorialResponse> eventos = new ArrayList<>();
        for (CorreccionCotizacion correccion : c.getCorrecciones()) {
            eventos.add(new EventoHistorialResponse(ResultadoAutorizacion.RECHAZADA.name(),
                    correccion.getFechaRechazo(), nombre(correccion.getRechazadaPor()),
                    correccion.getComentarioRechazo()));
            eventos.add(new EventoHistorialResponse(correccion.isReenviada() ? "REENVIADA" : "CORREGIDA",
                    correccion.getFechaCorreccion(), nombre(correccion.getCorregidaPor()), null));
        }

        AutorizacionDescuento a = c.getAutorizacion();
        if (a != null && a.getResultado() != ResultadoAutorizacion.PENDIENTE) {
            eventos.add(new EventoHistorialResponse(a.getResultado().name(), a.getFechaRespuesta(),
                    nombre(a.getRespondidaPor()), a.getComentario()));
        }
        return eventos;
    }

    private static String nombre(Usuario u) {
        return u.getNombres() + " " + u.getApellidoPaterno();
    }
}
