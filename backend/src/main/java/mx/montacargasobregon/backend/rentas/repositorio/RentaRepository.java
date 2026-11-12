package mx.montacargasobregon.backend.rentas.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.rentas.modelo.Renta;

public interface RentaRepository extends JpaRepository<Renta, Long> {

    boolean existsByFolio(String folio);

    // La renta que salió de esa cotización, si ya se creó (HU-37)
    Optional<Renta> findByCotizacionId(Long cotizacionId);

    // Número del último folio de renta: "R-2090" -> 2090. Null si no hay rentas
    @Query("select max(cast(substring(r.folio, 3) as Integer)) from Renta r")
    Integer ultimoNumeroFolio();

    @Query("""
        SELECT DISTINCT r
        FROM Renta r
        JOIN FETCH r.cliente
        LEFT JOIN FETCH r.equipos er
        LEFT JOIN FETCH er.equipo
        """)
    List<Renta> findAllConDetalles();
}
