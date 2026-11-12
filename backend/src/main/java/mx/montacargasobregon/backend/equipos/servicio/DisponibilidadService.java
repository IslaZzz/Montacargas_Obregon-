package mx.montacargasobregon.backend.equipos.servicio;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;
import mx.montacargasobregon.backend.equipos.dto.DisponibilidadResponse;
import mx.montacargasobregon.backend.equipos.dto.EquipoDisponibleResponse;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.equipos.modelo.Etiqueta;
import mx.montacargasobregon.backend.equipos.repositorio.DisponibilidadRepository;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;

@Service
public class DisponibilidadService {

    private final DisponibilidadRepository repositorio;

    public DisponibilidadService(DisponibilidadRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse consultar(Long categoriaId, Etiqueta etiqueta, LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha final no puede ser anterior a la inicial.");
        }

        // Se deja 1 día de margen antes y después
        LocalDate inicioMargen = desde.minusDays(1);
        LocalDate finMargen = hasta.plusDays(1);

        // Equipos de la categoría que se pueden rentar, con la etiqueta si se pidió
        List<Equipo> candidatos = repositorio
                .findByModeloCategoriaIdAndEstadoOrderByNumeroUnidad(categoriaId, EstadoEquipo.OPERATIVO)
                .stream()
                .filter(e -> etiqueta == null || e.getEtiquetas().contains(etiqueta))
                .toList();

        // Equipos ocupados por rentas y apartados por cotizaciones aceptadas
        Set<Long> ocupados = new HashSet<>(repositorio.idsOcupadosPorRentas(
                FaseEquipoRenta.EN_REVISION_ENTRADA,
                List.of(FaseEquipoRenta.COMPLETADO, FaseEquipoRenta.CANCELADO),
                List.of(EstadoRenta.PROGRAMADA, EstadoRenta.EN_CURSO),
                inicioMargen, finMargen));
        ocupados.addAll(repositorio.idsApartadosPorCotizaciones(
                EstadoCotizacion.ACEPTADA,
                inicioMargen.atStartOfDay(),
                finMargen.plusDays(1).atStartOfDay()));

        List<EquipoDisponibleResponse> disponibles = candidatos.stream()
                .filter(e -> !ocupados.contains(e.getId()))
                .map(e -> new EquipoDisponibleResponse(
                        e.getId(),
                        e.getNumeroUnidad(),
                        e.getModelo().getNombre(),
                        e.getEtiquetas(),
                        e.getModelo().getTarifaDia()))
                .toList();

        return new DisponibilidadResponse(disponibles, candidatos.size() - disponibles.size());
    }
}
