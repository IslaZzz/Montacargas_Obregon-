package mx.montacargasobregon.backend.personal.servicio;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.personal.dto.PersonalRequest;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;

/** Reglas de negocio del personal. */
@Service
public class PersonalService {

    private final PersonalRepository repositorio;

    public PersonalService(PersonalRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Registra a alguien del personal. El código no se puede repetir. */
    @Transactional
    public Personal registrar(PersonalRequest datos) {
        String codigo = datos.codigo().trim().toUpperCase();

        if (repositorio.existsByCodigo(codigo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe personal con ese código");
        }

        return repositorio.save(new Personal(
                codigo, datos.nombres().trim(), datos.apellidoPaterno().trim(),
                datos.apellidoMaterno(), datos.tipo()));
    }

    /**
     * Con tipo, devuelve solo los activos de esa función; es lo que se usa en asignación.
     * Sin tipo, busca por texto, y sin texto, todos.
     */
    @Transactional(readOnly = true)
    public List<Personal> buscar(String texto, TipoPersonal tipo) {
        if (tipo != null) {
            return repositorio.findByTipoAndActivoTrue(tipo);
        }
        if (texto == null || texto.isBlank()) {
            return repositorio.findAll();
        }
        return repositorio.buscar(texto.trim());
    }

    /** Obtiene personal por su id; si no existe, responde 404. */
    @Transactional(readOnly = true)
    public Personal obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personal no encontrado"));
    }

    /** Cambia los datos de alguien del personal. */
    @Transactional
    public Personal actualizar(Long id, PersonalRequest datos) {
        Personal personal = obtener(id);
        String codigo = datos.codigo().trim().toUpperCase();

        // El código puede repetirse consigo mismo, pero no con el de otro
        if (repositorio.existsByCodigoAndIdNot(codigo, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe personal con ese código");
        }

        personal.actualizar(codigo, datos.nombres().trim(), datos.apellidoPaterno().trim(),
                datos.apellidoMaterno(), datos.tipo());
        return personal; // Dentro de la transacción, JPA guarda el cambio solo
    }

    /** Desactiva a alguien del personal. No se borra, conserva su historial. */
    @Transactional
    public void desactivar(Long id) {
        obtener(id).desactivar();
    }
}