package cl.duoc.eventos.controller;

import cl.duoc.eventos.dto.EventoRequest;
import cl.duoc.eventos.dto.EventoResponse;
import cl.duoc.eventos.dto.ReservaRequest;
import cl.duoc.eventos.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService service;

    public EventoController(EventoService service) {
        this.service = service;
    }

    /**
     * Lista los eventos disponibles (fecha futura y con entradas).
     * Con ?todos=true devuelve todos los eventos registrados.
     */
    @GetMapping
    public List<EventoResponse> listar(@RequestParam(defaultValue = "false") boolean todos) {
        return todos ? service.listarTodos() : service.listarDisponibles();
    }

    @GetMapping("/{id}")
    public EventoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EventoResponse> crear(@Valid @RequestBody EventoRequest request) {
        EventoResponse creado = service.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    /** Descuenta entradas de un evento. Lo invoca ventas-service al registrar una compra. */
    @PostMapping("/{id}/reservas")
    public EventoResponse reservar(@PathVariable Long id, @Valid @RequestBody ReservaRequest request) {
        return service.reservarEntradas(id, request.cantidad());
    }
}
