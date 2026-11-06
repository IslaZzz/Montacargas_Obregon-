package mx.montacargasobregon.backend.rentas.detalle;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.rentas.repositorio.RentaRepository;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

/** Detalle de una renta y confirmación de su recolección. */
@Service
public class DetalleRentaService {

    private final RentaRepository rentaRepositorio;
    private final TrasladoRepository trasladoRepositorio;
    private final AvanceRentaService avanceRenta;

    public DetalleRentaService(RentaRepository rentaRepositorio, TrasladoRepository trasladoRepositorio,
                               AvanceRentaService avanceRenta) {
        this.rentaRepositorio = rentaRepositorio;
        this.trasladoRepositorio = trasladoRepositorio;
        this.avanceRenta = avanceRenta;
    }

    @Transactional(readOnly = true)
    public DetalleRentaResponse obtener(Long id) {
        Renta renta = buscar(id);
        return DetalleRentaResponse.de(renta, recoleccionDe(renta));
    }

    /**
     * Confirma o cambia el horario de recolección de una renta en curso.
     * La primera vez nace el traslado de recolección por asignar; después se le cambia el horario,
     * mientras no haya iniciado y sin empalmarse con otro traslado de su operador.
     */
    @Transactional
    public DetalleRentaResponse confirmarRecoleccion(Long id, HorarioRecoleccionRequest datos) {
        Renta renta = buscar(id);

        if (renta.getEstado() != EstadoRenta.EN_CURSO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La recolección se coordina cuando la renta está en curso");
        }
        if (!datos.desde().isBefore(datos.hasta())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La hora de fin debe ser después de la de inicio");
        }
        if (datos.desde().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Elige una fecha y hora que no hayan pasado");
        }

        Traslado traslado = recoleccionDe(renta);
        if (traslado == null) {
            avanceRenta.programarRecoleccion(renta, datos.desde(), datos.hasta());
            return DetalleRentaResponse.de(renta, recoleccionDe(renta));
        }

        if (traslado.isYaInicio()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La recolección ya inició y no se puede cambiar");
        }
        validarSinEmpalme(traslado, datos.desde(), datos.hasta());

        renta.confirmarRecoleccion(datos.desde(), datos.hasta());
        traslado.reprogramar(datos.desde(), datos.hasta());
        return DetalleRentaResponse.de(renta, traslado);
    }

    private Renta buscar(Long id) {
        return rentaRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Renta no encontrada"));
    }

    // El traslado de recolección vigente; null si todavía no hay
    private Traslado recoleccionDe(Renta renta) {
        return trasladoRepositorio
                .findFirstByRentaAndTipoAndEstadoNotOrderByIdDesc(renta, TipoTraslado.RECOLECCION, EstadoTraslado.CANCELADO)
                .orElse(null);
    }

    // Si ya tiene operador, el nuevo horario no debe chocar con otro de sus traslados
    private void validarSinEmpalme(Traslado traslado, LocalDateTime desde, LocalDateTime hasta) {
        Personal operador = traslado.getResponsable();
        if (operador == null) {
            return;
        }
        List<Traslado> empalmes = trasladoRepositorio.buscarEmpalmes(
                operador.getId(), desde, hasta, traslado.getId(), EstadoTraslado.CANCELADO);
        if (!empalmes.isEmpty()) {
            Traslado conflicto = empalmes.get(0);
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
                    "Conflicto de horario: %s ya tiene asignado el traslado %s de %02d:%02d a %02d:%02d.",
                    operador.getNombres() + " " + operador.getApellidoPaterno(),
                    conflicto.getFolio(),
                    conflicto.getFechaHoraInicio().getHour(), conflicto.getFechaHoraInicio().getMinute(),
                    conflicto.getFechaHoraFin().getHour(), conflicto.getFechaHoraFin().getMinute()));
        }
    }
}