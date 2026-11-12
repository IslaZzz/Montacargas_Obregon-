package mx.montacargasobregon.backend.rentas.captura;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/**
 * Confirmación temporal de los horarios de entrega y recolección de una renta.
 * La renta ya no se captura a mano: nace al aceptar la cotización (HU-37).
 */
@Service
public class CapturaRentaService {

    private final CapturaRentaRepository repositorio;
    private final AvanceRentaService avanceRenta;

    public CapturaRentaService(CapturaRentaRepository repositorio, AvanceRentaService avanceRenta) {
        this.repositorio = repositorio;
        this.avanceRenta = avanceRenta;
    }

    /** Confirma el horario de entrega de una renta que todavía no lo tiene. */
    @Transactional
    public RentaCapturadaResponse confirmarEntrega(Long id, HorarioRequest datos) {
        Renta renta = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Renta no encontrada"));

        if (renta.getEstado() != EstadoRenta.PROGRAMADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede confirmar la entrega de una renta programada");
        }
        if (renta.getEntregaDesde() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La renta " + renta.getFolio() + " ya tiene horario de entrega");
        }
        validarHorario(datos.desde(), datos.hasta(), renta.getFechaFin());

        programarEntrega(renta, datos.desde(), datos.hasta());
        return RentaCapturadaResponse.deRenta(renta);
    }

    /** Rentas en curso a las que todavía no se les acuerda la recolección. */
    @Transactional(readOnly = true)
    public List<RentaCapturadaResponse> rentasSinRecoleccion() {
        return repositorio.findByEstadoAndRecoleccionDesdeIsNullOrderByFechaFinAsc(EstadoRenta.EN_CURSO).stream()
                .map(RentaCapturadaResponse::deRenta)
                .toList();
    }

    /**
     * Confirma el horario de recolección de una renta en curso.
     * Al guardarlo nace el traslado de recolección por asignar.
     */
    @Transactional
    public RentaCapturadaResponse confirmarRecoleccion(Long id, HorarioRequest datos) {
        Renta renta = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Renta no encontrada"));

        if (renta.getEstado() != EstadoRenta.EN_CURSO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede programar la recolección de una renta en curso");
        }
        if (renta.getRecoleccionDesde() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La renta " + renta.getFolio() + " ya tiene horario de recolección");
        }
        if (!datos.desde().isBefore(datos.hasta())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El horario de recolección debe terminar después de empezar");
        }

        avanceRenta.programarRecoleccion(renta, datos.desde(), datos.hasta());
        return RentaCapturadaResponse.deRenta(renta);
    }

    /**
     * Guarda el horario y crea una revisión de salida pendiente por equipo.
     * El traslado de entrega nace después, cuando todos los equipos pasan su revisión.
     */
    private void programarEntrega(Renta renta, LocalDateTime desde, LocalDateTime hasta) {
        renta.confirmarEntrega(desde, hasta);
        avanceRenta.programarRevisionesDeSalida(renta);
    }

    private static void validarHorario(LocalDateTime desde, LocalDateTime hasta, LocalDate fechaFin) {
        if (desde == null || hasta == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indica cuándo empieza y cuándo termina el horario de entrega");
        }
        if (!desde.isBefore(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El horario de entrega debe terminar después de empezar");
        }
        if (desde.toLocalDate().isAfter(fechaFin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La entrega no puede ser después de que termine la renta");
        }
    }
}