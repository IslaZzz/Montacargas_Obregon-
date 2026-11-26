package mx.montacargasobregon.backend.contratos.repositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.contratos.modelo.Contrato;
import mx.montacargasobregon.backend.contratos.modelo.EstadoContrato;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    // El contrato que salió de esa cotización, si ya se generó
    Optional<Contrato> findByCotizacionId(Long cotizacionId);

    // Número del último folio: "CON-0087" -> 87. Null si no hay contratos
    @Query("select max(cast(substring(c.folio, 5) as Integer)) from Contrato c")
    Integer ultimoNumeroFolio();

    // Los apartados cuya fecha límite de anticipo ya llegó
    List<Contrato> findByEstadoAndFechaLimiteAnticipoLessThanEqual(EstadoContrato estado, LocalDate hoy);

    /**
     * De esos equipos, los que están en el contrato de otra cotización con ese estado y en fechas que se
     * empalman con [desde, hasta). Se compara por día, como la disponibilidad.
     */
    @Query("""
            select distinct e.equipo.id
            from Contrato k
            join k.cotizacion c
            join c.equipos e
            where k.estado = :estado
              and c.id <> :cotizacionId
              and e.equipo.id in :equipoIds
              and c.fechaHoraEntrega < :hasta
              and c.fechaHoraRecoleccion >= :desde
            """)
    List<Long> equiposEnContratoDeOtraCotizacion(@Param("estado") EstadoContrato estado,
            @Param("cotizacionId") Long cotizacionId,
            @Param("equipoIds") Collection<Long> equipoIds,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
