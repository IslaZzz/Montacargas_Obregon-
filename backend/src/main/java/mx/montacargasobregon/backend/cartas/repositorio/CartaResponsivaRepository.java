package mx.montacargasobregon.backend.cartas.repositorio;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.montacargasobregon.backend.cartas.modelo.CartaResponsiva;

public interface CartaResponsivaRepository extends JpaRepository<CartaResponsiva, Long> {

    // Las cartas de varias rentas en una sola consulta
    List<CartaResponsiva> findByRentaIdIn(Collection<Long> rentaIds);
}
