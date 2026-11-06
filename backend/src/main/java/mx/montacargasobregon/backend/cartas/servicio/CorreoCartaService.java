package mx.montacargasobregon.backend.cartas.servicio;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Envía la carta responsiva por correo al cliente, con el PDF adjunto.
 * La cuenta se configura en .env (MAIL_HOST, MAIL_USER, MAIL_PASSWORD...).
 * Si no está configurada o el servidor de correo falla, responde con error y no se envía nada.
 */
@Service
public class CorreoCartaService {

    private static final Logger log = LoggerFactory.getLogger(CorreoCartaService.class);

    /** Lo que se le pide al cliente al final del correo. */
    static final String INSTRUCCIONES = """
            Adjuntamos la carta responsiva de tu renta. Revísala, fírmala y respóndenos este correo \
            con la carta firmada.""";

    private final ObjectProvider<JavaMailSender> servidores;
    private final String host;
    private final String remitente;
    private final String usuario;

    public CorreoCartaService(ObjectProvider<JavaMailSender> servidores, String host, String remitente) {
        this(servidores, host, remitente, "");
    }

    /**
     * @param remitente MAIL_FROM: un correo ("Nombre <correo>") o solo el nombre que verá el cliente
     * @param usuario   MAIL_USER: la cuenta con que se entra al correo; es el remitente si MAIL_FROM no trae correo
     */
    @Autowired
    public CorreoCartaService(ObjectProvider<JavaMailSender> servidores,
                              @Value("${spring.mail.host:}") String host,
                              @Value("${montacargas.correo.remitente:}") String remitente,
                              @Value("${spring.mail.username:}") String usuario) {
        this.servidores = servidores;
        this.host = host;
        this.remitente = remitente == null ? "" : remitente.strip();
        this.usuario = usuario == null ? "" : usuario.strip();
    }

    /**
     * Envía el correo. Si no se puede, lanza 503 (correo sin configurar) o 502 (falló el servidor de correo).
     *
     * @param mensaje lo que escribió el usuario; se agregan las instrucciones de firma al final
     */
    public void enviar(String para, String folio, String mensaje, String nombreArchivo, byte[] pdf) {
        JavaMailSender servidor = servidores.getIfAvailable();
        if (servidor == null || host == null || host.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de correo no está configurado.");
        }

        try {
            MimeMessage correo = servidor.createMimeMessage();
            MimeMessageHelper partes = new MimeMessageHelper(correo, true, "UTF-8");
            ponerRemitente(partes);
            partes.setTo(para);
            partes.setSubject("Carta responsiva de la renta %s · Montacargas Obregón".formatted(folio));
            partes.setText(cuerpo(mensaje));
            partes.addAttachment(nombreArchivo, new ByteArrayResource(pdf), "application/pdf");
            servidor.send(correo);
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.warn("No se pudo enviar la carta {} a {}: {} {}", folio, para,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No hubo conexión con el servicio de correo.");
        }
    }

    // "Nombre <correo>" o "correo" se usa tal cual; "Nombre" solo se pone junto a la cuenta (MAIL_USER)
    private void ponerRemitente(MimeMessageHelper partes) throws MessagingException, UnsupportedEncodingException {
        if (remitente.contains("@")) {
            partes.setFrom(remitente);
        } else if (!usuario.isEmpty()) {
            if (remitente.isEmpty()) {
                partes.setFrom(usuario);
            } else {
                partes.setFrom(usuario, remitente);
            }
        }
    }

    static String cuerpo(String mensaje) {
        String texto = mensaje == null ? "" : mensaje.strip();
        return (texto.isEmpty() ? "" : texto + "\n\n") + INSTRUCCIONES + "\n\nMontacargas Obregón";
    }
}
