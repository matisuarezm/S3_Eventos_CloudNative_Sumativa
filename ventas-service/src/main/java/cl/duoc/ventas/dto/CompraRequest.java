package cl.duoc.ventas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos que recibe el endpoint POST /api/compras.
 */
public record CompraRequest(
        @NotNull(message = "Debe indicar el id del evento")
        Long eventoId,

        @NotNull(message = "Debe indicar la cantidad de entradas")
        @Min(value = 1, message = "Debe comprar al menos 1 entrada")
        @Max(value = 10, message = "Maximo 10 entradas por compra")
        Integer cantidad,

        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombreCliente,

        @NotBlank(message = "El email del cliente es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150, message = "El email no puede superar 150 caracteres")
        String emailCliente
) {
}
