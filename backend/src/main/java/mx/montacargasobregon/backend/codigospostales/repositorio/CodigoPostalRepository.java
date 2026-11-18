package mx.montacargasobregon.backend.codigospostales.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.montacargasobregon.backend.codigospostales.modelo.CodigoPostal;

public interface CodigoPostalRepository extends JpaRepository<CodigoPostal, Long> {

    List<CodigoPostal> findByCodigoPostalOrderByColoniaAsc(String codigoPostal);
}
