package mx.montacargasobregon.backend.cartas.servicio;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.cartas.dto.CambioCarta;
import mx.montacargasobregon.backend.cartas.dto.CartaResponse;
import mx.montacargasobregon.backend.cartas.modelo.CartaResponsiva;
import mx.montacargasobregon.backend.cartas.modelo.EstadoCarta;
import mx.montacargasobregon.backend.cartas.repositorio.CartaResponsivaRepository;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.rentas.repositorio.RentaRepository;

/**
 * Carta responsiva de una renta (OHU-04): consultarla, generarla o regenerarla y obtener su PDF.
 * El envío por correo al cliente queda pendiente: falta decidir con qué cuenta se envía.
 */
@Service
public class CartaResponsivaService {

    /** El PDF de la carta, con el nombre del archivo. */
    public record ArchivoCarta(String nombre, byte[] contenido) {
    }

    private final RentaRepository rentas;
    private final CartaResponsivaRepository cartas;
    private final CartaPdfService pdf;

    public CartaResponsivaService(RentaRepository rentas, CartaResponsivaRepository cartas, CartaPdfService pdf) {
        this.rentas = rentas;
        this.cartas = cartas;
        this.pdf = pdf;
    }

    /** En qué va la carta de la renta; si la renta no existe, responde 404. */
    @Transactional(readOnly = true)
    public CartaResponse consultar(Long rentaId) {
        Renta renta = renta(rentaId);
        return respuesta(renta, cartas.findByRentaId(rentaId).orElse(null));
    }

    /**
     * Genera la carta con los datos actuales de la renta. Si ya existía, la regenera:
     * la nueva versión queda como la única vigente y la carta vuelve a Generada (hay que enviarla de nuevo).
     * Solo para rentas Programadas y cartas sin firmar (409). Si falta un dato obligatorio no genera nada.
     */
    @Transactional
    public CartaResponse generar(Long rentaId) {
        Renta renta = renta(rentaId);
        if (renta.getEstado() != EstadoRenta.PROGRAMADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede generar la carta de una renta Programada.");
        }
        CartaResponsiva carta = cartas.findByRentaId(rentaId).orElse(null);
        if (carta != null && carta.getEstado() == EstadoCarta.FIRMADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La carta ya está firmada; no se puede volver a generar.");
        }

        DatosCarta datos = DatosCarta.de(renta);
        Map<String, String> faltan = datos.faltantes();
        if (!faltan.isEmpty()) {
            throw new DatosFaltantesException(faltan);
        }

        byte[] archivo = pdf.generar(datos);
        String resumen = ResumenCarta.texto(datos.resumen());
        if (carta == null) {
            carta = cartas.save(new CartaResponsiva(renta, resumen, archivo));
        } else {
            carta.regenerar(resumen, archivo);
        }
        return respuesta(renta, carta);
    }

    /** El PDF vigente; 404 si la renta no tiene carta generada. */
    @Transactional(readOnly = true)
    public ArchivoCarta archivo(Long rentaId) {
        renta(rentaId);
        CartaResponsiva carta = cartas.findByRentaId(rentaId)
                .filter(CartaResponsiva::tieneArchivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La renta no tiene una carta generada."));
        return new ArchivoCarta(carta.nombreArchivo(), carta.getPdf());
    }

    private Renta renta(Long rentaId) {
        return rentas.findById(rentaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la renta."));
    }

    private static CartaResponse respuesta(Renta renta, CartaResponsiva carta) {
        boolean firmada = carta != null && carta.getEstado() == EstadoCarta.FIRMADA;
        boolean puedeGenerar = renta.getEstado() == EstadoRenta.PROGRAMADA && !firmada;

        if (carta == null) {
            return new CartaResponse(renta.getId(), renta.getFolio(), renta.getEstado(), EstadoCarta.SIN_GENERAR,
                    null, null, null, renta.getCliente().getCorreo(), false, puedeGenerar, List.of());
        }

        // Solo se compara si se sabe con qué datos se generó (las cartas de prueba no lo guardan)
        List<CambioCarta> cambios = carta.getDatos() == null || firmada
                ? List.of()
                : ResumenCarta.cambios(ResumenCarta.leer(carta.getDatos()), DatosCarta.de(renta).resumen());

        return new CartaResponse(renta.getId(), renta.getFolio(), renta.getEstado(), carta.getEstado(),
                Math.max(carta.getNumeroVersion(), 1), carta.nombreArchivo(), carta.getFechaGeneracion(),
                renta.getCliente().getCorreo(), carta.tieneArchivo(), puedeGenerar, cambios);
    }
}
