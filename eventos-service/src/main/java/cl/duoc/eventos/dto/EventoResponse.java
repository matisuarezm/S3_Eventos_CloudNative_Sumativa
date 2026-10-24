package cl.duoc.eventos.dto;

import cl.duoc.eventos.model.Evento;

import java.time.LocalDateTime;

/**
 * Representacion de un evento que devuelve la API.
 */
public record EventoResponse(
        Long id,
        String nombre,
        String descripcion,
        String lugar,
        LocalDateTime fecha,
        Long precioEntrada,
        Integer capacidad,
        Integer entradasDisponibles
) {
    public static EventoResponse desde(Evento e) {
        return new EventoResponse(e.getId(), e.getNombre(), e.getDescripcion(), e.getLugar(),
                e.getFecha(), e.getPrecioEntrada(), e.getCapacidad(), e.getEntradasDisponibles());
    }
}
