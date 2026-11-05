package mx.montacargasobregon.backend.cotizaciones.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    /** Todas las cotizaciones, la más nueva primero. */
    List<Cotizacion> findAllByOrderByIdDesc();
}
