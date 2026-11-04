package mx.montacargasobregon.backend.equipos.repositorio;

import mx.montacargasobregon.backend.equipos.modelo.Equipo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    boolean existsByNumeroUnidad(String numeroUnidad);

    boolean existsByNumeroSerie(String numeroSerie);

    boolean existsByNumeroUnidadAndIdNot(String numeroUnidad, Long id);

    boolean existsByNumeroSerieAndIdNot(String numeroSerie, Long id);

    @Query("""
        select e from Equipo e
        where lower(e.numeroUnidad)  like lower(concat('%', :texto, '%'))
           or lower(e.numeroSerie)   like lower(concat('%', :texto, '%'))
           or lower(e.modelo.nombre) like lower(concat('%', :texto, '%'))
           or lower(e.modelo.marca)  like lower(concat('%', :texto, '%'))
        """)
    List<Equipo> buscar(@Param("texto") String texto);
}