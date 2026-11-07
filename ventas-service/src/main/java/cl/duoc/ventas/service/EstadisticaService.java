package cl.duoc.ventas.service;

import cl.duoc.ventas.client.EventosClient;
import cl.duoc.ventas.dto.EstadisticaEventoResponse;
import cl.duoc.ventas.dto.EventoInfoDto;
import cl.duoc.ventas.dto.VentasPorEventoDto;
import cl.duoc.ventas.exception.RecursoNoEncontradoException;
import cl.duoc.ventas.model.EstadoTicket;
import cl.duoc.ventas.repository.CompraRepository;
import cl.duoc.ventas.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Estadisticas de ventas y reservas por evento.
 * - Ventas (compras, entradas, recaudacion): se calculan en la BD con GROUP BY.
 * - Tickets por estado: tabla TICKETS.
 * - Capacidad y disponibles: se consultan a eventos-service (si no responde, quedan en null).
 */
@Service
public class EstadisticaService {

    private final CompraRepository compraRepository;
    private final TicketRepository ticketRepository;
    private final EventosClient eventosClient;

    public EstadisticaService(CompraRepository compraRepository,
                              TicketRepository ticketRepository,
                              EventosClient eventosClient) {
        this.compraRepository = compraRepository;
        this.ticketRepository = ticketRepository;
        this.eventosClient = eventosClient;
    }

    @Transactional(readOnly = true)
    public List<EstadisticaEventoResponse> porEvento() {
        Map<String, Long> tickets = contarTickets();
        return compraRepository.ventasPorEvento().stream()
                .map(ventas -> armar(ventas, tickets))
                .toList();
    }

    @Transactional(readOnly = true)
    public EstadisticaEventoResponse deEvento(Long eventoId) {
        return porEvento().stream()
                .filter(e -> e.eventoId().equals(eventoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El evento " + eventoId + " no tiene ventas registradas"));
    }

    private EstadisticaEventoResponse armar(VentasPorEventoDto ventas, Map<String, Long> tickets) {
        long enEfs = tickets.getOrDefault(clave(ventas.eventoId(), EstadoTicket.GENERADO_EFS), 0L);
        long enS3 = tickets.getOrDefault(clave(ventas.eventoId(), EstadoTicket.SUBIDO_S3), 0L);

        EventoInfoDto evento = eventosClient.obtenerEvento(ventas.eventoId());
        Integer capacidad = evento == null ? null : evento.capacidad();
        Integer disponibles = evento == null ? null : evento.entradasDisponibles();
        Double ocupacion = null;
        if (capacidad != null && capacidad > 0 && disponibles != null) {
            ocupacion = Math.round((capacidad - disponibles) * 1000.0 / capacidad) / 10.0;
        }

        return new EstadisticaEventoResponse(
                ventas.eventoId(),
                ventas.evento(),
                ventas.compras(),
                ventas.entradasVendidas(),
                ventas.recaudacion(),
                enEfs + enS3,
                enEfs,
                enS3,
                capacidad,
                disponibles,
                ocupacion);
    }

    /** Convierte las filas [eventoId, estado, cantidad] en un mapa "eventoId-ESTADO" -> cantidad. */
    private Map<String, Long> contarTickets() {
        Map<String, Long> mapa = new HashMap<>();
        for (Object[] fila : ticketRepository.contarPorEventoYEstado()) {
            mapa.put(clave((Long) fila[0], (EstadoTicket) fila[1]), (Long) fila[2]);
        }
        return mapa;
    }

    private String clave(Long eventoId, EstadoTicket estado) {
        return eventoId + "-" + estado.name();
    }
}
