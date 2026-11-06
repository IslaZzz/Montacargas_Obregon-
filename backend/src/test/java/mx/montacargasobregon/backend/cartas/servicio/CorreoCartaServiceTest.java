package mx.montacargasobregon.backend.cartas.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

class CorreoCartaServiceTest {

    private static final byte[] PDF = "%PDF-1.4 prueba".getBytes(StandardCharsets.US_ASCII);

    private JavaMailSender servidor;
    private ObjectProvider<JavaMailSender> servidores;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        servidor = mock(JavaMailSender.class);
        when(servidor.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));
        servidores = mock(ObjectProvider.class);
        when(servidores.getIfAvailable()).thenReturn(servidor);
    }

    @Test
    void enviaAlClienteConLaCartaAdjunta() throws Exception {
        CorreoCartaService correo = new CorreoCartaService(servidores, "smtp.ejemplo.com", "rentas@ejemplo.com");

        correo.enviar("juan.perez@ejemplo.com", "R-1042", "Hola Juan.", "Carta_responsiva_R-1042_v1.pdf", PDF);

        ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
        verify(servidor).send(enviado.capture());
        MimeMessage mensaje = enviado.getValue();
        assertEquals("juan.perez@ejemplo.com", mensaje.getAllRecipients()[0].toString());
        assertEquals("rentas@ejemplo.com", mensaje.getFrom()[0].toString());
        assertEquals("Carta responsiva de la renta R-1042 · Montacargas Obregón", mensaje.getSubject());

        ByteArrayOutputStream crudo = new ByteArrayOutputStream();
        mensaje.writeTo(crudo);
        assertTrue(crudo.toString(StandardCharsets.US_ASCII).contains("Carta_responsiva_R-1042_v1.pdf"));
    }

    @Test
    void sinRemitenteSeUsaLaCuentaDelCorreo() throws Exception {
        CorreoCartaService correo = new CorreoCartaService(servidores, "smtp.gmail.com", "", "cartas@gmail.com");

        correo.enviar("juan.perez@ejemplo.com", "R-1042", "Hola", "carta.pdf", PDF);

        ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
        verify(servidor).send(enviado.capture());
        assertEquals("cartas@gmail.com", enviado.getValue().getFrom()[0].toString());
    }

    @Test
    void siElRemitenteEsSoloUnNombreVaJuntoALaCuenta() throws Exception {
        CorreoCartaService correo = new CorreoCartaService(servidores, "smtp.gmail.com", "Montacargas Obregón",
                "cartas@gmail.com");

        correo.enviar("juan.perez@ejemplo.com", "R-1042", "Hola", "carta.pdf", PDF);

        ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
        verify(servidor).send(enviado.capture());
        InternetAddress de = (InternetAddress) enviado.getValue().getFrom()[0];
        assertEquals("cartas@gmail.com", de.getAddress());
        assertEquals("Montacargas Obregón", de.getPersonal());
    }

    @Test
    void elCuerpoLlevaElMensajeYLasInstrucciones() {
        assertEquals("Hola Juan.\n\n" + CorreoCartaService.INSTRUCCIONES + "\n\nMontacargas Obregón",
                CorreoCartaService.cuerpo("  Hola Juan.  "));
    }

    @Test
    void sinConfigurarElCorreoResponde503() {
        CorreoCartaService correo = new CorreoCartaService(servidores, "", "");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> correo.enviar("juan.perez@ejemplo.com", "R-1042", "Hola", "carta.pdf", PDF));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verify(servidor, never()).send(any(MimeMessage.class));
    }

    @Test
    void siFallaElServidorResponde502() {
        doThrow(new MailSendException("Connection refused")).when(servidor).send(any(MimeMessage.class));
        CorreoCartaService correo = new CorreoCartaService(servidores, "smtp.ejemplo.com", "");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> correo.enviar("juan.perez@ejemplo.com", "R-1042", "Hola", "carta.pdf", PDF));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        assertEquals("No hubo conexión con el servicio de correo.", error.getReason());
    }
}
