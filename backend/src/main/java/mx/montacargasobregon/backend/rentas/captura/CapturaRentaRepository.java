package mx.montacargasobregon.backend.rentas.captura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/**
 * Consultas que necesita la confirmación temporal de horarios.
 * Va aparte de RentaRepository para no chocar con la parte de consultar rentas.
 */
public interface CapturaRentaRepository extends JpaRepository<Renta, Long> {

    // Rentas en curso que todavía no tienen horario de recolección, las que terminan antes primero
    List<Renta> findByEstadoAndRecoleccionDesdeIsNullOrderByFechaFinAsc(EstadoRenta estado);
}