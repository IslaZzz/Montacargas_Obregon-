package mx.montacargasobregon.backend.revisiones.repositorio;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;

public interface RevisionRepository extends JpaRepository<Revision, Long> {

    // Trae de una vez renta, cliente, equipo, modelo y mecánico, en lugar de una consulta por fila.
    // El orden deja juntas las revisiones de la misma renta.
    @Query("""
        select rv from Revision rv
        join fetch rv.equipoRenta er
        join fetch er.renta r
        join fetch r.cliente
        join fetch er.equipo e
        join fetch e.modelo mo
        join fetch mo.categoria
        left join fetch rv.mecanico
        order by rv.fechaProgramada, r.folio, e.numeroUnidad
        """)
    List<Revision> listar();

    // Para calcular cuántas revisiones sin terminar tiene cada mecánico
    List<Revision> findByEstadoIn(Collection<EstadoRevision> estados);
}