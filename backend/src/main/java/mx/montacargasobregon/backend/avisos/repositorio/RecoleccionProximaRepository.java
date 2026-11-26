package mx.montacargasobregon.backend.avisos.repositorio;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;

/** Consultas de solo lectura para el recordatorio de recolecciones (OHU-25). */
public interface RecoleccionProximaRepository extends Repository<Renta, Long> {

    // Las rentas en curso que terminan a más tardar en la fecha indicada (las vencidas incluidas)
    @Query("""
        select distinct r from Renta r
        join fetch r.cliente
        left join fetch r.equipos er
        left join fetch er.equipo
        where r.estado = :estado
          and r.fechaFin <= :hasta
        order by r.fechaFin, r.folio
        """)
    List<Renta> listarQueTerminan(@Param("estado") EstadoRenta estado, @Param("hasta") LocalDate hasta);

    // Las recolecciones de esas rentas, para saber si ya tienen horario o ya se confirmaron
    @Query("""
        select t from Traslado t
        where t.renta.id in :rentas
          and t.tipo = mx.montacargasobregon.backend.traslados.modelo.TipoTraslado.RECOLECCION
          and t.estado <> mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado.CANCELADO
        """)
    List<Traslado> recoleccionesDe(@Param("rentas") Collection<Long> rentas);
}