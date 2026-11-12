package mx.montacargasobregon.backend.equipos.consulta;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;

/** Equipos que están en una renta, para saber su estado en la consulta. */
public interface ConsultaEquiposRepository extends Repository<EquipoRenta, Long> {

    // Hoy: los que siguen en una renta vigente, sin importar sus fechas
    @Query("""
        select er from EquipoRenta er join fetch er.renta r
        where er.fase not in :libres
          and r.estado in :vigentes
        """)
    List<EquipoRenta> enRentasVigentes(@Param("libres") Collection<FaseEquipoRenta> libres,
                                       @Param("vigentes") Collection<EstadoRenta> vigentes);

    // En un periodo: los de una renta vigente que se cruza con esas fechas.
    // Los que esperan su revisión de entrada siguen ocupados, sea cual sea la fecha
    @Query("""
        select er from EquipoRenta er join fetch er.renta r
        where er.fase = :revisionEntrada
           or (er.fase not in :libres
               and r.estado in :vigentes
               and r.fechaInicio <= :fin
               and r.fechaFin >= :inicio)
        """)
    List<EquipoRenta> enRentasDelPeriodo(@Param("revisionEntrada") FaseEquipoRenta revisionEntrada,
                                         @Param("libres") Collection<FaseEquipoRenta> libres,
                                         @Param("vigentes") Collection<EstadoRenta> vigentes,
                                         @Param("inicio") LocalDate inicio,
                                         @Param("fin") LocalDate fin);
}