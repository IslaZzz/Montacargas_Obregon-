package mx.montacargasobregon.backend.cotizaciones.servicio;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;
import mx.montacargasobregon.backend.cotizaciones.repositorio.CotizacionRepository;

/** Reglas de negocio para consultar las cotizaciones y cambiar su estado. */
@Service
public class CotizacionService {

    private final CotizacionRepository repositorio;

    public CotizacionService(CotizacionRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Todas las cotizaciones, la más nueva primero. */
    @Transactional(readOnly = true)
    public List<Cotizacion> listar() {
        return repositorio.findAllByOrderByIdDesc();
    }

    /** Obtiene una cotización por su id; si no existe, responde 404. */
    @Transactional(readOnly = true)
    public Cotizacion obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cotización no encontrada"));
    }

    /**
     * Cambia el estado: Borrador → Enviada → Aceptada o No aceptada.
     * Repetir el estado actual no cambia nada (por ejemplo, volver a descargar el PDF de una enviada).
     */
    @Transactional
    public Cotizacion cambiarEstado(Long id, EstadoCotizacion nuevoEstado) {
        Cotizacion cotizacion = obtener(id);

        if (cotizacion.getEstado() == nuevoEstado) {
            return cotizacion;
        }
        if (!cotizacion.getEstado().puedeCambiarA(nuevoEstado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cotización ya no puede cambiar a ese estado");
        }
        // Sin equipos no hay PDF que mandar (HU-18)
        if (nuevoEstado == EstadoCotizacion.ENVIADA && cotizacion.getEquipos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Agrega al menos un equipo");
        }

        cotizacion.cambiarEstado(nuevoEstado, LocalDate.now(Cotizacion.ZONA_HORARIA));
        return cotizacion; // Dentro de la transacción, JPA guarda el cambio solo
    }
}
