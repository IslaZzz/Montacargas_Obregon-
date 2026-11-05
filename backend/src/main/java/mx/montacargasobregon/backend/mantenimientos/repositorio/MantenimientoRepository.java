package mx.montacargasobregon.backend.mantenimientos.repositorio;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;

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

    // Para calcular cuántos mantenimientos sin terminar tiene cada mecánico
    List<Mantenimiento> findByEstadoIn(Collection<EstadoMantenimiento> estados);
}