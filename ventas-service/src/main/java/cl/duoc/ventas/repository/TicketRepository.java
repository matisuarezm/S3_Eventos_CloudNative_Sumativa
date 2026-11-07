package cl.duoc.ventas.repository;

import cl.duoc.ventas.model.EstadoTicket;
import cl.duoc.ventas.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsByCompraId(Long compraId);

    /**
     * Historial con filtros opcionales: si un filtro viene null, no se aplica.
     * Ej: buscar(5, null, null) = todos los tickets del evento 5.
     */
    @Query("""
            SELECT t FROM Ticket t
             WHERE (:eventoId IS NULL OR t.eventoId = :eventoId)
               AND (:usuario  IS NULL OR t.usuario  = :usuario)
               AND (:estado   IS NULL OR t.estado   = :estado)
             ORDER BY t.id
            """)
    List<Ticket> buscar(@Param("eventoId") Long eventoId,
                        @Param("usuario") String usuario,
                        @Param("estado") EstadoTicket estado);

    /** Cantidad de tickets por evento y estado, para las estadisticas. Cada fila: [eventoId, estado, cantidad]. */
    @Query("SELECT t.eventoId, t.estado, COUNT(t) FROM Ticket t GROUP BY t.eventoId, t.estado")
    List<Object[]> contarPorEventoYEstado();
}
