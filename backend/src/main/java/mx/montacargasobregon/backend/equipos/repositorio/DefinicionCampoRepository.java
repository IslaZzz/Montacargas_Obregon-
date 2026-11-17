package mx.montacargasobregon.backend.equipos.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.montacargasobregon.backend.equipos.modelo.DefinicionCampo;

public interface DefinicionCampoRepository extends JpaRepository<DefinicionCampo, Long> {

    List<DefinicionCampo> findByCategoriaIdOrderByOrden(Long categoriaId);
}
