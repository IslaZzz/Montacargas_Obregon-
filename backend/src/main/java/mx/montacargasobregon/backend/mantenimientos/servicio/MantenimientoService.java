package mx.montacargasobregon.backend.mantenimientos.servicio;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.mantenimientos.dto.AsignarRequest;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.servicio.PersonalService;

/** Reglas de negocio de los mantenimientos. */
@Service
public class MantenimientoService {

    private final MantenimientoRepository repositorio;
    private final PersonalService personalServicio;

    public MantenimientoService(MantenimientoRepository repositorio, PersonalService personalServicio) {
        this.repositorio = repositorio;
        this.personalServicio = personalServicio;
    }

    /** Todos los mantenimientos, del más próximo a salir al más lejano. La pantalla filtra. */
    @Transactional(readOnly = true)
    public List<Mantenimiento> listar() {
        return repositorio.listar();
    }

    /**
     * Asigna o cambia el mecánico de uno o varios mantenimientos.
     * Si alguno no se puede, no se asigna ninguno.
     */
    @Transactional
    public List<Mantenimiento> asignar(AsignarRequest datos) {
        Personal mecanico = personalServicio.mecanicoActivo(datos.personalId());

        // Sin repetidos, por si el front manda el mismo id dos veces
        Set<Long> ids = new HashSet<>(datos.mantenimientoIds());
        List<Mantenimiento> mantenimientos = repositorio.findAllById(ids);
        if (mantenimientos.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uno o más mantenimientos no existen");
        }

        // Primero se revisan todos y luego se asignan, para no dejar la mitad hecha
        for (Mantenimiento m : mantenimientos) {
            if (!m.puedeAsignarse()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        m.getEquipo().getNumeroUnidad() + " ya está terminado o cancelado y no se puede asignar");
            }
        }

        mantenimientos.forEach(m -> m.asignar(mecanico));
        return mantenimientos; // Dentro de la transacción, JPA guarda los cambios solo
    }
}