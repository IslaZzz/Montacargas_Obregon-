package mx.montacargasobregon.backend.operacion.servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
     * La entrega quedó programada: crea una revisión de salida pendiente por equipo, para el día de la entrega.
     * El traslado de entrega nace después, cuando todos los equipos pasan su revisión.
     */
    public void programarRevisionesDeSalida(Renta renta) {
        for (EquipoRenta equipoRenta : renta.getEquipos()) {
            revisionRepositorio.save(new Revision(equipoRenta, TipoRevision.SALIDA, renta.getEntregaDesde()));
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
     * El operador salió con los equipos desde la app móvil: van en traslado hasta que confirme que llegó.
     */
    public void trasladoIniciado(Traslado traslado) {
        FaseEquipoRenta desde = traslado.getTipo() == TipoTraslado.ENTREGA
                ? FaseEquipoRenta.LISTO_ENTREGA
                : FaseEquipoRenta.EN_OPERACION;
        enFases(traslado.getRenta(), desde).forEach(er -> er.cambiarFase(FaseEquipoRenta.EN_TRASLADO));
    }

    /**
     * El operador confirmó que hizo el traslado.
     * Entrega: los equipos quedan con el cliente y la renta pasa a EN_CURSO.
     * Recolección: los equipos regresan y nace una revisión de entrada pendiente por cada uno.
     * Los equipos pueden venir EN_TRASLADO (se inició desde la app) o en su fase anterior
     * (se marcó directo desde la oficina).
     */
    public void trasladoRealizado(Traslado traslado) {
        Renta renta = traslado.getRenta();

        if (traslado.getTipo() == TipoTraslado.ENTREGA) {
            enFases(renta, FaseEquipoRenta.EN_TRASLADO, FaseEquipoRenta.LISTO_ENTREGA)
                    .forEach(er -> er.cambiarFase(FaseEquipoRenta.EN_OPERACION));
            renta.cambiarEstado(EstadoRenta.EN_CURSO);
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (EquipoRenta equipoRenta : enFases(renta, FaseEquipoRenta.EN_TRASLADO, FaseEquipoRenta.EN_OPERACION)) {
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

    private static List<EquipoRenta> enFases(Renta renta, FaseEquipoRenta... fases) {
        Set<FaseEquipoRenta> buscadas = Set.of(fases);
        return renta.getEquipos().stream()
                .filter(er -> buscadas.contains(er.getFase()))
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