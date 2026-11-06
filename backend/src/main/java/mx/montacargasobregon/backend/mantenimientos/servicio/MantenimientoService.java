package mx.montacargasobregon.backend.mantenimientos.servicio;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.mantenimientos.dto.AsignarRequest;
import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.servicio.PersonalService;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;

/** Reglas de negocio de los mantenimientos. */
@Service
public class MantenimientoService {

    private final MantenimientoRepository repositorio;
    private final PersonalService personalServicio;
    private final AvanceRentaService avanceRenta;

    public MantenimientoService(MantenimientoRepository repositorio, PersonalService personalServicio,
                                AvanceRentaService avanceRenta) {
        this.repositorio = repositorio;
        this.personalServicio = personalServicio;
        this.avanceRenta = avanceRenta;
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

    /**
     * Marca terminado un mantenimiento y el equipo vuelve a estar operativo.
     * Si la falla se encontró en la revisión de salida de una renta que aún no se entrega,
     * el equipo queda listo para entrega (y, si es el último, nace el traslado).
     */
    @Transactional
    public Mantenimiento terminar(Long id) {
        Mantenimiento mantenimiento = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mantenimiento no encontrado"));

        String unidad = mantenimiento.getEquipo().getNumeroUnidad();
        if (!mantenimiento.puedeTerminarse()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    mantenimiento.getEstado() == EstadoMantenimiento.PENDIENTE
                            ? "Primero asigna un mecánico al mantenimiento de " + unidad
                            : "El mantenimiento de " + unidad + " ya está terminado o cancelado");
        }

        mantenimiento.terminar();
        mantenimiento.getEquipo().cambiarEstado(EstadoEquipo.OPERATIVO);

        Revision revision = mantenimiento.getRevision();
        if (revision != null
                && revision.getTipo() == TipoRevision.SALIDA
                && revision.getEquipoRenta().getRenta().getEstado() == EstadoRenta.PROGRAMADA) {
            avanceRenta.equipoListoParaEntrega(revision.getEquipoRenta());
        }
        return mantenimiento;
    }
}