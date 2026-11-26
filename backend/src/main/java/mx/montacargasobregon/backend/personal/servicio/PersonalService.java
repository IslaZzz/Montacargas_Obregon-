package mx.montacargasobregon.backend.personal.servicio;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.repositorio.MantenimientoRepository;
import mx.montacargasobregon.backend.personal.dto.PersonalRequest;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

/** Reglas de negocio del personal. */
@Service
public class PersonalService {

    // Lo que todavía tiene que hacer alguien: con esto pendiente no se le puede dar de baja
    private static final List<EstadoTraslado> TRASLADOS_PENDIENTES =
            List.of(EstadoTraslado.PROGRAMADO, EstadoTraslado.EN_CURSO);
    private static final List<EstadoRevision> REVISIONES_PENDIENTES =
            List.of(EstadoRevision.ASIGNADO, EstadoRevision.EN_PROCESO);
    private static final List<EstadoMantenimiento> MANTENIMIENTOS_PENDIENTES =
            List.of(EstadoMantenimiento.ASIGNADO, EstadoMantenimiento.EN_PROCESO);

    private final PersonalRepository repositorio;
    private final TrasladoRepository traslados;
    private final RevisionRepository revisiones;
    private final MantenimientoRepository mantenimientos;

    public PersonalService(PersonalRepository repositorio, TrasladoRepository traslados,
                           RevisionRepository revisiones, MantenimientoRepository mantenimientos) {
        this.repositorio = repositorio;
        this.traslados = traslados;
        this.revisiones = revisiones;
        this.mantenimientos = mantenimientos;
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

    /**
     * Busca a un mecánico para asignarle trabajo.
     * Responde 404 si no existe y 400 si no es mecánico o ya no está activo.
     */
    @Transactional(readOnly = true)
    public Personal mecanicoActivo(Long id) {
        Personal mecanico = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mecánico no encontrado"));

        if (!mecanico.isActivo() || mecanico.getTipo() != TipoPersonal.MECANICO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La persona elegida no es un mecánico activo");
        }
        return mecanico;
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

    /**
     * Desactiva a alguien del personal. No se borra, conserva su historial.
     * Si tiene trabajo pendiente responde 409 con lo que falta, para que primero se reasigne.
     */
    @Transactional
    public void desactivar(Long id) {
        Personal personal = obtener(id);

        List<String> pendientes = new ArrayList<>();
        agregar(pendientes, traslados.countByResponsableIdAndEstadoIn(id, TRASLADOS_PENDIENTES),
                "traslado", "traslados");
        agregar(pendientes, revisiones.countByMecanicoIdAndEstadoIn(id, REVISIONES_PENDIENTES),
                "revisión", "revisiones");
        agregar(pendientes, mantenimientos.countByMecanicoIdAndEstadoIn(id, MANTENIMIENTOS_PENDIENTES),
                "mantenimiento", "mantenimientos");

        if (!pendientes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede dar de baja: tiene pendientes " + unir(pendientes)
                            + ". Reasígnalos o termínalos primero.");
        }
        personal.desactivar();
    }

    // "1 traslado", "2 revisiones"; nada si es cero
    private static void agregar(List<String> pendientes, long cuantos, String singular, String plural) {
        if (cuantos > 0) {
            pendientes.add(cuantos + " " + (cuantos == 1 ? singular : plural));
        }
    }

    // "a", "a y b", "a, b y c"
    private static String unir(List<String> partes) {
        if (partes.size() == 1) {
            return partes.get(0);
        }
        return String.join(", ", partes.subList(0, partes.size() - 1)) + " y " + partes.get(partes.size() - 1);
    }
}