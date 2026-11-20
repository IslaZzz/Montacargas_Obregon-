package mx.montacargasobregon.backend.cotizaciones.repositorio;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    /** Todas las cotizaciones, la más nueva primero. */
    List<Cotizacion> findAllByOrderByIdDesc();

    /** El id más alto guardado, o 0 si aún no hay cotizaciones. Sirve para armar el siguiente folio (HU-12). */
    @Query("select coalesce(max(c.id), 0) from Cotizacion c")
    long ultimoId();

    // Las cotizaciones que tienen una solicitud de autorización (pendiente o respondida)
    List<Cotizacion> findByAutorizacionIsNotNull();

    long countByEstado(EstadoCotizacion estado);

    /** Las cotizaciones en un estado creadas antes de cierto momento: sirve para encontrar las vencidas (HU-17). */
    List<Cotizacion> findByEstadoAndFechaHoraCreacionBefore(EstadoCotizacion estado, LocalDateTime limite);
}