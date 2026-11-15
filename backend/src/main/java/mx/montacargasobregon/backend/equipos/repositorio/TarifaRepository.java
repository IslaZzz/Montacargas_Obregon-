package mx.montacargasobregon.backend.equipos.repositorio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.equipos.modelo.Modelo;

public interface TarifaRepository extends Repository<Modelo, Long> {

    List<Modelo> findAll(Sort orden);

    Optional<Modelo> findById(Long id);

    @Modifying(clearAutomatically = true)
    @Query("update Modelo m set m.tarifaDia = :tarifaDia where m.id = :id")
    int actualizarTarifa(@Param("id") Long id, @Param("tarifaDia") BigDecimal tarifaDia);
}