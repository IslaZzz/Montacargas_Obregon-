package mx.montacargasobregon.backend.equipos.servicio;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.equipos.dto.EquipoRequest;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.equipos.repositorio.EquipoRepository;
import mx.montacargasobregon.backend.equipos.repositorio.ModeloRepository;

/** Reglas de negocio de los equipos. */
@Service
public class EquipoService {

    private final EquipoRepository repositorio;
    private final ModeloRepository modeloRepositorio;

    public EquipoService(EquipoRepository repositorio, ModeloRepository modeloRepositorio) {
        this.repositorio = repositorio;
        this.modeloRepositorio = modeloRepositorio;
    }

    /** Registra una unidad nueva. Ni el número de unidad ni el de serie se repiten. */
    @Transactional
    public Equipo registrar(EquipoRequest datos) {
        String numeroUnidad = datos.numeroUnidad().trim().toUpperCase();
        String numeroSerie = datos.numeroSerie().trim().toUpperCase();

        if (repositorio.existsByNumeroUnidad(numeroUnidad)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un equipo con ese número de unidad");
        }
        if (repositorio.existsByNumeroSerie(numeroSerie)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un equipo con ese número de serie");
        }

        Equipo equipo = new Equipo(
                numeroUnidad, numeroSerie, datos.anioCompra(), obtenerModelo(datos.modeloId()));
        equipo.asignarEtiquetas(datos.etiquetas());

        return repositorio.save(equipo);
    }

    /** Busca equipos por número de unidad, de serie, o nombre o marca del modelo. Sin texto, devuelve todos. */
    @Transactional(readOnly = true)
    public List<Equipo> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return repositorio.findAll();
        }
        return repositorio.buscar(texto.trim());
    }

    /** Obtiene un equipo por su id; si no existe, responde 404. */
    @Transactional(readOnly = true)
    public Equipo obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo no encontrado"));
    }

    /** Cambia los datos de un equipo existente. */
    @Transactional
    public Equipo actualizar(Long id, EquipoRequest datos) {
        Equipo equipo = obtener(id);
        String numeroUnidad = datos.numeroUnidad().trim().toUpperCase();
        String numeroSerie = datos.numeroSerie().trim().toUpperCase();

        // Pueden repetirse consigo mismos, pero no con los de otro equipo
        if (repositorio.existsByNumeroUnidadAndIdNot(numeroUnidad, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un equipo con ese número de unidad");
        }
        if (repositorio.existsByNumeroSerieAndIdNot(numeroSerie, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un equipo con ese número de serie");
        }

        equipo.actualizar(numeroUnidad, numeroSerie, datos.anioCompra(), obtenerModelo(datos.modeloId()));
        equipo.asignarEtiquetas(datos.etiquetas());

        return equipo; // Dentro de la transacción, JPA guarda el cambio solo
    }

    /** Cambia el estado operativo de un equipo. */
    @Transactional
    public Equipo cambiarEstado(Long id, EstadoEquipo estado) {
        Equipo equipo = obtener(id);
        equipo.cambiarEstado(estado);
        return equipo;
    }

    /**
     * Da de baja un equipo. No se borra: conserva su historial de rentas,
     * revisiones y traslados, y deja de aparecer como disponible.
     */
    @Transactional
    public void darDeBaja(Long id) {
        obtener(id).darDeBaja();
    }

    private Modelo obtenerModelo(Long modeloId) {
        return modeloRepositorio.findById(modeloId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo no encontrado"));
    }
}