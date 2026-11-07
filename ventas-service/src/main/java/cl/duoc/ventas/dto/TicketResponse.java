package cl.duoc.ventas.dto;

import cl.duoc.ventas.model.EstadoTicket;
import cl.duoc.ventas.model.Ticket;

import java.time.LocalDateTime;

/**
 * Datos de un ticket que devuelve la API (incluye donde esta el archivo y su historial de fechas).
 */
public record TicketResponse(
        Long id,
        String codigo,
        Long compraId,
        Long eventoId,
        String evento,
        LocalDateTime fechaEvento,
        String usuario,
        String nombreAsistente,
        String sector,
        Integer numeroEntrada,
        Integer totalEntradas,
        EstadoTicket estado,
        String rutaArchivo,
        String almacenamiento,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaActualizacion,
        LocalDateTime fechaSubidaS3
) {
    public static TicketResponse desde(Ticket t) {
        return new TicketResponse(t.getId(), t.getCodigo(), t.getCompraId(), t.getEventoId(), t.getNombreEvento(),
                t.getFechaEvento(), t.getUsuario(), t.getNombreAsistente(), t.getSector(), t.getNumeroEntrada(),
                t.getTotalEntradas(), t.getEstado(), t.getRutaArchivo(),
                t.getEstado() == EstadoTicket.SUBIDO_S3 ? "S3" : "EFS",
                t.getFechaGeneracion(), t.getFechaActualizacion(), t.getFechaSubidaS3());
    }
}
