package cl.duoc.ventas.service;

import cl.duoc.ventas.client.EventosClient;
import cl.duoc.ventas.dto.CompraRequest;
import cl.duoc.ventas.dto.CompraResponse;
import cl.duoc.ventas.dto.EventoDto;
import cl.duoc.ventas.exception.RecursoNoEncontradoException;
import cl.duoc.ventas.model.Compra;
import cl.duoc.ventas.repository.CompraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompraService {

    private static final Logger log = LoggerFactory.getLogger(CompraService.class);

    private final CompraRepository repository;
    private final EventosClient eventosClient;

    public CompraService(CompraRepository repository, EventosClient eventosClient) {
        this.repository = repository;
        this.eventosClient = eventosClient;
    }

    /**
     * 1. Reserva las entradas en eventos-service (valida que el evento exista y tenga cupos).
     * 2. Calcula el total con el precio vigente del evento.
     * 3. Guarda la compra en Oracle y devuelve el resumen.
     */
    @Transactional
    public CompraResponse comprar(CompraRequest request) {
        EventoDto evento = eventosClient.reservarEntradas(request.eventoId(), request.cantidad());

        long precioUnitario = evento.precioEntrada();
        long total = precioUnitario * request.cantidad();

        Compra compra = new Compra();
        compra.setEventoId(evento.id());
        compra.setNombreEvento(evento.nombre());
        compra.setFechaEvento(evento.fecha());
        compra.setNombreCliente(request.nombreCliente().trim());
        compra.setEmailCliente(request.emailCliente().trim().toLowerCase());
        compra.setCantidadEntradas(request.cantidad());
        compra.setPrecioUnitario(precioUnitario);
        compra.setPrecioTotal(total);
        compra.setFechaCompra(LocalDateTime.now());

        Compra guardada = repository.save(compra);
        log.info("Compra {} registrada: {} entrada(s) para '{}' por ${}",
                guardada.getId(), guardada.getCantidadEntradas(), guardada.getNombreEvento(), total);
        return CompraResponse.desde(guardada);
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listar() {
        return repository.findAllByOrderByFechaCompraDesc().stream().map(CompraResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public CompraResponse obtener(Long id) {
        return repository.findById(id)
                .map(CompraResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la compra con id " + id));
    }
}
