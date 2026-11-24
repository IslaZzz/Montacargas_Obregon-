package mx.montacargasobregon.backend.avisos.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.avisos.modelo.AvisoOperacion;

public interface AvisoOperacionRepository extends JpaRepository<AvisoOperacion, Long> {

    /** Los 20 avisos más recientes de un usuario de la oficina. */
    List<AvisoOperacion> findTop20ByUsuarioIdOrderByFechaDesc(Long usuarioId);

    /** Los avisos sin leer de una persona del personal (app móvil). */
    List<AvisoOperacion> findByPersonalIdAndLeidoFalseOrderByFechaDesc(Long personalId);

    @Modifying(clearAutomatically = true)
    @Query("update AvisoOperacion a set a.leido = true where a.usuario.id = :usuarioId and a.leido = false")
    int marcarLeidosDeUsuario(@Param("usuarioId") Long usuarioId);

    /** Marca un aviso como leído, solo si es del usuario indicado. */
    @Modifying(clearAutomatically = true)
    @Query("update AvisoOperacion a set a.leido = true where a.id = :id and a.usuario.id = :usuarioId")
    int marcarLeidoDeUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Modifying(clearAutomatically = true)
    @Query("update AvisoOperacion a set a.leido = true where a.personal.id = :personalId and a.leido = false")
    int marcarLeidosDePersonal(@Param("personalId") Long personalId);
}