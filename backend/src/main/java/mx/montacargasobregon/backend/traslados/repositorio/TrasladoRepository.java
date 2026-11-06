package mx.montacargasobregon.backend.traslados.repositorio;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;

public interface TrasladoRepository extends JpaRepository<Traslado, Long> {

    /** Todos los traslados con renta, cliente y responsable cargados, ordenados cronológicamente. */
    @Query("""
        SELECT t FROM Traslado t
        JOIN FETCH t.renta r
        JOIN FETCH r.cliente c
        LEFT JOIN FETCH t.responsable resp
        ORDER BY t.fechaHoraInicio ASC
    """)
    List<Traslado> listarTodosConRelaciones();

    /** Traslados activos de un operador que se empalman con un intervalo [inicio, fin], excluyendo un traslado si aplica. */
    @Query("""
        SELECT t FROM Traslado t
        WHERE t.responsable.id = :operadorId
          AND (:excluirId IS NULL OR t.id != :excluirId)
          AND t.estado != :cancelado
          AND t.fechaHoraInicio < :fin
          AND t.fechaHoraFin > :inicio
    """)
    List<Traslado> buscarEmpalmes(
            @Param("operadorId") Long operadorId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("excluirId") Long excluirId,
            @Param("cancelado") EstadoTraslado cancelado
    );

    /** Traslados activos asignados a un operador en un rango de fechas (por ejemplo un día completo). */
    @Query("""
        SELECT t FROM Traslado t
        WHERE t.responsable.id = :operadorId
          AND t.estado != :cancelado
          AND t.fechaHoraInicio >= :desde
          AND t.fechaHoraInicio <= :hasta
        ORDER BY t.fechaHoraInicio ASC
    """)
    List<Traslado> buscarPorOperadorYRango(
            @Param("operadorId") Long operadorId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("cancelado") EstadoTraslado cancelado
    );

    /** Si la renta ya tiene un traslado de ese tipo que no esté cancelado. */
    boolean existsByRentaAndTipoAndEstadoNot(Renta renta, TipoTraslado tipo, EstadoTraslado estado);

    /** Número del último folio: "TR-0086" -> 86. Null si no hay traslados. */
    @Query("select max(cast(substring(t.folio, 4) as Integer)) from Traslado t")
    Integer ultimoNumeroFolio();
}

