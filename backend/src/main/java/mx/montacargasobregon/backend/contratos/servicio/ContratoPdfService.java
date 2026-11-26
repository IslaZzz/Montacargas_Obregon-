package mx.montacargasobregon.backend.contratos.servicio;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.montacargasobregon.backend.contratos.modelo.Contrato;
import mx.montacargasobregon.backend.cotizaciones.servicio.CotizacionPdfService;
import mx.montacargasobregon.backend.cotizaciones.servicio.CotizacionPdfService.ArchivoPdf;

/**
 * Genera el PDF del contrato (HU-23): la misma hoja que la cotización aceptada (cliente, razón social, equipos,
 * fechas y horas, lugar y precio), con el anticipo y su fecha límite, y la política de cancelación y penalizaciones.
 */
@Service
public class ContratoPdfService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static final String TITULO_POLITICA = "POLÍTICA DE CANCELACIÓN Y PENALIZACIONES";
    static final List<String> POLITICA = List.of(
            "Cancelación con 3 días o más antes de la entrega: se devuelve el anticipo completo.",
            "Cancelación con menos de 3 días: se pierde el anticipo como penalización.",
            "Si el anticipo no se paga antes de la fecha límite, el apartado vence y los equipos se liberan.",
            "El cliente firmará una carta responsiva al recibir el equipo.");

    private final ContratoService contratos;

    public ContratoPdfService(ContratoService contratos) {
        this.contratos = contratos;
    }

    /** Busca el contrato y genera su PDF. */
    @Transactional(readOnly = true)
    public ArchivoPdf generar(Long id) {
        Contrato contrato = contratos.obtener(id);
        return new ArchivoPdf(contrato.getFolio() + ".pdf", generar(contrato));
    }

    /** Arma el documento de un contrato ya cargado. */
    public byte[] generar(Contrato k) {
        return CotizacionPdfService.hoja(k.getCotizacion(), "CONTRATO", k.getFolio(),
                k.getFechaHoraGeneracion().toLocalDate(),
                CotizacionPdfService.recuadro(textoAnticipo(k)),
                CotizacionPdfService.lista(TITULO_POLITICA, POLITICA));
    }

    // "Anticipo 30%: $2,834.46, a pagar antes del 12/10/2026. Con el anticipo, el contrato pasa a Contratado."
    // Dice «antes del» porque ese día el apartado ya vence
    static String textoAnticipo(Contrato k) {
        return "Anticipo 30%: " + CotizacionPdfService.moneda(k.getAnticipo())
                + ", a pagar antes del " + k.getFechaLimiteAnticipo().format(FECHA)
                + ". Con el anticipo, el contrato pasa a Contratado.";
    }
}
