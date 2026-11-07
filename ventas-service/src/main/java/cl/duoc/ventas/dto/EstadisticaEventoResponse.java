package cl.duoc.ventas.dto;

/**
 * Reporte de ventas y reservas de un evento.
 */
public record EstadisticaEventoResponse(
        Long eventoId,
        String evento,
        Long comprasRealizadas,
        Long entradasVendidas,
        Long recaudacionTotal,
        Long ticketsGenerados,
        Long ticketsEnEfs,
        Long ticketsEnS3,
        Integer capacidad,
        Integer entradasDisponibles,
        Double porcentajeOcupacion
) {
}
