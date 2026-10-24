package cl.duoc.eventos.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Datos que recibe el endpoint POST /api/eventos.
 */
public record EventoRequest(
        @NotBlank(message = "El nombre del evento es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        String descripcion,

        @NotBlank(message = "El lugar es obligatorio")
        @Size(max = 150, message = "El lugar no puede superar 150 caracteres")
        String lugar,

        @NotNull(message = "La fecha es obligatoria (formato yyyy-MM-ddTHH:mm:ss)")
        @Future(message = "La fecha del evento debe ser futura")
        LocalDateTime fecha,

        @NotNull(message = "El precio de la entrada es obligatorio")
        @Min(value = 1, message = "El precio debe ser mayor a 0")
        Long precioEntrada,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser al menos 1")
        Integer capacidad
) {
}
