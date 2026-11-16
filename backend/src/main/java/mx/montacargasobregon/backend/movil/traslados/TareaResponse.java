package mx.montacargasobregon.backend.movil.traslados;

import java.time.LocalDateTime;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.comun.dto.DomicilioResponse;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;

/** Una tarea de la lista "Mis tareas". */
public record TareaResponse(Long id, String folio, TipoTraslado tipo, EstadoTraslado estado,
                            LocalDateTime inicio, LocalDateTime fin, String rentaFolio, String cliente,
                            DomicilioResponse domicilio, int equipos, boolean firmaEnSitio) {

    public static TareaResponse de(Traslado traslado, boolean firmaEnSitio) {
        Renta renta = traslado.getRenta();
        return new TareaResponse(
                traslado.getId(),
                traslado.getFolio(),
                traslado.getTipo(),
                traslado.getEstado(),
                traslado.getFechaHoraInicio(),
                traslado.getFechaHoraFin(),
                renta.getFolio(),
                nombreCliente(renta.getCliente()),
                domicilioDeLaObra(renta),
                (int) renta.getEquipos().stream()
                        .filter(e -> e.getFase() != FaseEquipoRenta.CANCELADO).count(),
                firmaEnSitio);
    }

    /** La empresa cuando el cliente la tiene; si no, su nombre. */
    static String nombreCliente(Cliente cliente) {
        if (cliente.getEmpresa() != null && !cliente.getEmpresa().isBlank()) {
            return cliente.getEmpresa();
        }
        return (cliente.getNombre() + " " + cliente.getApellidoPaterno()).trim();
    }

    /** La obra viene por partes desde la cotización; si no, el texto que guarda la renta. */
    static DomicilioResponse domicilioDeLaObra(Renta renta) {
        if (renta.getCotizacion() != null && renta.getCotizacion().getDireccionObra() != null) {
            return DomicilioResponse.deDireccion(renta.getCotizacion().getDireccionObra());
        }
        return DomicilioResponse.deTexto(renta.getDireccionObra());
    }
}