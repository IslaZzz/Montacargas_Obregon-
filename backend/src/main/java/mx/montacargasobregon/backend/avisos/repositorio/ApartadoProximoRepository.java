package mx.montacargasobregon.backend.avisos.repositorio;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/**
 * Consultas de rentas que solo usan los avisos.
 * Es de solo lectura y vive aquí para no cambiar RentaRepository.java.
 */
public interface ApartadoProximoRepository extends Repository<Renta, Long> {

    // Trae de una vez cliente, equipos y modelos, en lugar de una consulta por renta
    @Query("""
        select distinct r from Renta r
        join fetch r.cliente
        left join fetch r.equipos er
        left join fetch er.equipo e
        left join fetch e.modelo
        where r.estado = :estado
          and r.fechaInicio between :desde and :hasta
        order by r.fechaInicio, r.folio
        """)
    List<Renta> listarQueInician(@Param("estado") EstadoRenta estado,
                                 @Param("desde") LocalDate desde,
                                 @Param("hasta") LocalDate hasta);
}
