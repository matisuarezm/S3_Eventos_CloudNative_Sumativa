package cl.duoc.ventas.repository;

import cl.duoc.ventas.dto.VentasPorEventoDto;
import cl.duoc.ventas.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findAllByOrderByFechaCompraDesc();

    /**
     * Semana 3: totales de ventas por evento, calculados por la base de datos (GROUP BY).
     * "new ...VentasPorEventoDto(...)" le pide a JPA que arme directamente el record con cada fila.
     */
    @Query("""
            SELECT new cl.duoc.ventas.dto.VentasPorEventoDto(
                     c.eventoId, c.nombreEvento, COUNT(c), SUM(c.cantidadEntradas), SUM(c.precioTotal))
              FROM Compra c
             GROUP BY c.eventoId, c.nombreEvento
             ORDER BY c.eventoId
            """)
    List<VentasPorEventoDto> ventasPorEvento();
}
