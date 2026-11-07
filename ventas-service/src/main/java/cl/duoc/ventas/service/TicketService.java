package cl.duoc.ventas.service;

import cl.duoc.ventas.dto.ActualizarTicketRequest;
import cl.duoc.ventas.dto.TicketResponse;
import cl.duoc.ventas.exception.AlmacenamientoException;
import cl.duoc.ventas.exception.ConflictoException;
import cl.duoc.ventas.exception.RecursoNoEncontradoException;
import cl.duoc.ventas.exception.SolicitudInvalidaException;
import cl.duoc.ventas.model.Compra;
import cl.duoc.ventas.model.EstadoTicket;
import cl.duoc.ventas.model.Ticket;
import cl.duoc.ventas.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Logica de los tickets (Semana 3).
 *
 * Flujo:  compra -> generar PDF por entrada -> EFS (temporal) -> S3 (definitivo) -> se borra de EFS
 *
 * Organizacion (igual en EFS y en S3):
 *   tickets/evento-{eventoId}/{usuario}/ticket-{id}.pdf
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private final TicketRepository repository;
    private final CompraService compraService;
    private final TicketPdfService pdfService;
    private final AlmacenamientoEfsService efs;
    private final AlmacenamientoS3Service s3;

    public TicketService(TicketRepository repository,
                         CompraService compraService,
                         TicketPdfService pdfService,
                         AlmacenamientoEfsService efs,
                         AlmacenamientoS3Service s3) {
        this.repository = repository;
        this.compraService = compraService;
        this.pdfService = pdfService;
        this.efs = efs;
        this.s3 = s3;
    }

    // ------------------------------------------------------------------ generar

    /**
     * Genera un ticket por cada entrada de la compra y lo guarda en EFS.
     * Si subirAutomaticamente es true, ademas lo sube a S3 (y lo borra de EFS).
     * Si S3 falla, el ticket queda en EFS como GENERADO_EFS para subirlo despues.
     */
    @Transactional
    public List<TicketResponse> generar(Long compraId, boolean subirAutomaticamente) {
        Compra compra = compraService.buscarEntidad(compraId);
        if (repository.existsByCompraId(compraId)) {
            throw new ConflictoException("Los tickets de la compra " + compraId + " ya fueron generados");
        }

        List<Ticket> tickets = new ArrayList<>();
        for (int numero = 1; numero <= compra.getCantidadEntradas(); numero++) {
            Ticket ticket = repository.save(Ticket.desdeCompra(compra, numero)); // save -> obtiene el id
            ticket.setRutaArchivo(rutaArchivo(ticket));
            efs.guardar(ticket.getRutaArchivo(), pdfService.generar(ticket));
            tickets.add(ticket);
        }

        if (subirAutomaticamente) {
            for (Ticket ticket : tickets) {
                try {
                    subirAS3(ticket);
                } catch (AlmacenamientoException e) {
                    log.warn("Ticket {} queda en EFS, no se pudo subir a S3: {}", ticket.getCodigo(), e.getMessage());
                }
            }
        }
        return tickets.stream().map(TicketResponse::desde).toList();
    }

    // ------------------------------------------------------------------ subir

    /** Sube un ticket especifico a S3. */
    @Transactional
    public TicketResponse subir(Long id) {
        Ticket ticket = buscar(id);
        subirAS3(ticket);
        return TicketResponse.desde(ticket);
    }

    /** Sube a S3 todos los tickets que siguen en EFS (opcionalmente solo de un evento). */
    @Transactional
    public List<TicketResponse> subirPendientes(Long eventoId) {
        List<Ticket> pendientes = repository.buscar(eventoId, null, EstadoTicket.GENERADO_EFS);
        pendientes.forEach(this::subirAS3);
        return pendientes.stream().map(TicketResponse::desde).toList();
    }

    /** EFS -> S3. Si el archivo ya no esta en EFS, se vuelve a generar. Luego se borra el temporal. */
    private void subirAS3(Ticket ticket) {
        String ruta = ticket.getRutaArchivo();
        byte[] pdf = efs.existe(ruta) ? efs.leer(ruta) : pdfService.generar(ticket);
        s3.subir(ruta, pdf, CONTENT_TYPE_PDF);
        ticket.setEstado(EstadoTicket.SUBIDO_S3);
        ticket.setFechaSubidaS3(LocalDateTime.now());
        efs.eliminar(ruta); // EFS es solo almacenamiento temporal
    }

    // ------------------------------------------------------------------ consultar / descargar

    /** Historial de tickets con filtros opcionales por evento, usuario y estado. */
    @Transactional(readOnly = true)
    public List<TicketResponse> historial(Long eventoId, String usuario, EstadoTicket estado) {
        return repository.buscar(eventoId, normalizarUsuario(usuario), estado)
                .stream().map(TicketResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse obtener(Long id) {
        return TicketResponse.desde(buscar(id));
    }

    /** PDF de un ticket: desde S3 si ya se subio, o desde EFS si aun esta pendiente. */
    @Transactional(readOnly = true)
    public byte[] descargar(Long id) {
        return contenido(buscar(id));
    }

    public String nombreArchivo(Long id) {
        return "ticket-" + id + ".pdf";
    }

    /** ZIP con todos los tickets de un evento y/o usuario, manteniendo las carpetas. */
    @Transactional(readOnly = true)
    public byte[] descargarZip(Long eventoId, String usuario) {
        if (eventoId == null && (usuario == null || usuario.isBlank())) {
            throw new SolicitudInvalidaException("Debe indicar eventoId, usuario o ambos");
        }
        List<Ticket> tickets = repository.buscar(eventoId, normalizarUsuario(usuario), null);
        if (tickets.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay tickets para los filtros indicados");
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            for (Ticket ticket : tickets) {
                // dentro del zip: evento-5/camila_correo.cl/ticket-12.pdf
                zip.putNextEntry(new ZipEntry(ticket.getRutaArchivo().replaceFirst("^tickets/", "")));
                zip.write(contenido(ticket));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo armar el archivo ZIP", e);
        }
        return salida.toByteArray();
    }

    // ------------------------------------------------------------------ modificar / eliminar

    /** Modifica los datos del ticket y actualiza el PDF donde este (S3 o EFS), en la misma ruta. */
    @Transactional
    public TicketResponse actualizar(Long id, ActualizarTicketRequest request) {
        Ticket ticket = buscar(id);
        ticket.setNombreAsistente(request.nombreAsistente().trim());
        ticket.setSector(request.sector().trim());
        ticket.setFechaActualizacion(LocalDateTime.now());

        byte[] pdf = pdfService.generar(ticket);
        if (ticket.getEstado() == EstadoTicket.SUBIDO_S3) {
            s3.subir(ticket.getRutaArchivo(), pdf, CONTENT_TYPE_PDF);  // sobrescribe el objeto en S3
        } else {
            efs.guardar(ticket.getRutaArchivo(), pdf);                   // sobrescribe el temporal
        }
        return TicketResponse.desde(ticket);
    }

    /** Elimina el ticket: el archivo (S3 y/o EFS) y su registro. */
    @Transactional
    public TicketResponse eliminar(Long id) {
        Ticket ticket = buscar(id);
        if (ticket.getEstado() == EstadoTicket.SUBIDO_S3) {
            s3.eliminar(ticket.getRutaArchivo());
        }
        efs.eliminar(ticket.getRutaArchivo());
        repository.delete(ticket);
        return TicketResponse.desde(ticket);
    }

    // ------------------------------------------------------------------ apoyo

    private Ticket buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el ticket con id " + id));
    }

    private byte[] contenido(Ticket ticket) {
        if (ticket.getEstado() == EstadoTicket.SUBIDO_S3) {
            return s3.descargar(ticket.getRutaArchivo());
        }
        return efs.existe(ticket.getRutaArchivo()) ? efs.leer(ticket.getRutaArchivo()) : pdfService.generar(ticket);
    }

    /** tickets/evento-5/camila.rojas_correo.cl/ticket-12.pdf */
    private String rutaArchivo(Ticket ticket) {
        return "tickets/evento-" + ticket.getEventoId()
                + "/" + carpetaUsuario(ticket.getUsuario())
                + "/ticket-" + ticket.getId() + ".pdf";
    }

    /** El email no sirve tal cual como nombre de carpeta: se reemplazan los caracteres especiales. */
    static String carpetaUsuario(String email) {
        return email.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
    }

    private String normalizarUsuario(String usuario) {
        return (usuario == null || usuario.isBlank()) ? null : usuario.trim().toLowerCase();
    }
}
