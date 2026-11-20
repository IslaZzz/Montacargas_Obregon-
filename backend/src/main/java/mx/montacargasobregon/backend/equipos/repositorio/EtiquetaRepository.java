package mx.montacargasobregon.backend.equipos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.montacargasobregon.backend.equipos.modelo.Etiqueta;

public interface EtiquetaRepository extends JpaRepository<Etiqueta, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}
