package mx.montacargasobregon.backend.contratos.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pasa a Apartado vencido los contratos que terminaron el día de su fecha límite sin anticipo (HU-23).
 * Revisa al arrancar el backend y después al inicio de cada minuto, con la hora de Sonora,
 * igual que el vencimiento de las cotizaciones: el primer minuto del día siguiente ya los vence.
 */
@Component
public class VencimientoApartadosService {

    private static final Logger log = LoggerFactory.getLogger(VencimientoApartadosService.class);

    private final ContratoService contratos;

    public VencimientoApartadosService(ContratoService contratos) {
        this.contratos = contratos;
    }

    /** Al arrancar: los que vencieron mientras el backend estaba apagado. */
    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        revisar();
    }

    @Scheduled(cron = "0 * * * * *", zone = "America/Hermosillo")
    public void cadaMinuto() {
        revisar();
    }

    private void revisar() {
        int vencidos = contratos.vencerApartados();
        if (vencidos > 0) {
            log.info("Contratos que pasaron a Apartado vencido: {}", vencidos);
        }
    }
}
