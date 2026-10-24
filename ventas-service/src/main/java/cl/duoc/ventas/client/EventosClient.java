package cl.duoc.ventas.client;

import cl.duoc.ventas.dto.EventoDto;
import cl.duoc.ventas.exception.EntradasInsuficientesException;
import cl.duoc.ventas.exception.RecursoNoEncontradoException;
import cl.duoc.ventas.exception.ServicioNoDisponibleException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.Map;

/**
 * Cliente HTTP hacia eventos-service (comunicacion entre microservicios).
 * La URL se configura con la variable de entorno EVENTOS_URL.
 */
@Component
public class EventosClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public EventosClient(RestClient.Builder builder,
                         ObjectMapper objectMapper,
                         @Value("${eventos.url}") String eventosUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restClient = builder.baseUrl(eventosUrl).requestFactory(factory).build();
        this.objectMapper = objectMapper;
    }

    /**
     * Pide a eventos-service que descuente entradas del evento.
     * Devuelve los datos del evento (nombre, fecha, precio) para armar el resumen de compra.
     */
    public EventoDto reservarEntradas(Long eventoId, int cantidad) {
        try {
            return restClient.post()
                    .uri("/api/eventos/{id}/reservas", eventoId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("cantidad", cantidad))
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                        throw new RecursoNoEncontradoException("No existe el evento con id " + eventoId);
                    })
                    .onStatus(status -> status.value() == HttpStatus.CONFLICT.value(), (request, response) -> {
                        throw new EntradasInsuficientesException(leerMensaje(response));
                    })
                    .body(EventoDto.class);
        } catch (ResourceAccessException ex) {
            throw new ServicioNoDisponibleException("eventos-service no responde: " + ex.getMessage());
        } catch (RestClientResponseException ex) {
            throw new ServicioNoDisponibleException(
                    "eventos-service respondio con error " + ex.getStatusCode().value());
        }
    }

    private String leerMensaje(ClientHttpResponse response) {
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.path("mensaje").asText("No hay entradas suficientes");
        } catch (IOException e) {
            return "No hay entradas suficientes";
        }
    }
}
