package cl.duoc.ventas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos del cliente que se pueden corregir en una compra ya realizada.
 * (La cantidad y el evento no se modifican porque afectan las entradas reservadas.)
 */
public record ActualizarClienteRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombreCliente,

        @NotBlank(message = "El email del cliente es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150, message = "El email no puede superar 150 caracteres")
        String emailCliente
) {
}
