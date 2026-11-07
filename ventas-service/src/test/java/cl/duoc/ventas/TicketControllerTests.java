package cl.duoc.ventas;

import cl.duoc.ventas.client.EventosClient;
import cl.duoc.ventas.dto.EventoInfoDto;
import cl.duoc.ventas.exception.AlmacenamientoException;
import cl.duoc.ventas.model.Compra;
import cl.duoc.ventas.model.EstadoTicket;
import cl.duoc.ventas.model.Ticket;
import cl.duoc.ventas.repository.CompraRepository;
import cl.duoc.ventas.repository.TicketRepository;
import cl.duoc.ventas.service.AlmacenamientoS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de tickets y estadisticas (Semana 3).
 * - S3 se simula con @MockBean (no se necesitan credenciales AWS).
 * - EFS se reemplaza por una carpeta local (app.efs.path en application-test.properties),
 *   asi se puede comprobar que los archivos realmente se escriben y se borran.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    CompraRepository compraRepository;

    @Autowired
    TicketRepository ticketRepository;

    @MockBean
    AlmacenamientoS3Service s3;

    @MockBean
    EventosClient eventosClient;

    @Value("${app.efs.path}")
    String efsPath;

    private Long compraId;

    @BeforeEach
    void preparar() throws Exception {
        ticketRepository.deleteAll();
        compraRepository.deleteAll();
        FileSystemUtils.deleteRecursively(Path.of(efsPath));

        Compra compra = new Compra();
        compra.setEventoId(7L);
        compra.setNombreEvento("Festival de Jazz");
        compra.setFechaEvento(LocalDateTime.now().plusDays(30));
        compra.setNombreCliente("Camila Rojas");
        compra.setEmailCliente("camila@correo.cl");
        compra.setCantidadEntradas(2);
        compra.setPrecioUnitario(18000L);
        compra.setPrecioTotal(36000L);
        compra.setFechaCompra(LocalDateTime.now());
        compraId = compraRepository.save(compra).getId();
    }

    /** Ruta en EFS/S3 esperada para un ticket. */
    private String ruta(Long ticketId) {
        return "tickets/evento-7/camila_correo.cl/ticket-" + ticketId + ".pdf";
    }

    @Test
    void generarTicketsLosSubeAS3YBorraElTemporalDeEfs() throws Exception {
        mvc.perform(post("/api/compras/{id}/tickets", compraId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))                       // 1 ticket por entrada
                .andExpect(jsonPath("$[0].estado").value("SUBIDO_S3"))
                .andExpect(jsonPath("$[0].rutaArchivo", startsWith("tickets/evento-7/camila_correo.cl/ticket-")));

        List<Ticket> tickets = ticketRepository.findAll();
        verify(s3, times(2)).subir(anyString(), any(byte[].class), eq("application/pdf"));
        for (Ticket t : tickets) {
            verify(s3).subir(eq(ruta(t.getId())), any(byte[].class), eq("application/pdf"));
            assertThat(Files.exists(Path.of(efsPath, t.getRutaArchivo()))).isFalse(); // temporal borrado
        }
    }

    @Test
    void generarSinSubidaDejaLosPdfEnEfs() throws Exception {
        mvc.perform(post("/api/compras/{id}/tickets", compraId).param("subirAutomaticamente", "false"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].estado").value("GENERADO_EFS"))
                .andExpect(jsonPath("$[0].almacenamiento").value("EFS"));

        for (Ticket t : ticketRepository.findAll()) {
            Path archivo = Path.of(efsPath, t.getRutaArchivo());
            assertThat(Files.exists(archivo)).isTrue();
            assertThat(new String(Files.readAllBytes(archivo), 0, 4)).isEqualTo("%PDF");
        }
        verify(s3, never()).subir(anyString(), any(byte[].class), anyString());
    }

    @Test
    void siS3FallaElTicketQuedaEnEfs() throws Exception {
        doThrow(new AlmacenamientoException("S3 caido", null))
                .when(s3).subir(anyString(), any(byte[].class), anyString());

        mvc.perform(post("/api/compras/{id}/tickets", compraId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].estado").value("GENERADO_EFS"));
    }

    @Test
    void generarDosVecesRetorna409() throws Exception {
        mvc.perform(post("/api/compras/{id}/tickets", compraId)).andExpect(status().isCreated());
        mvc.perform(post("/api/compras/{id}/tickets", compraId)).andExpect(status().isConflict());
    }

    @Test
    void generarParaCompraInexistenteRetorna404() throws Exception {
        mvc.perform(post("/api/compras/{id}/tickets", 99999)).andExpect(status().isNotFound());
    }

    @Test
    void subirTicketPendienteAS3() throws Exception {
        mvc.perform(post("/api/compras/{id}/tickets", compraId).param("subirAutomaticamente", "false"));
        Ticket ticket = ticketRepository.findAll().get(0);

        mvc.perform(post("/api/tickets/{id}/s3", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SUBIDO_S3"))
                .andExpect(jsonPath("$.fechaSubidaS3").exists());

        verify(s3).subir(eq(ruta(ticket.getId())), any(byte[].class), eq("application/pdf"));
        assertThat(Files.exists(Path.of(efsPath, ticket.getRutaArchivo()))).isFalse();
    }

    @Test
    void subirPendientesSubeTodosLosQueEstanEnEfs() throws Exception {
        mvc.perform(post("/api/compras/{id}/tickets", compraId).param("subirAutomaticamente", "false"));

        mvc.perform(post("/api/tickets/s3/pendientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        assertThat(ticketRepository.findAll()).allMatch(t -> t.getEstado() == EstadoTicket.SUBIDO_S3);
    }

    @Test
    void historialFiltraPorEventoUsuarioYEstado() throws Exception {
        mvc.perform(post("/api/compras/{id}/tickets", compraId));

        mvc.perform(get("/api/tickets").param("eventoId", "7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/tickets").param("usuario", "CAMILA@correo.cl"))   // no distingue mayusculas
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/tickets").param("eventoId", "99"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/tickets").param("estado", "GENERADO_EFS"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void descargarTicketDesdeS3() throws Exception {
        mvc.perform(post("/api/compras/{id}/tickets", compraId));
        Ticket ticket = ticketRepository.findAll().get(0);
        when(s3.descargar(ruta(ticket.getId()))).thenReturn("%PDF-ticket".getBytes());

        mvc.perform(get("/api/tickets/{id}/pdf", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("%PDF-ticket".getBytes()));
    }

    @Test
    void descargarZipPorEvento() throws Exception {
        mvc.perform(post("/api/compras/{id}/tickets", compraId));
        when(s3.descargar(anyString())).thenReturn("%PDF-ticket".getBytes());

        byte[] zip = mvc.perform(get("/api/tickets/descargar").param("eventoId", "7"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(zip, 0, 2)).isEqualTo("PK"); // todo ZIP comienza con "PK"
    }

    @Test
    void descargarZipSinFiltrosRetorna400() throws Exception {
        mvc.perform(get("/api/tickets/descargar")).andExpect(status().isBadRequest());
    }

    @Test
    void modificarTicketActualizaElPdfEnS3() throws Exception {
        mvc.perform(post("/api/compras/{id}/tickets", compraId));
        Ticket ticket = ticketRepository.findAll().get(0);
        clearInvocations(s3);

        mvc.perform(put("/api/tickets/{id}", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreAsistente\":\"Pedro Soto\",\"sector\":\"VIP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreAsistente").value("Pedro Soto"))
                .andExpect(jsonPath("$.sector").value("VIP"))
                .andExpect(jsonPath("$.fechaActualizacion").exists());

        // se sobrescribe el mismo objeto en S3 (misma ruta)
        verify(s3).subir(eq(ruta(ticket.getId())), any(byte[].class), eq("application/pdf"));
    }

    @Test
    void modificarConDatosInvalidosRetorna400() throws Exception {
        mvc.perform(post("/api/compras/{id}/tickets", compraId));
        Ticket ticket = ticketRepository.findAll().get(0);

        mvc.perform(put("/api/tickets/{id}", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreAsistente\":\"\",\"sector\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarTicketBorraElArchivoDeS3YElRegistro() throws Exception {
        mvc.perform(post("/api/compras/{id}/tickets", compraId));
        Ticket ticket = ticketRepository.findAll().get(0);

        mvc.perform(delete("/api/tickets/{id}", ticket.getId()))
                .andExpect(status().isOk());

        verify(s3).eliminar(ruta(ticket.getId()));
        assertThat(ticketRepository.findById(ticket.getId())).isEmpty();

        mvc.perform(delete("/api/tickets/{id}", ticket.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void estadisticasPorEvento() throws Exception {
        when(eventosClient.obtenerEvento(7L)).thenReturn(new EventoInfoDto(7L, "Festival de Jazz", 100, 98));
        mvc.perform(post("/api/compras/{id}/tickets", compraId));

        mvc.perform(get("/api/estadisticas/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventoId").value(7))
                .andExpect(jsonPath("$[0].comprasRealizadas").value(1))
                .andExpect(jsonPath("$[0].entradasVendidas").value(2))
                .andExpect(jsonPath("$[0].recaudacionTotal").value(36000))
                .andExpect(jsonPath("$[0].ticketsGenerados").value(2))
                .andExpect(jsonPath("$[0].ticketsEnS3").value(2))
                .andExpect(jsonPath("$[0].porcentajeOcupacion").value(2.0));

        mvc.perform(get("/api/estadisticas/eventos/{id}", 7)).andExpect(status().isOk());
        mvc.perform(get("/api/estadisticas/eventos/{id}", 99)).andExpect(status().isNotFound());
    }
}
