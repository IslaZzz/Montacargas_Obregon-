package mx.montacargasobregon.backend.avisos.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.avisos.modelo.AvisoDescuento;
import mx.montacargasobregon.backend.usuarios.Usuario;

public interface AvisoDescuentoRepository extends JpaRepository<AvisoDescuento, Long> {

    /** Los 20 avisos más recientes del usuario. */
    List<AvisoDescuento> findTop20ByDestinatarioOrderByFechaDesc(Usuario destinatario);

    @Modifying(clearAutomatically = true)
    @Query("update AvisoDescuento a set a.leido = true where a.destinatario = :usuario and a.leido = false")
    int marcarLeidos(@Param("usuario") Usuario usuario);

    /** Marca un aviso como leído, solo si es del usuario indicado. */
    @Modifying(clearAutomatically = true)
    @Query("update AvisoDescuento a set a.leido = true where a.id = :id and a.destinatario = :usuario")
    int marcarLeido(@Param("id") Long id, @Param("usuario") Usuario usuario);
}