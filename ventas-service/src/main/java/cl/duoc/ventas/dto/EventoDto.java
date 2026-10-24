package cl.duoc.ventas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Datos del evento que devuelve eventos-service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoDto(
        Long id,
        String nombre,
        LocalDateTime fecha,
        Long precioEntrada,
        Integer entradasDisponibles
) {
}
