package mx.montacargasobregon.backend.equipos.repositorio
import mx.montacargasobregon.backend.equipos.modelo.Categoria;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}