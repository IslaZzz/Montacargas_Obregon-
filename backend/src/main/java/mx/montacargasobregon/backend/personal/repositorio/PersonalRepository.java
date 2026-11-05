package mx.montacargasobregon.backend.personal.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;

public interface PersonalRepository extends JpaRepository<Personal, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    // Para asignación, solo quien está activo
    List<Personal> findByTipoAndActivoTrue(TipoPersonal tipo);

    @Query("""
        select p from Personal p
        where lower(p.nombres)         like lower(concat('%', :texto, '%'))
           or lower(p.apellidoPaterno) like lower(concat('%', :texto, '%'))
           or lower(p.codigo)          like lower(concat('%', :texto, '%'))
        """)
    List<Personal> buscar(@Param("texto") String texto);
}