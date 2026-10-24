package cl.duoc.ventas;

import cl.duoc.ventas.client.EventosClient;
import cl.duoc.ventas.dto.EventoDto;
import cl.duoc.ventas.exception.EntradasInsuficientesException;
import cl.duoc.ventas.exception.RecursoNoEncontradoException;
import cl.duoc.ventas.exception.ServicioNoDisponibleException;
import cl.duoc.ventas.repository.CompraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompraControllerTests {

    private static final String COMPRA_JSON = """
            {"eventoId":1,"cantidad":3,"nombreCliente":"Camila Rojas","emailCliente":"camila@correo.cl"}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    CompraRepository repository;

    /** Se simula eventos-service para probar ventas-service de forma aislada. */
    @MockBean
    EventosClient eventosClient;

    @BeforeEach
    void limpiar() {
        repository.deleteAll();
    }

    @Test
    void comprarEntradasDevuelveResumenYGuardaLaCompra() throws Exception {
        when(eventosClient.reservarEntradas(eq(1L), eq(3))).thenReturn(
                new EventoDto(1L, "Concierto Rock", LocalDateTime.now().plusDays(20), 25000L, 97));

        mvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON).content(COMPRA_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCompra").exists())
                .andExpect(jsonPath("$.evento").value("Concierto Rock"))
                .andExpect(jsonPath("$.cantidadEntradas").value(3))
                .andExpect(jsonPath("$.precioUnitario").value(25000))
                .andExpect(jsonPath("$.precioTotal").value(75000));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void comprarEventoInexistenteRetorna404() throws Exception {
        when(eventosClient.reservarEntradas(anyLong(), anyInt()))
                .thenThrow(new RecursoNoEncontradoException("No existe el evento con id 1"));

        mvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON).content(COMPRA_JSON))
                .andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }

    @Test
    void comprarSinEntradasSuficientesRetorna409() throws Exception {
        when(eventosClient.reservarEntradas(anyLong(), anyInt()))
                .thenThrow(new EntradasInsuficientesException("No hay entradas suficientes"));

        mvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON).content(COMPRA_JSON))
                .andExpect(status().isConflict());
        assertThat(repository.count()).isZero();
    }

    @Test
    void eventosServiceCaidoRetorna503() throws Exception {
        when(eventosClient.reservarEntradas(anyLong(), anyInt()))
                .thenThrow(new ServicioNoDisponibleException("timeout"));

        mvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON).content(COMPRA_JSON))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void compraInvalidaRetorna400SinLlamarAEventos() throws Exception {
        String json = """
                {"eventoId":1,"cantidad":0,"nombreCliente":"","emailCliente":"no-es-email"}
                """;
        mvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cantidad").exists())
                .andExpect(jsonPath("$.campos.nombreCliente").exists())
                .andExpect(jsonPath("$.campos.emailCliente").exists());
        verifyNoInteractions(eventosClient);
    }

    @Test
    void listarYConsultarCompras() throws Exception {
        when(eventosClient.reservarEntradas(anyLong(), anyInt())).thenReturn(
                new EventoDto(1L, "Concierto Rock", LocalDateTime.now().plusDays(20), 25000L, 97));
        mvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON).content(COMPRA_JSON))
                .andExpect(status().isCreated());

        Long id = repository.findAll().get(0).getId();

        mvc.perform(get("/api/compras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/compras/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioTotal").value(75000));
        mvc.perform(get("/api/compras/{id}", 99999))
                .andExpect(status().isNotFound());
    }
}
