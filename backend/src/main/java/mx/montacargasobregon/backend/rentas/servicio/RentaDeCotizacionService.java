package mx.montacargasobregon.backend.rentas.servicio;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.rentas.repositorio.RentaRepository;

/**
 * Crea la renta de una cotización aceptada (HU-37) para que Operativo prepare la entrega.
 * En Delivery S3 la renta nacerá al registrar el anticipo del contrato (HU-23 y HU-24).
 */
@Service
public class RentaDeCotizacionService {

    private final RentaRepository repositorio;
    private final AvanceRentaService avanceRenta;

    public RentaDeCotizacionService(RentaRepository repositorio, AvanceRentaService avanceRenta) {
        this.repositorio = repositorio;
        this.avanceRenta = avanceRenta;
    }

    /**
     * Crea la renta de la cotización; si ya tiene una, regresa esa: cada cotización genera una sola.
     * La entrega nace programada, así que también nacen sus revisiones de salida, como en el flujo de Operativo.
     */
    @Transactional
    public Renta crear(Cotizacion cotizacion) {
        return repositorio.findByCotizacionId(cotizacion.getId()).orElseGet(() -> {
            Renta renta = repositorio.save(new Renta(siguienteFolio(), cotizacion));
            avanceRenta.programarRevisionesDeSalida(renta);
            return renta;
        });
    }

    /** La renta que salió de esa cotización, si ya se creó. */
    @Transactional(readOnly = true)
    public Optional<Renta> buscar(Long cotizacionId) {
        return repositorio.findByCotizacionId(cotizacionId);
    }

    // Misma serie que las rentas de Operativo: R-2090 -> R-2091
    private String siguienteFolio() {
        Integer ultimo = repositorio.ultimoNumeroFolio();
        return String.format("R-%04d", ultimo == null ? 1 : ultimo + 1);
    }
}
