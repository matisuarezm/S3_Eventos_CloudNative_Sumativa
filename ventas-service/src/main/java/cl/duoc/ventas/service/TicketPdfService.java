package cl.duoc.ventas.service;

import cl.duoc.ventas.model.Ticket;
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
 * Genera el PDF de un ticket personalizado (uno por entrada).
 * Igual que ResumenPdfService: solo arma el documento en memoria.
 */
@Service
public class TicketPdfService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final Locale CHILE = Locale.of("es", "CL");

    public byte[] generar(Ticket ticket) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        // Formato A5 apaisado: tamano tipo "entrada"
        Document documento = new Document(PageSize.A5.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font marca = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font evento = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font etiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font codigo = FontFactory.getFont(FontFactory.COURIER_BOLD, 14);

            Paragraph encabezado = new Paragraph("TICKET DE ACCESO", marca);
            encabezado.setAlignment(Element.ALIGN_CENTER);
            documento.add(encabezado);

            Paragraph nombreEvento = new Paragraph(ticket.getNombreEvento(), evento);
            nombreEvento.setAlignment(Element.ALIGN_CENTER);
            nombreEvento.setSpacingAfter(12f);
            documento.add(nombreEvento);

            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{1.1f, 1.6f, 1.1f, 1.6f});

            celda(tabla, "Asistente", etiqueta, true);
            celda(tabla, ticket.getNombreAsistente(), normal, false);
            celda(tabla, "Fecha", etiqueta, true);
            celda(tabla, formatear(ticket.getFechaEvento()), normal, false);

            celda(tabla, "Sector", etiqueta, true);
            celda(tabla, ticket.getSector(), normal, false);
            celda(tabla, "Entrada", etiqueta, true);
            celda(tabla, ticket.getNumeroEntrada() + " de " + ticket.getTotalEntradas(), normal, false);

            celda(tabla, "Titular", etiqueta, true);
            celda(tabla, ticket.getUsuario(), normal, false);
            celda(tabla, "Compra N°", etiqueta, true);
            celda(tabla, String.valueOf(ticket.getCompraId()), normal, false);

            celda(tabla, "Precio", etiqueta, true);
            celda(tabla, "$" + NumberFormat.getIntegerInstance(CHILE).format(ticket.getPrecio()), normal, false);
            celda(tabla, "Emitido", etiqueta, true);
            celda(tabla, formatear(LocalDateTime.now()), normal, false);

            documento.add(tabla);

            Paragraph cod = new Paragraph(ticket.getCodigo(), codigo);
            cod.setAlignment(Element.ALIGN_CENTER);
            cod.setSpacingBefore(14f);
            documento.add(cod);

            Paragraph pie = new Paragraph("Presente este ticket en el acceso. Valido para una persona.", normal);
            pie.setAlignment(Element.ALIGN_CENTER);
            documento.add(pie);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF del ticket " + ticket.getCodigo(), e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    private void celda(PdfPTable tabla, String texto, Font fuente, boolean esEtiqueta) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "-" : texto, fuente));
        celda.setPadding(6f);
        if (esEtiqueta) {
            celda.setGrayFill(0.92f);
        }
        tabla.addCell(celda);
    }

    private String formatear(LocalDateTime fecha) {
        return fecha == null ? "-" : fecha.format(FECHA);
    }
}
