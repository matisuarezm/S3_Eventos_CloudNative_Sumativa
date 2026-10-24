package cl.duoc.eventos.service;

import cl.duoc.eventos.dto.EventoRequest;
import cl.duoc.eventos.dto.EventoResponse;
import cl.duoc.eventos.exception.EntradasInsuficientesException;
import cl.duoc.eventos.exception.RecursoNoEncontradoException;
import cl.duoc.eventos.model.Evento;
import cl.duoc.eventos.repository.EventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventoService {

    private final EventoRepository repository;

    public EventoService(EventoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listarDisponibles() {
        return repository
                .findByFechaAfterAndEntradasDisponiblesGreaterThanOrderByFechaAsc(LocalDateTime.now(), 0)
                .stream()
                .map(EventoResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listarTodos() {
        return repository.findAll().stream().map(EventoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public EventoResponse obtener(Long id) {
        return EventoResponse.desde(buscar(id));
    }

    @Transactional
    public EventoResponse crear(EventoRequest request) {
        Evento evento = new Evento(
                request.nombre().trim(),
                request.descripcion(),
                request.lugar().trim(),
                request.fecha(),
                request.precioEntrada(),
                request.capacidad());
        return EventoResponse.desde(repository.save(evento));
    }

    @Transactional
    public EventoResponse reservarEntradas(Long id, int cantidad) {
        Evento evento = buscar(id);
        if (!evento.getFecha().isAfter(LocalDateTime.now())) {
            throw new EntradasInsuficientesException("El evento '" + evento.getNombre() + "' ya se realizo");
        }
        int filas = repository.descontarEntradas(id, cantidad);
        if (filas == 0) {
            throw new EntradasInsuficientesException(
                    "No hay entradas suficientes para '" + evento.getNombre() + "'. Disponibles: "
                            + evento.getEntradasDisponibles() + ", solicitadas: " + cantidad);
        }
        return EventoResponse.desde(buscar(id));
    }

    private Evento buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el evento con id " + id));
    }
}
