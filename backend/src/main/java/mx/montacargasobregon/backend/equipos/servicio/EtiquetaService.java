package mx.montacargasobregon.backend.equipos.servicio;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.montacargasobregon.backend.equipos.dto.EtiquetaRequest;
import mx.montacargasobregon.backend.equipos.modelo.Etiqueta;
import mx.montacargasobregon.backend.equipos.repositorio.EtiquetaRepository;

/** Etiquetas que se le pueden poner a un equipo (HU-02). */
@Service
public class EtiquetaService {

    private final EtiquetaRepository repositorio;

    public EtiquetaService(EtiquetaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<Etiqueta> listar() {
        return repositorio.findAll(Sort.by("nombre"));
    }

    /** Agrega una etiqueta; el nombre no se repite, sin importar mayúsculas. */
    @Transactional
    public Etiqueta crear(EtiquetaRequest datos) {
        String nombre = datos.nombre().trim();
        if (repositorio.existsByNombreIgnoreCase(nombre)) {
            throw new EtiquetaRepetidaException();
        }
        return repositorio.save(new Etiqueta(nombre));
    }
}
