package mx.montacargasobregon.backend.contratos.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import mx.montacargasobregon.backend.contratos.ContratoDePrueba;
import mx.montacargasobregon.backend.contratos.modelo.Contrato;

/** HU-23: el PDF del contrato trae los datos de la cotización, el anticipo y la política de cancelación. */
class ContratoPdfServiceTest {

    // No usa la base de datos: genera el PDF de un contrato armado en memoria
    private final ContratoPdfService servicio = new ContratoPdfService(null);
    private final Contrato contrato = new Contrato("CON-0087", ContratoDePrueba.cotizacion(),
            LocalDateTime.of(2026, 10, 9, 11, 30));

    @Test
    void pdfTraeLosDatosDelContrato() throws Exception {
        String texto = textoDelPdf();

        assertContiene(texto, "CONTRATO");
        assertContiene(texto, "Folio CON-0087");
        assertContiene(texto, "Fecha: 09/10/2026");
        // Cliente y razón social
        assertContiene(texto, "Ramón Gutiérrez Soto");
        assertContiene(texto, "Constructora del Valle de Guasave S.A. de C.V.");
        assertContiene(texto, "RFC: CVG150312KD4");
        // Fechas y horas, lugar y contacto
        assertContiene(texto, "lunes 12 de octubre de 2026, 09:00 a. m.");
        assertContiene(texto, "viernes 16 de octubre de 2026, 05:00 p. m.");
        assertContiene(texto, "Lugar: Carretera Guasave-Los Mochis km 4.5");
        assertContiene(texto, "Contacto en la obra: Ing. Raúl Soto · 687 210 4455");
        // Equipos y precio
        assertContiene(texto, "MC-07");
        assertContiene(texto, "PT-05");
        assertContiene(texto, "$9,448.20");
    }

    @Test
    void pdfTraeElAnticipoYLaPolitica() throws Exception {
        String texto = textoDelPdf().replaceAll("\\s+", " ");

        assertContiene(texto, "Anticipo 30%: $2,834.46. Fecha límite para pagarlo: 12/10/2026.");
        assertContiene(texto, "POLÍTICA DE CANCELACIÓN Y PENALIZACIONES");
        assertContiene(texto, "Cancelación con 3 días o más antes de la entrega: se devuelve el anticipo completo.");
        assertContiene(texto, "Cancelación con menos de 3 días: se pierde el anticipo como penalización.");
        assertContiene(texto, "El cliente firmará una carta responsiva al recibir el equipo.");
    }

    @Test
    void textoDelAnticipo() {
        assertEquals("Anticipo 30%: $2,834.46. Fecha límite para pagarlo: 12/10/2026. "
                + "Con el anticipo, el contrato pasa a Contratado.", ContratoPdfService.textoAnticipo(contrato));
    }

    private String textoDelPdf() throws Exception {
        PdfReader lector = new PdfReader(servicio.generar(contrato));
        try {
            return new PdfTextExtractor(lector).getTextFromPage(1);
        } finally {
            lector.close();
        }
    }

    private static void assertContiene(String texto, String esperado) {
        assertTrue(texto.contains(esperado), () -> "El PDF no trae «" + esperado + "». Texto:\n" + texto);
    }
}
