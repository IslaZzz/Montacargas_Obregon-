package mx.montacargasobregon.backend.avisos.servicio;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.avisos.dto.RecoleccionProximaResponse;
import mx.montacargasobregon.backend.avisos.repositorio.RecoleccionProximaRepository;
import mx.montacargasobregon.backend.cartas.modelo.CartaResponsiva;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;

/**
 * Recordatorio de recolecciones próximas (OHU-25): rentas en curso que terminan entre hoy y
 * los días de anticipación, o que ya terminaron, y cuya recolección todavía no se confirma.
 * Igual que los apartados próximos, se calcula en cada consulta: cuando el operador confirma
 * la recolección (OHU-14), la renta deja de aparecer sola.
 */
@Service
public class RecoleccionProximaService {

    public static final int DIAS_MINIMOS = 1;
    public static final int DIAS_MAXIMOS = 30;

    private final RecoleccionProximaRepository repositorio;

    public RecoleccionProximaService(RecoleccionProximaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<RecoleccionProximaResponse> listar(int dias) {
        return listar(dias, LocalDate.now(CartaResponsiva.ZONA_HORARIA));
    }

    // Recibe "hoy" para poder probarlo con una fecha fija
    List<RecoleccionProximaResponse> listar(int dias, LocalDate hoy) {
        if (dias < DIAS_MINIMOS || dias > DIAS_MAXIMOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La anticipación debe ser de " + DIAS_MINIMOS + " a " + DIAS_MAXIMOS + " días.");
        }

        List<Renta> rentas = repositorio.listarQueTerminan(EstadoRenta.EN_CURSO, hoy.plusDays(dias));
        if (rentas.isEmpty()) {
            return List.of();
        }

        Map<Long, Traslado> recolecciones = repositorio.recoleccionesDe(rentas.stream().map(Renta::getId).toList())
                .stream()
                .collect(Collectors.toMap(t -> t.getRenta().getId(), Function.identity(), (a, b) -> a));

        return rentas.stream()
                // Confirmada (en camino) o ya recibida: ya no hay nada que recordar
                .filter(r -> !confirmada(recolecciones.get(r.getId())))
                .map(r -> RecoleccionProximaResponse.de(r, recolecciones.get(r.getId()), hoy))
                .toList();
    }

    private static boolean confirmada(Traslado recoleccion) {
        return recoleccion != null
                && (recoleccion.getEstado() == EstadoTraslado.EN_CURSO
                || recoleccion.getEstado() == EstadoTraslado.COMPLETADO);
    }
}