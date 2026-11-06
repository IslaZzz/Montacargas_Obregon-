package mx.montacargasobregon.backend.operacion.servicio;

import org.springframework.stereotype.Service;

import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

/**
 * Hace avanzar la renta cuando sus equipos terminan un paso.
 * Lo usan revisiones y mantenimientos para no repetir esta lógica.
 * Se llama dentro de la transacción de quien lo usa.
 */
@Service
public class AvanceRentaService {

    private final TrasladoRepository trasladoRepositorio;

    public AvanceRentaService(TrasladoRepository trasladoRepositorio) {
        this.trasladoRepositorio = trasladoRepositorio;
    }

    /**
     * El equipo pasó su revisión de salida, o ya se reparó.
     * Cuando todos los equipos de la renta están listos, nace el traslado de entrega por asignar.
     */
    public void equipoListoParaEntrega(EquipoRenta equipoRenta) {
        equipoRenta.cambiarFase(FaseEquipoRenta.LISTO_ENTREGA);

        Renta renta = equipoRenta.getRenta();
        if (todosEn(renta, FaseEquipoRenta.LISTO_ENTREGA)
                && renta.getEntregaDesde() != null
                && !trasladoRepositorio.existsByRentaAndTipoAndEstadoNot(
                renta, TipoTraslado.ENTREGA, EstadoTraslado.CANCELADO)) {
            trasladoRepositorio.save(new Traslado(siguienteFolio(), renta, TipoTraslado.ENTREGA,
                    renta.getEntregaDesde(), renta.getEntregaHasta()));
        }
    }

    /**
     * El equipo regresó y terminó su revisión de entrada: ya no pertenece a la renta.
     * Cuando regresan todos, la renta se cierra.
     */
    public void equipoDevuelto(EquipoRenta equipoRenta) {
        equipoRenta.cambiarFase(FaseEquipoRenta.COMPLETADO);

        Renta renta = equipoRenta.getRenta();
        if (todosEn(renta, FaseEquipoRenta.COMPLETADO)) {
            renta.cambiarEstado(EstadoRenta.CERRADA);
        }
    }

    // Los equipos cancelados no cuentan
    private static boolean todosEn(Renta renta, FaseEquipoRenta fase) {
        return renta.getEquipos().stream()
                .filter(er -> er.getFase() != FaseEquipoRenta.CANCELADO)
                .allMatch(er -> er.getFase() == fase);
    }

    // TR-0086 -> TR-0087
    private String siguienteFolio() {
        Integer ultimo = trasladoRepositorio.ultimoNumeroFolio();
        return String.format("TR-%04d", ultimo == null ? 1 : ultimo + 1);
    }
}