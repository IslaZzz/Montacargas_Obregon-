package mx.montacargasobregon.backend.cotizaciones.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marca como Vencidas las cotizaciones Enviadas que pasaron sus 7 días sin respuesta (HU-17).
 * Revisa al arrancar el backend y después al inicio de cada hora, con la hora de Sonora.
 */
@Component
public class VencimientoCotizacionesService {

    private static final Logger log = LoggerFactory.getLogger(VencimientoCotizacionesService.class);

    private final CotizacionService cotizaciones;

    public VencimientoCotizacionesService(CotizacionService cotizaciones) {
        this.cotizaciones = cotizaciones;
    }

    /** Al arrancar: las que vencieron mientras el backend estaba apagado. */
    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        revisar();
    }

    /** Al inicio de cada minuto. */
    @Scheduled(cron = "0 * * * * *", zone = "America/Hermosillo")
    public void cadaMinuto() {
        revisar();
    }

    private void revisar() {
        int vencidas = cotizaciones.vencerCotizaciones();
        if (vencidas > 0) {
            log.info("Cotizaciones que pasaron a Vencida: {}", vencidas);
        }
    }
}