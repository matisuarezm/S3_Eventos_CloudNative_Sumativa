package cl.duoc.ventas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Detalles modificables de un ticket.
 */
public record ActualizarTicketRequest(
        @NotBlank(message = "El nombre del asistente es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombreAsistente,

        @NotBlank(message = "El sector es obligatorio")
        @Size(max = 40, message = "El sector no puede superar 40 caracteres")
        String sector
) {
}
