package mx.montacargasobregon.backend.equipos.repositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;

public interface DisponibilidadRepository extends JpaRepository<Equipo, Long> {

    // Equipos de una categoría en estado operativo
    List<Equipo> findByModeloCategoriaIdAndEstadoOrderByNumeroUnidad(Long categoriaId, EstadoEquipo estado);

    // Equipos ocupados por una renta: en revisión de entrada, o en una renta activa que se cruza con el rango
    @Query("""
            SELECT DISTINCT er.equipo.id FROM EquipoRenta er
            WHERE er.fase = :revisionEntrada
               OR (er.fase NOT IN :fasesTerminadas
                   AND er.renta.estado IN :estadosActivos
                   AND er.renta.fechaInicio <= :fin
                   AND er.renta.fechaFin >= :inicio)
            """)
    List<Long> idsOcupadosPorRentas(
            @Param("revisionEntrada") FaseEquipoRenta revisionEntrada,
            @Param("fasesTerminadas") List<FaseEquipoRenta> fasesTerminadas,
            @Param("estadosActivos") List<EstadoRenta> estadosActivos,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);

    // Equipos apartados: están en una cotización aceptada que se cruza con el rango
    @Query("""
            SELECT DISTINCT e.equipo.id FROM Cotizacion c JOIN c.equipos e
            WHERE c.estado = :estado
              AND c.fechaHoraEntrega < :finExclusivo
              AND c.fechaHoraRecoleccion >= :inicio
            """)
    List<Long> idsApartadosPorCotizaciones(
            @Param("estado") EstadoCotizacion estado,
            @Param("inicio") LocalDateTime inicio,
            @Param("finExclusivo") LocalDateTime finExclusivo);
}