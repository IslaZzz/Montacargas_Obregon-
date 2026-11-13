package mx.montacargasobregon.backend.usuarios.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.montacargasobregon.backend.usuarios.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // El correo con el que se inicia sesión, sin importar mayúsculas (HU-28)
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
}
