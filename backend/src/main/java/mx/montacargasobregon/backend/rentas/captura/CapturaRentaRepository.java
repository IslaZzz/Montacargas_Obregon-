package mx.montacargasobregon.backend.rentas.captura;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/**
 * Consultas que necesita la captura manual de rentas.
 * Va aparte de RentaRepository para no chocar con la parte de consultar rentas.
 */
public interface CapturaRentaRepository extends JpaRepository<Renta, Long> {

    // Equipos que siguen en una renta vigente que se cruza con esas fechas.
    // Los que ya regresaron (o se cancelaron) cuentan como libres aunque su renta siga abierta
    @Query("""
        select distinct er.equipo.id from EquipoRenta er
        where er.renta.estado in :estados
          and er.fase not in :libres
          and er.renta.fechaInicio <= :fin
          and er.renta.fechaFin >= :inicio
        """)
    List<Long> equiposOcupados(@Param("inicio") LocalDate inicio,
                               @Param("fin") LocalDate fin,
                               @Param("estados") Collection<EstadoRenta> estados,
                               @Param("libres") Collection<FaseEquipoRenta> libres);

    // Número del último folio de renta: "R-2090" -> 2090. Null si no hay rentas
    @Query("select max(cast(substring(r.folio, 3) as Integer)) from Renta r")
    Integer ultimoNumeroFolio();
}