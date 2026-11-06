package mx.montacargasobregon.backend.mantenimientos.repositorio;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.personal.dto.CargaPorMecanico;

public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    // Trae de una vez equipo, modelo, categoría y personas, en lugar de una consulta por fila
    @Query("""
        select m from Mantenimiento m
        join fetch m.equipo e
        join fetch e.modelo mo
        join fetch mo.categoria
        left join fetch m.reportadoPor
        left join fetch m.mecanico
        order by m.fechaLimite
        """)
    List<Mantenimiento> listar();

    // Cuántos mantenimientos en esos estados tiene cada mecánico, contados en la base de datos
    @Query("""
        select new mx.montacargasobregon.backend.personal.dto.CargaPorMecanico(m.mecanico.id, count(m))
        from Mantenimiento m
        where m.mecanico is not null and m.estado in :estados
        group by m.mecanico.id
        """)
    List<CargaPorMecanico> contarPorMecanico(@Param("estados") Collection<EstadoMantenimiento> estados);
}