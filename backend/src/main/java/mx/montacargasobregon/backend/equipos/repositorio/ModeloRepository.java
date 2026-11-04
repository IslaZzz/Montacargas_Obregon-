package mx.montacargasobregon.backend.equipos.repositorio;

import mx.montacargasobregon.backend.equipos.modelo.Modelo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModeloRepository extends JpaRepository<Modelo, Long> {
}