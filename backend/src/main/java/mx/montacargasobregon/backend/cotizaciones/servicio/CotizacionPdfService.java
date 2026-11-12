package mx.montacargasobregon.backend.cotizaciones.servicio;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.clientes.modelo.RazonSocial;
import mx.montacargasobregon.backend.comun.DatosContactoSitio;
import mx.montacargasobregon.backend.comun.DatosDireccion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EquipoCotizacion;

/**
 * Genera el PDF de una cotización (HU-18): encabezado con folio y fecha, cliente y razón social,
 * entrega y recolección, tabla de equipos y Subtotal, IVA (16%) y Total.
 * Traslado, descuento, vigencia y condiciones se agregan con HU-35.
 */
@Service
public class CotizacionPdfService {

    /** El PDF listo para enviarse, con el nombre del archivo. */
    public record ArchivoPdf(String nombre, byte[] contenido) {
    }

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES_MX);

    // Colores del prototipo
    private static final Color AZUL = new Color(0x0F3D4C);
    private static final Color MENTA = new Color(0x267C93);
    private static final Color TEXTO = new Color(0x2E2D37);
    private static final Color FONDO_ENCABEZADO = new Color(0xF0F1EA);
    private static final Color DIVISOR = new Color(0xCFD8DB);

    private static final Font MARCA = new Font(Font.HELVETICA, 17, Font.BOLD, AZUL);
    private static final Font TITULO = new Font(Font.HELVETICA, 15, Font.BOLD, AZUL);
    private static final Font ETIQUETA = new Font(Font.HELVETICA, 8.5f, Font.BOLD, MENTA);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL, AZUL);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 10, Font.BOLD, AZUL);
    private static final Font PEQUENA = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXTO);
    private static final Font TOTAL = new Font(Font.HELVETICA, 12.5f, Font.BOLD, AZUL);

    private final CotizacionService cotizaciones;

    public CotizacionPdfService(CotizacionService cotizaciones) {
        this.cotizaciones = cotizaciones;
    }

    /** Busca la cotización y genera su PDF. Sin equipos no hay PDF: responde 409. */
    @Transactional(readOnly = true)
    public ArchivoPdf generar(Long id) {
        Cotizacion cotizacion = cotizaciones.obtener(id);
        if (cotizacion.getEquipos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Agrega al menos un equipo");
        }
        return new ArchivoPdf(cotizacion.getFolio() + ".pdf", generar(cotizacion));
    }

    /** Arma el documento de una cotización ya cargada. */
    public byte[] generar(Cotizacion c) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, 50, 50, 45, 45);
        PdfWriter.getInstance(documento, salida);
        documento.open();

        documento.add(encabezado(c));
        documento.add(clienteYFacturacion(c));
        documento.add(entregaYRecoleccion(c));
        documento.add(equipos(c));
        documento.add(totales(c));

        documento.close();
        return salida.toByteArray();
    }

    // Marca a la izquierda; folio y fecha a la derecha; abajo, una línea azul
    private PdfPTable encabezado(Cotizacion c) {
        PdfPTable tabla = tabla(2, 100);

        PdfPCell marca = celdaEncabezado();
        marca.addElement(new Paragraph("Montacargas Obregón.", MARCA));
        marca.addElement(new Paragraph("Renta de maquinaria · Cd. Obregón, Sonora", PEQUENA));
        tabla.addCell(marca);

        PdfPCell folio = celdaEncabezado();
        folio.addElement(alineado(new Paragraph("COTIZACIÓN", TITULO), Element.ALIGN_RIGHT));
        folio.addElement(alineado(new Paragraph("Folio " + c.getFolio(), NEGRITA), Element.ALIGN_RIGHT));
        folio.addElement(alineado(new Paragraph("Fecha: " + c.getFechaHoraCreacion().format(FECHA), PEQUENA), Element.ALIGN_RIGHT));
        tabla.addCell(folio);

        return tabla;
    }

    private static PdfPCell celdaEncabezado() {
        PdfPCell celda = celda();
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderWidthBottom(2f);
        celda.setBorderColorBottom(AZUL);
        celda.setPaddingBottom(10);
        return celda;
    }

    private PdfPTable clienteYFacturacion(Cotizacion c) {
        Cliente cliente = c.getCliente();
        RazonSocial razon = c.getRazonSocial();

        PdfPTable tabla = tabla(2, 100);
        tabla.setSpacingBefore(14);

        PdfPCell datosCliente = bloque("CLIENTE", nombreCompleto(cliente));
        if (cliente.getEmpresa() != null && !cliente.getEmpresa().isBlank()) {
            datosCliente.addElement(new Paragraph(cliente.getEmpresa(), NORMAL));
        }
        datosCliente.addElement(new Paragraph(telefono(cliente.getTelefono()) + " · " + cliente.getCorreo(), NORMAL));
        tabla.addCell(datosCliente);

        tabla.addCell(bloque("FACTURAR A",
                razon.getNombre(),
                "RFC: " + razon.getRfc(),
                razon.getDireccionFiscal().enUnRenglon()));
        return tabla;
    }

    private PdfPTable entregaYRecoleccion(Cotizacion c) {
        PdfPTable tabla = tabla(1, 100);
        tabla.setSpacingBefore(12);
        DatosDireccion obra = c.getDireccionObra();
        DatosContactoSitio contacto = c.getContactoObra();

        tabla.addCell(bloque("ENTREGA Y RECOLECCIÓN",
                "Entrega: " + fechaHora(c.getFechaHoraEntrega()),
                "Recolección: " + fechaHora(c.getFechaHoraRecoleccion()),
                "Lugar: " + obra.enUnRenglon(),
                "Contacto en la obra: " + contacto.getNombre() + " · " + telefono(contacto.getTelefono())));
        return tabla;
    }

    private PdfPTable equipos(Cotizacion c) {
        PdfPTable tabla = tabla(5, 100);
        tabla.setWidths(new float[] { 1.2f, 3.4f, 0.8f, 1.6f, 1.6f });
        tabla.setSpacingBefore(16);
        tabla.setHeaderRows(1);

        String[] columnas = { "Unidad", "Modelo", "Días", "Tarifa / día", "Importe" };
        for (int i = 0; i < columnas.length; i++) {
            PdfPCell encabezado = celdaTabla(columnas[i], NEGRITA, i >= 2);
            encabezado.setBackgroundColor(FONDO_ENCABEZADO);
            tabla.addCell(encabezado);
        }

        String dias = String.valueOf(c.getDias());
        for (EquipoCotizacion p : c.getEquipos()) {
            tabla.addCell(celdaTabla(p.getEquipo().getNumeroUnidad(), NORMAL, false));
            tabla.addCell(celdaTabla(p.getEquipo().getModelo().getNombre(), NORMAL, false));
            tabla.addCell(celdaTabla(dias, NORMAL, true));
            tabla.addCell(celdaTabla(moneda(p.getTarifa()), NORMAL, true));
            tabla.addCell(celdaTabla(moneda(p.getImporte()), NORMAL, true));
        }
        return tabla;
    }

    // Subtotal, IVA y Total alineados a la derecha
    private PdfPTable totales(Cotizacion c) {
        PdfPTable tabla = tabla(2, 45);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.setSpacingBefore(10);

        filaTotal(tabla, "Subtotal", moneda(c.getSubtotal()), NORMAL, NEGRITA);
        filaTotal(tabla, "IVA (16%)", moneda(c.getIva()), NORMAL, NEGRITA);
        filaTotal(tabla, "Total", moneda(c.getTotal()), TOTAL, TOTAL);
        return tabla;
    }

    private void filaTotal(PdfPTable tabla, String concepto, String monto, Font fuenteConcepto, Font fuenteMonto) {
        tabla.addCell(alineado(celda(new Phrase(concepto, fuenteConcepto)), Element.ALIGN_RIGHT));
        tabla.addCell(alineado(celda(new Phrase(monto, fuenteMonto)), Element.ALIGN_RIGHT));
    }

    // ===== Piezas =====

    private static PdfPTable tabla(int columnas, float anchoPorcentaje) {
        PdfPTable tabla = new PdfPTable(columnas);
        tabla.setWidthPercentage(anchoPorcentaje);
        return tabla;
    }

    private static PdfPCell celda() {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(0);
        return celda;
    }

    private static PdfPCell celda(Phrase contenido) {
        PdfPCell celda = new PdfPCell(contenido);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPaddingTop(2);
        celda.setPaddingBottom(2);
        return celda;
    }

    // Etiqueta pequeña en color menta y sus renglones debajo
    private static PdfPCell bloque(String etiqueta, String... renglones) {
        PdfPCell celda = celda();
        celda.setPaddingRight(12);
        celda.addElement(new Paragraph(etiqueta, ETIQUETA));
        for (String renglon : renglones) {
            celda.addElement(new Paragraph(renglon, NORMAL));
        }
        return celda;
    }

    private static PdfPCell celdaTabla(String texto, Font fuente, boolean derecha) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(DIVISOR);
        celda.setPadding(6);
        celda.setHorizontalAlignment(derecha ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        return celda;
    }

    private static Paragraph alineado(Paragraph parrafo, int alineacion) {
        parrafo.setAlignment(alineacion);
        return parrafo;
    }

    private static PdfPCell alineado(PdfPCell celda, int alineacion) {
        celda.setHorizontalAlignment(alineacion);
        return celda;
    }

    // ===== Formatos =====

    // 9860 → "$9,860.00"
    static String moneda(BigDecimal monto) {
        return NumberFormat.getCurrencyInstance(ES_MX).format(monto);
    }

    // → "lunes 12 de octubre de 2026, 09:00 a. m."
    static String fechaHora(LocalDateTime fecha) {
        int hora = fecha.getHour() % 12 == 0 ? 12 : fecha.getHour() % 12;
        String sufijo = fecha.getHour() < 12 ? "a. m." : "p. m.";
        return "%s, %02d:%02d %s".formatted(fecha.format(FECHA_LARGA), hora, fecha.getMinute(), sufijo);
    }

    // "6872104455" → "687 210 4455"
    static String telefono(String telefono) {
        return telefono.replaceFirst("^(\\d{3})(\\d{3})(\\d{4})$", "$1 $2 $3");
    }

    private static String nombreCompleto(Cliente c) {
        String nombre = c.getNombre() + " " + c.getApellidoPaterno();
        return c.getApellidoMaterno() == null ? nombre : nombre + " " + c.getApellidoMaterno();
    }
}
