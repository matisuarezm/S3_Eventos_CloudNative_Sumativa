package cl.duoc.eventos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cantidad de entradas a descontar de un evento.
 * Lo usa ventas-service al confirmar una compra.
 */
public record ReservaRequest(
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad
) {
}
