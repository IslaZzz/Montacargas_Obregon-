package mx.montacargasobregon.backend.cotizaciones.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    /** Todas las cotizaciones, la más nueva primero. */
    List<Cotizacion> findAllByOrderByIdDesc();

    /** El id más alto guardado, o 0 si aún no hay cotizaciones. Sirve para armar el siguiente folio (HU-12). */
    @Query("select coalesce(max(c.id), 0) from Cotizacion c")
    long ultimoId();
}
