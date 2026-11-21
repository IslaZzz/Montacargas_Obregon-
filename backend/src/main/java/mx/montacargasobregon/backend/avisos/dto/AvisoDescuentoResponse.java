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
        AutorizacionDescuento a = c.getAutorizacion();
        return new AvisoDescuentoResponse(
                aviso.getId(),
                c.getId(),
                c.getFolio(),
                ApartadoProximoResponse.nombreCliente(c.getCliente()),
                aviso.getResultado(),
                a.getComentario(),
                nombre(a.getRespondidaPor()),
                aviso.getFecha(),
                aviso.isLeido());
    }

    private static String nombre(Usuario u) {
        return u.getNombres() + " " + u.getApellidoPaterno();
    }
}