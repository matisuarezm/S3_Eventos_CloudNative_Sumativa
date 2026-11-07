package cl.duoc.ventas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Capacidad y entradas disponibles de un evento (respuesta de GET /api/eventos/{id} en eventos-service).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoInfoDto(
        Long id,
        String nombre,
        Integer capacidad,
        Integer entradasDisponibles
) {
}
