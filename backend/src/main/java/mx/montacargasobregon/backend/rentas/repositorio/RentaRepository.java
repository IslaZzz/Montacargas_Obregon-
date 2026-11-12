package mx.montacargasobregon.backend.rentas.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.rentas.modelo.Renta;

public interface RentaRepository extends JpaRepository<Renta, Long> {

    boolean existsByFolio(String folio);

    @Query("""
        SELECT DISTINCT r
        FROM Renta r
        JOIN FETCH r.cliente
        LEFT JOIN FETCH r.equipos er
        LEFT JOIN FETCH er.equipo
        """)
    List<Renta> findAllConDetalles();
}
