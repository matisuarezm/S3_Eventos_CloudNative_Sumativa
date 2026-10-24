package cl.duoc.ventas.dto;

import cl.duoc.ventas.model.Compra;

import java.time.LocalDateTime;

/**
 * Resumen de la compra: evento, numero de entradas, precio unitario y precio total.
 */
public record CompraResponse(
        Long idCompra,
        Long eventoId,
        String evento,
        LocalDateTime fechaEvento,
        String nombreCliente,
        String emailCliente,
        Integer cantidadEntradas,
        Long precioUnitario,
        Long precioTotal,
        LocalDateTime fechaCompra
) {
    public static CompraResponse desde(Compra c) {
        return new CompraResponse(c.getId(), c.getEventoId(), c.getNombreEvento(), c.getFechaEvento(),
                c.getNombreCliente(), c.getEmailCliente(), c.getCantidadEntradas(),
                c.getPrecioUnitario(), c.getPrecioTotal(), c.getFechaCompra());
    }
}
