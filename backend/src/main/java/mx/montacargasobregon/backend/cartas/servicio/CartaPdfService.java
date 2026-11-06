package mx.montacargasobregon.backend.cartas.servicio;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

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
import org.springframework.stereotype.Service;

import mx.montacargasobregon.backend.cartas.servicio.DatosCarta.EquipoCarta;

/**
 * Arma el PDF de la carta responsiva con el formato del prototipo (Figma OHU-04, "Vista previa de la carta"):
 * datos de entrega y cliente, equipos y periodo, inventario para llenar al entregar, nota y firmas.
 * Los textos fijos son los del prototipo; si la empresa tiene su propio formato, se cambian aquí.
 */
@Service
public class CartaPdfService {

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");

    // Lo que se revisa al entregar cada equipo; se llena a mano o al registrar la entrega
    static final List<String> INVENTARIO = List.of("HORÓMETRO", "TORRETA", "EXTINTOR", "ESPEJO",
            "FOCOS DELANTEROS", "FOCOS TRASEROS", "TANQUE DE GAS", "ALARMA DE REVERSA", "DOS CUCHILLAS");

    static final String NOTA_ENTREGA = "Nota: No trasladar el montacargas a otra dirección.";
    static final String NOTA_IMPORTANTE = "LOS DAÑOS OCASIONADOS POR NEGLIGENCIA DERIVADOS DEL TRABAJO PARA EL "
            + "CUAL SE RENTÓ EL MONTACARGAS, SERÁN TOTALMENTE CUBIERTOS POR CUENTA DEL CLIENTE EL CUAL FIRMA DE "
            + "RECIBIDO EN BUENAS CONDICIONES DE FUNCIONAMIENTO.";

    // Mismos colores que el PDF de cotizaciones
    private static final Color AZUL = new Color(0x0F3D4C);
    private static final Color TEXTO = new Color(0x2E2D37);
    private static final Color FONDO_ETIQUETA = new Color(0xF0F1EA);
    private static final Color BORDE = new Color(0x9AA9AE);

    private static final Font TITULO = new Font(Font.HELVETICA, 16, Font.BOLD, AZUL);
    private static final Font MARCA = new Font(Font.HELVETICA, 10, Font.BOLD, AZUL);
    private static final Font PEQUENA = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, TEXTO);
    private static final Font ETIQUETA = new Font(Font.HELVETICA, 8, Font.BOLD, TEXTO);
    private static final Font DATO = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, TEXTO);
    private static final Font DATO_NEGRITA = new Font(Font.HELVETICA, 8.5f, Font.BOLD, TEXTO);
    private static final Font FOLIO = new Font(Font.HELVETICA, 10, Font.BOLD, AZUL);
    private static final Font SECCION = new Font(Font.HELVETICA, 9, Font.BOLD, AZUL);

    /** Arma el documento. Quien llama revisa antes que no falten datos. */
    public byte[] generar(DatosCarta d) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, 50, 50, 40, 40);
        PdfWriter.getInstance(documento, salida);
        documento.open();

        documento.add(encabezado());
        documento.add(folio(d));
        documento.add(entregaYCliente(d));
        documento.add(equipos(d));
        documento.add(periodo(d));
        documento.add(seccion("INVENTARIO"));
        documento.add(inventario(d));
        documento.add(seccion("NOTA IMPORTANTE:"));
        documento.add(new Paragraph(NOTA_IMPORTANTE, PEQUENA));
        documento.add(firmas());

        documento.close();
        return salida.toByteArray();
    }

    // Título y empresa, centrados
    private PdfPTable encabezado() {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.addElement(centrado(new Paragraph("RESPONSIVA DE MONTACARGAS", TITULO)));
        celda.addElement(centrado(new Paragraph("Montacargas Obregón", MARCA)));
        celda.addElement(centrado(new Paragraph("Renta de maquinaria · Cd. Obregón, Sonora", PEQUENA)));
        tabla.addCell(celda);
        return tabla;
    }

    private static Paragraph centrado(Paragraph parrafo) {
        parrafo.setAlignment(Element.ALIGN_CENTER);
        return parrafo;
    }

    // Folio en un recuadro, a la derecha
    private PdfPTable folio(DatosCarta d) {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(40);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.setWidths(new float[] { 1f, 1.4f });
        tabla.setSpacingBefore(10);

        PdfPCell etiqueta = new PdfPCell(new Phrase("FOLIO:", FOLIO));
        etiqueta.setBorder(Rectangle.NO_BORDER);
        etiqueta.setHorizontalAlignment(Element.ALIGN_RIGHT);
        etiqueta.setVerticalAlignment(Element.ALIGN_MIDDLE);
        etiqueta.setPaddingRight(8);
        tabla.addCell(etiqueta);

        PdfPCell valor = new PdfPCell(new Phrase(d.folio(), FOLIO));
        valor.setBorderColor(AZUL);
        valor.setHorizontalAlignment(Element.ALIGN_CENTER);
        valor.setPadding(4);
        tabla.addCell(valor);
        return tabla;
    }

    private PdfPTable entregaYCliente(DatosCarta d) {
        PdfPTable tabla = tablaDatos(2, new float[] { 1.1f, 3.5f });
        fila(tabla, "FECHA ENTREGA:", d.fechaEntrega().format(DatosCarta.FECHA_LARGA));
        fila(tabla, "CLIENTE:", new Phrase(d.cliente().toUpperCase(ES_MX), DATO_NEGRITA));
        fila(tabla, "DIRECCIÓN FISCAL:", d.direccionFiscal() == null ? "No registrada" : d.direccionFiscal());

        PdfPCell direccion = celda(null);
        direccion.addElement(new Paragraph(d.direccionEntrega(), DATO));
        direccion.addElement(new Paragraph(NOTA_ENTREGA, PEQUENA));
        tabla.addCell(etiqueta("DIRECCIÓN ENTREGA:"));
        tabla.addCell(direccion);

        fila(tabla, "TEL. Y CONTACTO:", telefono(d.telefonoContacto()) + "  " + d.contactoObra().toUpperCase(ES_MX));
        fila(tabla, "CORREO:", d.correo());
        return tabla;
    }

    // Un renglón por equipo
    private PdfPTable equipos(DatosCarta d) {
        PdfPTable tabla = tablaDatos(4, new float[] { 1f, 1.4f, 1.4f, 1.6f });
        for (String columna : List.of("MONTACARGAS", "MARCA", "MODELO", "SERIE")) {
            tabla.addCell(etiqueta(columna));
        }
        for (EquipoCarta e : d.equipos()) {
            tabla.addCell(celda(new Phrase(e.numeroUnidad(), DATO_NEGRITA)));
            tabla.addCell(celda(new Phrase(e.marca().toUpperCase(ES_MX), DATO)));
            tabla.addCell(celda(new Phrase(e.modelo(), DATO)));
            tabla.addCell(celda(new Phrase(e.serie(), DATO)));
        }
        return tabla;
    }

    private PdfPTable periodo(DatosCarta d) {
        PdfPTable tabla = tablaDatos(6, new float[] { 1.1f, 1f, 1f, 1f, 1.3f, 0.8f });
        long dias = d.dias();
        tabla.addCell(etiqueta("FECHA DE INICIO"));
        tabla.addCell(celda(new Phrase(d.fechaInicio().format(DatosCarta.FECHA), DATO)));
        tabla.addCell(etiqueta("FECHA FINAL"));
        tabla.addCell(celda(new Phrase(d.fechaFin().format(DatosCarta.FECHA), DATO)));
        tabla.addCell(etiqueta("PERIODO DE RENTA"));
        tabla.addCell(celda(new Phrase(dias == 1 ? "1 día" : dias + " días", DATO)));
        return tabla;
    }

    // Una columna por equipo, vacía para revisarla al entregar
    private PdfPTable inventario(DatosCarta d) {
        int columnas = d.equipos().size();
        float[] anchos = new float[columnas + 1];
        anchos[0] = Math.max(2.2f, columnas * 0.6f);
        for (int i = 1; i <= columnas; i++) anchos[i] = 1f;

        PdfPTable tabla = tablaDatos(columnas + 1, anchos);
        tabla.setWidthPercentage(Math.min(100, 50 + columnas * 12));
        tabla.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabla.setSpacingBefore(4);
        tabla.setHeaderRows(1);

        tabla.addCell(etiqueta(""));
        for (EquipoCarta e : d.equipos()) {
            PdfPCell unidad = etiqueta(e.numeroUnidad());
            unidad.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(unidad);
        }
        for (String concepto : INVENTARIO) {
            tabla.addCell(celda(new Phrase(concepto, DATO)));
            for (int i = 0; i < columnas; i++) {
                PdfPCell vacia = celda(new Phrase(" ", DATO));
                vacia.setMinimumHeight(16);
                tabla.addCell(vacia);
            }
        }
        return tabla;
    }

    private PdfPTable firmas() {
        PdfPTable tabla = new PdfPTable(3);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[] { 1f, 0.15f, 1f });
        tabla.setSpacingBefore(46);
        tabla.addCell(lineaFirma("NOMBRE Y FIRMA DE RECIBIDO"));
        PdfPCell separador = new PdfPCell();
        separador.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(separador);
        tabla.addCell(lineaFirma("NOMBRE Y FIRMA DE ENTREGADO"));
        return tabla;
    }

    private static PdfPCell lineaFirma(String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, ETIQUETA));
        celda.setBorder(Rectangle.TOP);
        celda.setBorderColor(TEXTO);
        celda.setPaddingTop(4);
        return celda;
    }

    // ===== Piezas =====

    private static Paragraph seccion(String titulo) {
        Paragraph parrafo = new Paragraph(titulo, SECCION);
        parrafo.setSpacingBefore(12);
        parrafo.setSpacingAfter(2);
        return parrafo;
    }

    private static PdfPTable tablaDatos(int columnas, float[] anchos) {
        PdfPTable tabla = new PdfPTable(columnas);
        tabla.setWidthPercentage(100);
        tabla.setWidths(anchos);
        tabla.setSpacingBefore(10);
        return tabla;
    }

    private static void fila(PdfPTable tabla, String etiqueta, String valor) {
        fila(tabla, etiqueta, new Phrase(valor, DATO));
    }

    private static void fila(PdfPTable tabla, String etiqueta, Phrase valor) {
        tabla.addCell(etiqueta(etiqueta));
        tabla.addCell(celda(valor));
    }

    private static PdfPCell etiqueta(String texto) {
        PdfPCell celda = celda(new Phrase(texto, ETIQUETA));
        celda.setBackgroundColor(FONDO_ETIQUETA);
        return celda;
    }

    private static PdfPCell celda(Phrase contenido) {
        PdfPCell celda = contenido == null ? new PdfPCell() : new PdfPCell(contenido);
        celda.setBorderColor(BORDE);
        celda.setBorderWidth(0.6f);
        celda.setPadding(4);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }

    // "6872104455" → "687 210 4455"
    static String telefono(String telefono) {
        return telefono.replaceFirst("^(\\d{3})(\\d{3})(\\d{4})$", "$1 $2 $3");
    }
}
