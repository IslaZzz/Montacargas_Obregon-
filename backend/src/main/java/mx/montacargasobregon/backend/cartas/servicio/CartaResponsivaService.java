package mx.montacargasobregon.backend.cartas.servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.cartas.dto.CambioCarta;
import mx.montacargasobregon.backend.cartas.dto.CartaResponse;
import mx.montacargasobregon.backend.cartas.dto.FirmaCarta;
import mx.montacargasobregon.backend.cartas.modelo.CartaResponsiva;
import mx.montacargasobregon.backend.cartas.modelo.EstadoCarta;
import mx.montacargasobregon.backend.cartas.modelo.TipoFirma;
import mx.montacargasobregon.backend.cartas.repositorio.CartaResponsivaRepository;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.rentas.repositorio.RentaRepository;

/**
 * Carta responsiva de una renta: consultarla, generarla o regenerarla, obtener su PDF
 * y enviarla por correo al cliente (OHU-04), y registrar que el cliente la firmó (OHU-05).
 */
@Service
public class CartaResponsivaService {

    /** Un archivo de la carta (el PDF generado o la carta firmada), con su nombre y tipo. */
    public record ArchivoCarta(String nombre, String tipo, byte[] contenido) {
    }

    // TODO: quitar cuando exista el inicio de sesión y tomar al usuario de la sesión.
    // Es el mismo usuario fijo que muestra el menú lateral (AppLayout).
    static final String USUARIO_PROVISIONAL = "Juan Pérez";
    static final String ROL_PROVISIONAL = "Auxiliar de oficina";

    /** Lo más que puede pesar la carta firmada (igual que spring.servlet.multipart.max-file-size). */
    static final long MAXIMO_ARCHIVO_FIRMADO = 10L * 1024 * 1024;

    // La firma se registra antes de entregar o, si es física, al entregar el equipo
    private static final Set<EstadoRenta> RENTAS_QUE_SE_FIRMAN =
            Set.of(EstadoRenta.PROGRAMADA, EstadoRenta.EN_CURSO);

    private final RentaRepository rentas;
    private final CartaResponsivaRepository cartas;
    private final CartaPdfService pdf;
    private final CorreoCartaService correo;

    public CartaResponsivaService(RentaRepository rentas, CartaResponsivaRepository cartas, CartaPdfService pdf,
                                  CorreoCartaService correo) {
        this.rentas = rentas;
        this.cartas = cartas;
        this.pdf = pdf;
        this.correo = correo;
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

    /**
     * Envía la versión vigente al correo registrado del cliente y la carta pasa a Enviada.
     * Si el envío falla responde 502 o 503 y la carta se queda como estaba (Generada).
     * Se puede reenviar mientras la carta no esté firmada.
     *
     * @param mensaje el texto del correo; si viene vacío se usa el mensaje sugerido
     */
    @Transactional
    public CartaResponse enviar(Long rentaId, String mensaje) {
        Renta renta = renta(rentaId);
        CartaResponsiva carta = cartas.findByRentaId(rentaId)
                .filter(CartaResponsiva::tieneArchivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Primero hay que generar la carta."));
        if (carta.getEstado() == EstadoCarta.FIRMADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La carta ya está firmada.");
        }
        if (renta.getEstado() != EstadoRenta.PROGRAMADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede enviar la carta de una renta Programada.");
        }
        String para = renta.getCliente().getCorreo();
        if (para == null || para.isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El cliente no tiene correo registrado.");
        }

        String texto = mensaje == null || mensaje.isBlank()
                ? mensajeSugerido(renta, DatosCarta.de(renta))
                : mensaje;
        // Si falla lanza la excepción antes de marcarla como Enviada
        correo.enviar(para.strip(), renta.getFolio(), texto, carta.nombreArchivo(), carta.getPdf());
        carta.registrarEnvio(para.strip());
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
        return new ArchivoCarta(carta.nombreArchivo(), "application/pdf", carta.getPdf());
    }

    /**
     * Registra que el cliente firmó la carta y guarda la carta firmada; la carta pasa a Firmada.
     * Solo si la carta ya se generó y no está firmada (409). La carta firmada es obligatoria:
     * en la firma electrónica, el PDF que regresó el cliente; en la física, una foto o un escaneo
     * (PDF, JPG o PNG). Si falta o no es válida responde 400 y no registra nada.
     */
    @Transactional
    public CartaResponse registrarFirma(Long rentaId, TipoFirma tipo, LocalDateTime fecha, String firmante,
                                        byte[] archivo) {
        Renta renta = renta(rentaId);
        CartaResponsiva carta = cartas.findByRentaId(rentaId)
                .filter(CartaResponsiva::tieneArchivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede registrar la firma de una carta que no se ha generado."));
        if (carta.getEstado() == EstadoCarta.FIRMADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La carta ya está firmada.");
        }
        if (!RENTAS_QUE_SE_FIRMAN.contains(renta.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se registra la firma de una renta Programada o En curso.");
        }

        if (tipo == null) {
            throw invalido("Elige si la firma fue electrónica o física.");
        }
        if (fecha == null) {
            throw invalido("Indica la fecha y hora de la firma.");
        }
        // Unos minutos de margen por si el reloj del equipo va adelantado
        if (fecha.isAfter(LocalDateTime.now(CartaResponsiva.ZONA_HORARIA).plusMinutes(5))) {
            throw invalido("La fecha de la firma no puede ser posterior a hoy.");
        }
        if (firmante == null || firmante.isBlank()) {
            throw invalido("Escribe el nombre de quien firma.");
        }
        if (firmante.strip().length() > 150) {
            throw invalido("El nombre de quien firma no puede pasar de 150 caracteres.");
        }
        String tipoArchivo = validarArchivo(tipo, archivo);

        carta.registrarFirma(tipo, fecha, firmante.strip(), USUARIO_PROVISIONAL, ROL_PROVISIONAL,
                archivo, tipoArchivo);
        return respuesta(renta, carta);
    }

    /** La carta firmada; 404 si la carta no está firmada. */
    @Transactional(readOnly = true)
    public ArchivoCarta archivoFirmado(Long rentaId) {
        renta(rentaId);
        CartaResponsiva carta = cartas.findByRentaId(rentaId)
                .filter(CartaResponsiva::tieneArchivoFirmado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La carta no tiene un archivo firmado."));
        return new ArchivoCarta(carta.nombreArchivoFirmado(), carta.getTipoArchivoFirmado(),
                carta.getArchivoFirmado());
    }

    // Devuelve el tipo del archivo según su contenido, no según su nombre
    static String validarArchivo(TipoFirma tipo, byte[] archivo) {
        if (archivo == null || archivo.length == 0) {
            throw invalido(tipo == TipoFirma.ELECTRONICA
                    ? "Adjunta la carta firmada que regresó el cliente (PDF)."
                    : "Adjunta la carta firmada para registrar la firma.");
        }
        if (archivo.length > MAXIMO_ARCHIVO_FIRMADO) {
            throw invalido("El archivo no puede pesar más de 10 MB.");
        }
        String detectado = tipoDeArchivo(archivo);
        if (detectado == null) {
            throw invalido("El archivo debe ser PDF, JPG o PNG.");
        }
        if (tipo == TipoFirma.ELECTRONICA && !detectado.equals("application/pdf")) {
            throw invalido("La carta firmada electrónicamente debe ser un PDF.");
        }
        return detectado;
    }

    // Por los primeros bytes: %PDF, FF D8 FF (JPG) o 89 50 4E 47 (PNG)
    static String tipoDeArchivo(byte[] a) {
        if (a.length >= 4 && a[0] == '%' && a[1] == 'P' && a[2] == 'D' && a[3] == 'F') {
            return "application/pdf";
        }
        if (a.length >= 3 && (a[0] & 0xFF) == 0xFF && (a[1] & 0xFF) == 0xD8 && (a[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (a.length >= 4 && (a[0] & 0xFF) == 0x89 && a[1] == 'P' && a[2] == 'N' && a[3] == 'G') {
            return "image/png";
        }
        return null;
    }

    private static ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }

    private Renta renta(Long rentaId) {
        return rentas.findById(rentaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la renta."));
    }

    private static CartaResponse respuesta(Renta renta, CartaResponsiva carta) {
        boolean firmada = carta != null && carta.getEstado() == EstadoCarta.FIRMADA;
        boolean puedeGenerar = renta.getEstado() == EstadoRenta.PROGRAMADA && !firmada;
        DatosCarta datos = DatosCarta.de(renta);
        String sugerido = mensajeSugerido(renta, datos);
        String firmanteSugerido = firmanteSugerido(renta, datos);

        if (carta == null) {
            return new CartaResponse(renta.getId(), renta.getFolio(), renta.getEstado(), EstadoCarta.SIN_GENERAR,
                    null, null, null, renta.getCliente().getCorreo(), false, puedeGenerar, false,
                    null, null, sugerido, false, firmanteSugerido, null, List.of());
        }

        boolean puedeFirmar = carta.tieneArchivo() && !firmada && RENTAS_QUE_SE_FIRMAN.contains(renta.getEstado());
        FirmaCarta firma = firmada && carta.getTipoFirma() != null
                ? new FirmaCarta(carta.getTipoFirma(), carta.getFechaFirma(), carta.getFirmante(),
                        carta.getFirmaRegistradaPor(), carta.getFirmaRegistradaRol(), carta.getFirmaRegistradaEn(),
                        carta.tieneArchivoFirmado() ? carta.nombreArchivoFirmado() : null,
                        carta.getTipoArchivoFirmado())
                : null;

        // Solo se compara si se sabe con qué datos se generó (las cartas de prueba no lo guardan)
        List<CambioCarta> cambios = carta.getDatos() == null || firmada
                ? List.of()
                : ResumenCarta.cambios(ResumenCarta.leer(carta.getDatos()), datos.resumen());

        return new CartaResponse(renta.getId(), renta.getFolio(), renta.getEstado(), carta.getEstado(),
                Math.max(carta.getNumeroVersion(), 1), carta.nombreArchivo(), carta.getFechaGeneracion(),
                renta.getCliente().getCorreo(), carta.tieneArchivo(), puedeGenerar,
                puedeGenerar && carta.tieneArchivo(), carta.getFechaEnvio(), carta.getCorreoEnvio(),
                sugerido, puedeFirmar, firmanteSugerido, firma, cambios);
    }

    // El contacto en la obra; si no hay, la persona del cliente
    private static String firmanteSugerido(Renta renta, DatosCarta datos) {
        if (datos.contactoObra() != null && !datos.contactoObra().isBlank()) {
            return datos.contactoObra().strip();
        }
        var c = renta.getCliente();
        return String.join(" ", Stream.of(c.getNombre(), c.getApellidoPaterno())
                .filter(p -> p != null && !p.isBlank())
                .toList());
    }

    /**
     * Ej. "Hola Juan, te compartimos la carta responsiva de la renta R-1042, con el equipo
     * Toyota 8FGU25 (MC-09), para tu firma electrónica."
     */
    static String mensajeSugerido(Renta renta, DatosCarta datos) {
        String nombre = renta.getCliente().getNombre();
        String saludo = nombre == null || nombre.isBlank() ? "Hola" : "Hola " + nombre.strip();

        List<String> equipos = datos.equipos().stream()
                .map(e -> "%s %s (%s)".formatted(e.marca(), e.modelo(), e.numeroUnidad()))
                .toList();
        String conEquipos = switch (equipos.size()) {
            case 0 -> "";
            case 1 -> ", con el equipo " + equipos.get(0) + ",";
            default -> ", con los equipos " + String.join(", ", equipos.subList(0, equipos.size() - 1))
                    + " y " + equipos.get(equipos.size() - 1) + ",";
        };
        return "%s, te compartimos la carta responsiva de la renta %s%s para tu firma electrónica."
                .formatted(saludo, renta.getFolio(), conEquipos);
    }
}
