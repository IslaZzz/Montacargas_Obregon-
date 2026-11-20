package mx.montacargasobregon.backend.movil.sesion;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionMovilRepository extends JpaRepository<SesionMovil, Long> {

    // Trae a la persona en la misma consulta: la sesión se valida en cada petición
    @Query("select s from SesionMovil s join fetch s.personal where s.tokenHash = :tokenHash")
    Optional<SesionMovil> buscarPorTokenHash(@Param("tokenHash") String tokenHash);

    // Una sola sesión por persona: al entrar de nuevo se cierran las anteriores
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update SesionMovil s set s.revocada = true where s.personal.id = :personalId and s.revocada = false")
    int revocarActivasDe(@Param("personalId") Long personalId);
}

