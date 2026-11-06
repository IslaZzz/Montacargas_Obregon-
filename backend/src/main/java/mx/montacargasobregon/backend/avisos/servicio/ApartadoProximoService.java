package mx.montacargasobregon.backend.avisos.servicio;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.avisos.dto.ApartadoProximoResponse;
import mx.montacargasobregon.backend.avisos.repositorio.ApartadoProximoRepository;
import mx.montacargasobregon.backend.cartas.modelo.CartaResponsiva;
import mx.montacargasobregon.backend.cartas.modelo.EstadoCarta;
import mx.montacargasobregon.backend.cartas.repositorio.CartaResponsivaRepository;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/**
 * Avisos de apartado próximo (OHU-24): rentas programadas que empiezan pronto
 * y a las que todavía les falta la carta responsiva firmada.
 * Se calculan en cada consulta, así que una renta no puede avisarse dos veces.
 */
@Service
public class ApartadoProximoService {

    // La pantalla ofrece 1, 3, 5, 7 o 10 días; se deja margen por si se agregan opciones
    public static final int DIAS_MINIMOS = 1;
    public static final int DIAS_MAXIMOS = 30;

    private final ApartadoProximoRepository rentas;
    private final CartaResponsivaRepository cartas;

    public ApartadoProximoService(ApartadoProximoRepository rentas, CartaResponsivaRepository cartas) {
        this.rentas = rentas;
        this.cartas = cartas;
    }

    /** Apartados que empiezan entre hoy y dentro de los días indicados, del más próximo al más lejano. */
    @Transactional(readOnly = true)
    public List<ApartadoProximoResponse> listar(int dias) {
        return listar(dias, LocalDate.now(CartaResponsiva.ZONA_HORARIA));
    }

    // Recibe "hoy" para poder probarlo con una fecha fija
    List<ApartadoProximoResponse> listar(int dias, LocalDate hoy) {
        if (dias < DIAS_MINIMOS || dias > DIAS_MAXIMOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La anticipación debe ser de " + DIAS_MINIMOS + " a " + DIAS_MAXIMOS + " días.");
        }

        List<Renta> programadas = rentas.listarQueInician(EstadoRenta.PROGRAMADA, hoy, hoy.plusDays(dias));
        if (programadas.isEmpty()) {
            return List.of();
        }

        Map<Long, EstadoCarta> estadoPorRenta = cartas
                .findByRentaIdIn(programadas.stream().map(Renta::getId).toList())
                .stream()
                .collect(Collectors.toMap(c -> c.getRenta().getId(), CartaResponsiva::getEstado));

        return programadas.stream()
                .map(r -> ApartadoProximoResponse.de(
                        r, estadoPorRenta.getOrDefault(r.getId(), EstadoCarta.SIN_GENERAR), hoy))
                // Con la carta firmada ya no hay nada que avisar
                .filter(a -> a.estadoCarta() != EstadoCarta.FIRMADA)
                .toList();
    }
}
