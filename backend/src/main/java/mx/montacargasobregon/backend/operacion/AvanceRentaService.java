package mx.montacargasobregon.backend.operacion.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

/**
 * Hace avanzar la renta cuando sus equipos terminan un paso.
 * Lo usan revisiones, mantenimientos, traslados y la captura de rentas para no repetir esta lógica.
 * Se llama dentro de la transacción de quien lo usa.
 */
@Service
public class AvanceRentaService {

    private final TrasladoRepository trasladoRepositorio;
    private final RevisionRepository revisionRepositorio;

    public AvanceRentaService(TrasladoRepository trasladoRepositorio, RevisionRepository revisionRepositorio) {
        this.trasladoRepositorio = trasladoRepositorio;
        this.revisionRepositorio = revisionRepositorio;
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
                && !tieneTraslado(renta, TipoTraslado.ENTREGA)) {
            trasladoRepositorio.save(new Traslado(siguienteFolio(), renta, TipoTraslado.ENTREGA,
                    renta.getEntregaDesde(), renta.getEntregaHasta()));
        }
    }

    /**
     * Se acordó con el cliente cuándo recoger: guarda el horario y crea el traslado de recolección por asignar.
     * Quien llama revisa el horario y que la renta esté en curso.
     */
    public void programarRecoleccion(Renta renta, LocalDateTime desde, LocalDateTime hasta) {
        renta.confirmarRecoleccion(desde, hasta);

        if (!tieneTraslado(renta, TipoTraslado.RECOLECCION)) {
            trasladoRepositorio.save(new Traslado(siguienteFolio(), renta, TipoTraslado.RECOLECCION, desde, hasta));
        }
    }

    /**
     * El operador confirmó que hizo el traslado.
     * Entrega: los equipos quedan con el cliente y la renta pasa a EN_CURSO.
     * Recolección: los equipos regresan y nace una revisión de entrada pendiente por cada uno.
     */
    public void trasladoRealizado(Traslado traslado) {
        Renta renta = traslado.getRenta();

        if (traslado.getTipo() == TipoTraslado.ENTREGA) {
            enFase(renta, FaseEquipoRenta.LISTO_ENTREGA)
                    .forEach(er -> er.cambiarFase(FaseEquipoRenta.EN_OPERACION));
            renta.cambiarEstado(EstadoRenta.EN_CURSO);
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (EquipoRenta equipoRenta : enFase(renta, FaseEquipoRenta.EN_OPERACION)) {
            equipoRenta.cambiarFase(FaseEquipoRenta.EN_REVISION_ENTRADA);
            revisionRepositorio.save(new Revision(equipoRenta, TipoRevision.ENTRADA, ahora));
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

    private static List<EquipoRenta> enFase(Renta renta, FaseEquipoRenta fase) {
        return renta.getEquipos().stream()
                .filter(er -> er.getFase() == fase)
                .toList();
    }

    // Uno cancelado no cuenta: se puede volver a crear
    private boolean tieneTraslado(Renta renta, TipoTraslado tipo) {
        return trasladoRepositorio.existsByRentaAndTipoAndEstadoNot(renta, tipo, EstadoTraslado.CANCELADO);
    }

    // TR-0086 -> TR-0087
    private String siguienteFolio() {
        Integer ultimo = trasladoRepositorio.ultimoNumeroFolio();
        return String.format("TR-%04d", ultimo == null ? 1 : ultimo + 1);
    }
}