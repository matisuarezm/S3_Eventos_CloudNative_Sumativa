package cl.duoc.eventos;

import cl.duoc.eventos.model.Evento;
import cl.duoc.eventos.repository.EventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventoControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    EventoRepository repository;

    private Evento futuro;

    @BeforeEach
    void preparar() {
        repository.deleteAll();
        futuro = repository.save(new Evento("Concierto", "desc", "Santiago",
                LocalDateTime.now().plusDays(10), 20000L, 100));
        repository.save(new Evento("Evento pasado", "desc", "Santiago",
                LocalDateTime.now().minusDays(1), 10000L, 100));
        Evento agotado = new Evento("Agotado", "desc", "Santiago",
                LocalDateTime.now().plusDays(5), 10000L, 10);
        agotado.setEntradasDisponibles(0);
        repository.save(agotado);
    }

    @Test
    void listarSoloDevuelveEventosDisponibles() throws Exception {
        mvc.perform(get("/api/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Concierto"))
                .andExpect(jsonPath("$[0].precioEntrada").value(20000));
    }

    @Test
    void listarTodosDevuelveTodosLosEventos() throws Exception {
        mvc.perform(get("/api/eventos").param("todos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void crearEventoRetorna201() throws Exception {
        String json = """
                {"nombre":"Feria Tech","descripcion":"Charlas","lugar":"Espacio Riesco",
                 "fecha":"%s","precioEntrada":15000,"capacidad":200}
                """.formatted(LocalDateTime.now().plusMonths(2).withNano(0));
        mvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.entradasDisponibles").value(200));
    }

    @Test
    void crearEventoInvalidoRetorna400() throws Exception {
        String json = """
                {"nombre":"","lugar":"X","fecha":"2020-01-01T10:00:00","precioEntrada":0,"capacidad":0}
                """;
        mvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.fecha").exists())
                .andExpect(jsonPath("$.campos.precioEntrada").exists());
    }

    @Test
    void obtenerEventoInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/eventos/{id}", 99999))
                .andExpect(status().isNotFound());
    }

    @Test
    void reservarDescuentaEntradas() throws Exception {
        mvc.perform(post("/api/eventos/{id}/reservas", futuro.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entradasDisponibles").value(97));
    }

    @Test
    void reservarMasEntradasQueLasDisponiblesRetorna409() throws Exception {
        mvc.perform(post("/api/eventos/{id}/reservas", futuro.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":101}"))
                .andExpect(status().isConflict());
    }
}
