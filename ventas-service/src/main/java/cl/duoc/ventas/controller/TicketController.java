package cl.duoc.ventas.controller;

import cl.duoc.ventas.dto.ActualizarTicketRequest;
import cl.duoc.ventas.dto.TicketResponse;
import cl.duoc.ventas.model.EstadoTicket;
import cl.duoc.ventas.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de tickets (Semana 3).
 *
 *   POST   /api/compras/{compraId}/tickets      generar tickets (EFS) y subir a S3 automaticamente
 *   POST   /api/tickets/{id}/s3                 subir un ticket a S3
 *   POST   /api/tickets/s3/pendientes           subir a S3 todos los que siguen en EFS
 *   GET    /api/tickets?eventoId=&usuario=&estado=   historial con filtros
 *   GET    /api/tickets/{id}                    detalle
 *   GET    /api/tickets/{id}/pdf                descargar un ticket
 *   GET    /api/tickets/descargar?eventoId=&usuario= descargar ZIP por evento o usuario
 *   PUT    /api/tickets/{id}                    modificar detalles (actualiza el PDF en S3)
 *   DELETE /api/tickets/{id}                    eliminar un ticket
 */
@RestController
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @PostMapping("/api/compras/{compraId}/tickets")
    public ResponseEntity<List<TicketResponse>> generar(
            @PathVariable Long compraId,
            @RequestParam(defaultValue = "true") boolean subirAutomaticamente) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generar(compraId, subirAutomaticamente));
    }

    @PostMapping("/api/tickets/{id}/s3")
    public TicketResponse subir(@PathVariable Long id) {
        return service.subir(id);
    }

    @PostMapping("/api/tickets/s3/pendientes")
    public List<TicketResponse> subirPendientes(@RequestParam(required = false) Long eventoId) {
        return service.subirPendientes(eventoId);
    }

    @GetMapping("/api/tickets")
    public List<TicketResponse> historial(@RequestParam(required = false) Long eventoId,
                                          @RequestParam(required = false) String usuario,
                                          @RequestParam(required = false) EstadoTicket estado) {
        return service.historial(eventoId, usuario, estado);
    }

    @GetMapping("/api/tickets/{id}")
    public TicketResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/api/tickets/{id}/pdf")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        return archivo(service.descargar(id), service.nombreArchivo(id), MediaType.APPLICATION_PDF);
    }

    @GetMapping("/api/tickets/descargar")
    public ResponseEntity<byte[]> descargarZip(@RequestParam(required = false) Long eventoId,
                                               @RequestParam(required = false) String usuario) {
        String nombre = "tickets"
                + (eventoId != null ? "-evento-" + eventoId : "")
                + (usuario != null && !usuario.isBlank() ? "-" + usuario.trim().toLowerCase().replaceAll("[^a-z0-9._-]", "_") : "")
                + ".zip";
        return archivo(service.descargarZip(eventoId, usuario), nombre, MediaType.parseMediaType("application/zip"));
    }

    @PutMapping("/api/tickets/{id}")
    public TicketResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarTicketRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/api/tickets/{id}")
    public TicketResponse eliminar(@PathVariable Long id) {
        return service.eliminar(id);
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, String nombre, MediaType tipo) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .contentLength(contenido.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(contenido);
    }
}
