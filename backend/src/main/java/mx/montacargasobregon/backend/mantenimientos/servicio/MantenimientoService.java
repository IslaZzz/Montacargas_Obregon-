package mx.montacargasobregon.backend.mantenimientos.servicio;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.mantenimientos.dto.AsignarRequest;
import mx.montacargasobregon.backend.mantenimientos.dto.MecanicoCargaResponse;
import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;

/** Reglas de negocio de los mantenimientos. */
@Service
public class MantenimientoService {

    // Lo que cuenta como trabajo pendiente de un mecánico
    private static final List<EstadoMantenimiento> SIN_TERMINAR =
            List.of(EstadoMantenimiento.ASIGNADO, EstadoMantenimiento.EN_PROCESO);

    private final MantenimientoRepository repositorio;
    private final PersonalRepository personalRepositorio;

    public MantenimientoService(MantenimientoRepository repositorio,
                                PersonalRepository personalRepositorio) {
        this.repositorio = repositorio;
        this.personalRepositorio = personalRepositorio;
    }

    /** Todos los mantenimientos, del más próximo a salir al más lejano. La pantalla filtra. */
    @Transactional(readOnly = true)
    public List<Mantenimiento> listar() {
        return repositorio.listar();
    }

    /** Mecánicos activos con su carga, para elegir a quién asignar. */
    @Transactional(readOnly = true)
    public List<MecanicoCargaResponse> mecanicosConCarga() {
        Map<Long, Long> carga = repositorio.findByEstadoIn(SIN_TERMINAR).stream()
                .filter(m -> m.getMecanico() != null)
                .collect(Collectors.groupingBy(m -> m.getMecanico().getId(), Collectors.counting()));

        return personalRepositorio.findByTipoAndActivoTrue(TipoPersonal.MECANICO).stream()
                .map(p -> new MecanicoCargaResponse(
                        p.getId(),
                        p.getCodigo(),
                        p.getNombres() + " " + p.getApellidoPaterno(),
                        carga.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    /**
     * Asigna o cambia el mecánico de uno o varios mantenimientos.
     * Si alguno no se puede, no se asigna ninguno.
     */
    @Transactional
    public List<Mantenimiento> asignar(AsignarRequest datos) {
        Personal mecanico = personalRepositorio.findById(datos.personalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mecánico no encontrado"));

        if (!mecanico.isActivo() || mecanico.getTipo() != TipoPersonal.MECANICO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La persona elegida no es un mecánico activo");
        }

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