package cl.duoc.ventas.service;

import cl.duoc.ventas.model.Compra;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera el archivo PDF con el resumen de una compra.
 * Solo arma el documento en memoria (byte[]); no sabe nada de S3 ni de HTTP.
 */
@Service
public class ResumenPdfService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final Locale CHILE = Locale.of("es", "CL");

    public byte[] generar(Compra compra) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font etiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph encabezado = new Paragraph("Resumen de compra N° " + compra.getId(), titulo);
            encabezado.setAlignment(Element.ALIGN_CENTER);
            documento.add(encabezado);

            Paragraph emision = new Paragraph("Emitido el " + formatear(LocalDateTime.now()), subtitulo);
            emision.setAlignment(Element.ALIGN_CENTER);
            emision.setSpacingAfter(20f);
            documento.add(emision);

            PdfPTable tabla = new PdfPTable(2);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{1f, 2f});

            fila(tabla, "N° de compra", String.valueOf(compra.getId()), etiqueta, normal);
            fila(tabla, "Fecha de compra", formatear(compra.getFechaCompra()), etiqueta, normal);
            fila(tabla, "Cliente", compra.getNombreCliente(), etiqueta, normal);
            fila(tabla, "Email", compra.getEmailCliente(), etiqueta, normal);
            fila(tabla, "Evento", compra.getNombreEvento(), etiqueta, normal);
            fila(tabla, "Fecha del evento", formatear(compra.getFechaEvento()), etiqueta, normal);
            fila(tabla, "Cantidad de entradas", String.valueOf(compra.getCantidadEntradas()), etiqueta, normal);
            fila(tabla, "Precio unitario", pesos(compra.getPrecioUnitario()), etiqueta, normal);
            fila(tabla, "Precio total", pesos(compra.getPrecioTotal()), etiqueta, etiqueta);

            documento.add(tabla);

            Paragraph pie = new Paragraph("Documento generado automaticamente por ventas-service.", subtitulo);
            pie.setSpacingBefore(25f);
            documento.add(pie);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de la compra " + compra.getId(), e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    /** Agrega una fila "etiqueta | valor" a la tabla. */
    private void fila(PdfPTable tabla, String nombre, String valor, Font fuenteNombre, Font fuenteValor) {
        PdfPCell celdaNombre = new PdfPCell(new Phrase(nombre, fuenteNombre));
        celdaNombre.setGrayFill(0.92f);
        celdaNombre.setPadding(7f);
        tabla.addCell(celdaNombre);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, fuenteValor));
        celdaValor.setPadding(7f);
        tabla.addCell(celdaValor);
    }

    private String formatear(LocalDateTime fecha) {
        return fecha == null ? "-" : fecha.format(FECHA);
    }

    private String pesos(Long monto) {
        return "$" + NumberFormat.getIntegerInstance(CHILE).format(monto);
    }
}
