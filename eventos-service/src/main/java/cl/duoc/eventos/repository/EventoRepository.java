package cl.duoc.eventos.repository;

import cl.duoc.eventos.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    /** Eventos que aun no ocurren y que tienen entradas disponibles, ordenados por fecha. */
    List<Evento> findByFechaAfterAndEntradasDisponiblesGreaterThanOrderByFechaAsc(
            LocalDateTime fecha, Integer entradasDisponibles);

    /**
     * Descuenta entradas en una sola sentencia UPDATE, solo si alcanzan.
     * Al hacerlo en la base de datos se evita vender mas entradas que la capacidad
     * cuando llegan dos compras al mismo tiempo.
     *
     * @return filas actualizadas (1 = reserva exitosa, 0 = no habia entradas suficientes)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Evento e
               SET e.entradasDisponibles = e.entradasDisponibles - :cantidad
             WHERE e.id = :id
               AND e.entradasDisponibles >= :cantidad
            """)
    int descontarEntradas(@Param("id") Long id, @Param("cantidad") Integer cantidad);
}
