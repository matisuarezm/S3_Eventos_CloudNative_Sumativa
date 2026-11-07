package cl.duoc.ventas.controller;

import cl.duoc.ventas.dto.ActualizarClienteRequest;
import cl.duoc.ventas.dto.CompraRequest;
import cl.duoc.ventas.dto.CompraResponse;
import cl.duoc.ventas.service.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService service;

    public CompraController(CompraService service) {
        this.service = service;
    }

    /** Compra de entradas: devuelve el resumen (evento, cantidad, precio unitario y total). */
    @PostMapping
    public ResponseEntity<CompraResponse> comprar(@Valid @RequestBody CompraRequest request) {
        CompraResponse compra = service.comprar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(compra.idCompra()).toUri();
        return ResponseEntity.created(location).body(compra);
    }

    @GetMapping
    public List<CompraResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public CompraResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** Semana 2: corrige nombre y email del cliente (en caso de error en el resumen). */
    @PutMapping("/{id}")
    public CompraResponse actualizarCliente(@PathVariable Long id,
                                            @Valid @RequestBody ActualizarClienteRequest request) {
        return service.actualizarCliente(id, request);
    }
}
