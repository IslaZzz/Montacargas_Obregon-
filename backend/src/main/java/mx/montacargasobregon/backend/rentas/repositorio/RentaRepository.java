package mx.montacargasobregon.backend.rentas.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.montacargasobregon.backend.rentas.modelo.Renta;

public interface RentaRepository extends JpaRepository<Renta, Long> {

    boolean existsByFolio(String folio);
}