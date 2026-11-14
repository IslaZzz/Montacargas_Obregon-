package mx.montacargasobregon.backend.equipos.servicio;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.equipos.dto.CategoriaRequest;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.repositorio.CategoriaRepository;

@Service
public class CategoriaService {

    private final CategoriaRepository repositorio;

    public CategoriaService(CategoriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return repositorio.findAll(Sort.by("nombre"));
    }

    @Transactional(readOnly = true)
    public Categoria obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
    }

    @Transactional
    public Categoria crear(CategoriaRequest datos) {
        String nombre = datos.nombre().trim();
        if (repositorio.existsByNombreIgnoreCase(nombre)) {
            throw new CategoriaRepetidaException();
        }
        return repositorio.save(new Categoria(nombre));
    }

    @Transactional
    public Categoria actualizar(Long id, CategoriaRequest datos) {
        Categoria categoria = obtener(id);
        String nombre = datos.nombre().trim();
        if (repositorio.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new CategoriaRepetidaException();
        }
        categoria.cambiarNombre(nombre);
        return categoria;
    }
}
