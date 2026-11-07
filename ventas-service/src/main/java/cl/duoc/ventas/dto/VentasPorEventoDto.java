package cl.duoc.ventas.dto;

/**
 * Resultado de la consulta GROUP BY de compras por evento (lo arma JPA directamente).
 */
public record VentasPorEventoDto(
        Long eventoId,
        String evento,
        Long compras,
        Long entradasVendidas,
        Long recaudacion
) {
}
