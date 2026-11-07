package cl.duoc.ventas.controller;

import cl.duoc.ventas.dto.EstadisticaEventoResponse;
import cl.duoc.ventas.service.EstadisticaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reporte de ventas y reservas por evento.
 *   GET /api/estadisticas/eventos        todos los eventos con ventas
 *   GET /api/estadisticas/eventos/{id}   un evento
 */
@RestController
@RequestMapping("/api/estadisticas/eventos")
public class EstadisticaController {

    private final EstadisticaService service;

    public EstadisticaController(EstadisticaService service) {
        this.service = service;
    }

    @GetMapping
    public List<EstadisticaEventoResponse> porEvento() {
        return service.porEvento();
    }

    @GetMapping("/{eventoId}")
    public EstadisticaEventoResponse deEvento(@PathVariable Long eventoId) {
        return service.deEvento(eventoId);
    }
}
