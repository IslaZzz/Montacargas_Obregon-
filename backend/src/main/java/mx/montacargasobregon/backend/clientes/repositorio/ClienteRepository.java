package mx.montacargasobregon.backend.clientes.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("""
        select distinct c from Cliente c
        left join c.razonesSociales r
        where lower(c.nombre)  like lower(concat('%', :texto, '%'))
           or lower(c.empresa) like lower(concat('%', :texto, '%'))
           or lower(r.rfc)     like lower(concat('%', :texto, '%'))
           or c.telefono       like concat('%', :texto, '%')
        """)
    List<Cliente> buscar(@Param("texto") String texto);
}
