package mx.montacargasobregon.backend.cotizaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.clientes.modelo.DireccionFiscal;
import mx.montacargasobregon.backend.clientes.modelo.RazonSocial;
import mx.montacargasobregon.backend.comun.DatosContactoSitio;
import mx.montacargasobregon.backend.comun.DatosDireccion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EquipoCotizacion;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;

/** El PDF trae los datos y los mismos totales que el resumen (TC-18-02 y TC-18-03). */
class CotizacionPdfServiceTest {

    // No usa la base de datos: genera el PDF de una cotización armada en memoria
    private final CotizacionPdfService servicio = new CotizacionPdfService(null);

    @Test
    void pdfTraeDatosYTotales() throws Exception {
        String texto = textoDelPdf(cotizacionDePrueba());

        assertContiene(texto, "COT-0001");
        assertContiene(texto, "María López Ruiz");
        assertContiene(texto, "Obras del Mayo Maquinaria S.A. de C.V.");
        assertContiene(texto, "RFC: OMM180920QW5");
        assertContiene(texto, "martes 10 de noviembre de 2026, 08:00 a. m.");
        assertContiene(texto, "sábado 14 de noviembre de 2026, 06:00 p. m.");
        assertContiene(texto, "Lugar: Calle 200 1500, Col. Centro, Cd. Obregón, Sonora, C.P. 85000");
        assertContiene(texto, "Referencias: Portón azul. Preguntar por el Ing. Soto.");
        assertContiene(texto, "Contacto en la obra: Ing. Raúl Soto · 644 210 4455");
        assertContiene(texto, "MC-07");
        assertContiene(texto, "GR-02");
        assertContiene(texto, "$2,500.00");
        assertContiene(texto, "$6,000.00");
        // Los mismos totales que el resumen del paso 4
        assertContiene(texto, "Subtotal");
        assertContiene(texto, "$8,500.00");
        assertContiene(texto, "IVA (16%)");
        assertContiene(texto, "$1,360.00");
        assertContiene(texto, "$9,860.00");
    }

    @Test
    void formatos() {
        assertEquals("$1,432.09", CotizacionPdfService.moneda(new BigDecimal("1432.09")));
        assertEquals("644 123 4567", CotizacionPdfService.telefono("6441234567"));
        // La dirección fiscal todavía no lleva estado: no se pone
        assertEquals("Calle Jalisco 312, Col. Centro, Navojoa, C.P. 85800", CotizacionPdfService.direccion(
                new DireccionFiscal("Calle Jalisco", "312", "Centro", "Navojoa", "85800")));
        assertEquals("jueves 5 de noviembre de 2026, 12:30 p. m.",
                CotizacionPdfService.fechaHora(LocalDateTime.of(2026, 11, 5, 12, 30)));
    }

    // MC-07 ($500/día) y GR-02 ($1,200/día) por 5 días, como en TC-18-02
    private Cotizacion cotizacionDePrueba() {
        Cliente cliente = new Cliente("María", "López", "Ruiz", "Obras del Mayo", "6449876543",
                "compras@obrasdelmayo.mx", LocalDate.of(2026, 11, 1));
        RazonSocial razon = new RazonSocial("Obras del Mayo Maquinaria S.A. de C.V.", "OMM180920QW5",
                new DireccionFiscal("Blvd. Rodolfo Elías Calles", "845", "Centro", "Cd. Obregón", "85000"));
        cliente.agregarRazonSocial(razon);

        Categoria montacargas = new Categoria("Montacargas");
        Categoria grua = new Categoria("Grúa");
        Equipo mc07 = new Equipo("MC-07", "SER-0007", 2022, new Modelo("Montacargas eléctrico 3 t", "Toyota",
                "TOY-3T", null, new BigDecimal("500.00"), montacargas));
        Equipo gr02 = new Equipo("GR-02", "SER-0102", 2021, new Modelo("Grúa articulada 12 t", "Hiab",
                "HIA-12T", null, new BigDecimal("1200.00"), grua));

        Cotizacion cotizacion = new Cotizacion("COT-0001", cliente, razon,
                LocalDateTime.of(2026, 11, 10, 8, 0), LocalDateTime.of(2026, 11, 14, 18, 0),
                new DatosDireccion("Calle 200", "1500", "Centro", "Cd. Obregón", "Sonora", "85000",
                        "Portón azul. Preguntar por el Ing. Soto."),
                new DatosContactoSitio("Ing. Raúl Soto", "6442104455"),
                new BigDecimal("8500.00"), new BigDecimal("1360.00"), new BigDecimal("9860.00"));
        cotizacion.agregarEquipo(new EquipoCotizacion(mc07, new BigDecimal("500.00"), new BigDecimal("2500.00")));
        cotizacion.agregarEquipo(new EquipoCotizacion(gr02, new BigDecimal("1200.00"), new BigDecimal("6000.00")));
        return cotizacion;
    }

    private String textoDelPdf(Cotizacion cotizacion) throws Exception {
        PdfReader lector = new PdfReader(servicio.generar(cotizacion));
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
