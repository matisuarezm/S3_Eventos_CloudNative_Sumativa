package cl.duoc.ventas;

import cl.duoc.ventas.client.EventosClient;
import cl.duoc.ventas.model.Compra;
import cl.duoc.ventas.repository.CompraRepository;
import cl.duoc.ventas.service.AlmacenamientoS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los endpoints del resumen de compra.
 * S3 se simula con @MockBean: las pruebas no necesitan credenciales de AWS.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResumenCompraControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    CompraRepository repository;

    @MockBean
    AlmacenamientoS3Service almacenamiento;

    @MockBean
    EventosClient eventosClient;

    private Long idCompra;
    private String key;

    @BeforeEach
    void preparar() {
        repository.deleteAll();
        Compra compra = new Compra();
        compra.setEventoId(1L);
        compra.setNombreEvento("Concierto Rock");
        compra.setFechaEvento(LocalDateTime.now().plusDays(20));
        compra.setNombreCliente("Camila Rojas");
        compra.setEmailCliente("camila@correo.cl");
        compra.setCantidadEntradas(3);
        compra.setPrecioUnitario(25000L);
        compra.setPrecioTotal(75000L);
        compra.setFechaCompra(LocalDateTime.now());
        idCompra = repository.save(compra).getId();
        key = "resumenes/" + idCompra + "/resumen-compra-" + idCompra + ".pdf";
        when(almacenamiento.getBucket()).thenReturn("bucket-test");
    }

    @Test
    void generarResumenDevuelveUnPdf() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/compras/{id}/resumen", idCompra))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"resumen-compra-" + idCompra + ".pdf\""))
                .andReturn();

        byte[] pdf = resultado.getResponse().getContentAsByteArray();
        // Todo PDF valido comienza con "%PDF"
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generarResumenDeCompraInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/compras/{id}/resumen", 99999))
                .andExpect(status().isNotFound());
    }

    @Test
    void subirResumenLoGuardaEnLaCarpetaDeLaCompra() throws Exception {
        mvc.perform(post("/api/compras/{id}/resumen/s3", idCompra))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.bucket").value("bucket-test"));

        verify(almacenamiento).subir(eq(key), any(byte[].class), eq("application/pdf"));
    }

    @Test
    void descargarResumenDesdeS3() throws Exception {
        when(almacenamiento.existe(key)).thenReturn(true);
        when(almacenamiento.descargar(key)).thenReturn("%PDF-prueba".getBytes());

        mvc.perform(get("/api/compras/{id}/resumen/s3", idCompra))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("%PDF-prueba".getBytes()));
    }

    @Test
    void descargarResumenQueNoEstaEnS3Retorna404() throws Exception {
        when(almacenamiento.existe(key)).thenReturn(false);

        mvc.perform(get("/api/compras/{id}/resumen/s3", idCompra))
                .andExpect(status().isNotFound());
    }

    @Test
    void reemplazarResumenConUnPdfEnviado() throws Exception {
        when(almacenamiento.existe(key)).thenReturn(true);
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "corregido.pdf", "application/pdf", "%PDF-corregido".getBytes());

        mvc.perform(multipart(HttpMethod.PUT, "/api/compras/{id}/resumen/s3", idCompra).file(archivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(key));

        verify(almacenamiento).subir(key, "%PDF-corregido".getBytes(), "application/pdf");
    }

    @Test
    void regenerarResumenSinArchivo() throws Exception {
        when(almacenamiento.existe(key)).thenReturn(true);

        mvc.perform(put("/api/compras/{id}/resumen/s3", idCompra))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Resumen regenerado con los datos actuales de la compra"));

        verify(almacenamiento).subir(eq(key), any(byte[].class), eq("application/pdf"));
    }

    @Test
    void reemplazarConArchivoQueNoEsPdfRetorna400() throws Exception {
        when(almacenamiento.existe(key)).thenReturn(true);
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.png", "image/png", new byte[]{1, 2, 3});

        mvc.perform(multipart(HttpMethod.PUT, "/api/compras/{id}/resumen/s3", idCompra).file(archivo))
                .andExpect(status().isBadRequest());

        verify(almacenamiento, never()).subir(anyString(), any(byte[].class), anyString());
    }

    @Test
    void eliminarResumenExistente() throws Exception {
        when(almacenamiento.existe(key)).thenReturn(true);

        mvc.perform(delete("/api/compras/{id}/resumen/s3", idCompra))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Resumen eliminado de S3"));

        verify(almacenamiento).eliminar(key);
    }

    @Test
    void eliminarResumenInexistenteRetorna404() throws Exception {
        when(almacenamiento.existe(key)).thenReturn(false);

        mvc.perform(delete("/api/compras/{id}/resumen/s3", idCompra))
                .andExpect(status().isNotFound());

        verify(almacenamiento, never()).eliminar(anyString());
    }

    @Test
    void corregirDatosDelCliente() throws Exception {
        mvc.perform(put("/api/compras/{id}", idCompra)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCliente\":\"Camila Rojas Perez\",\"emailCliente\":\"CRojas@Correo.cl\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCliente").value("Camila Rojas Perez"))
                .andExpect(jsonPath("$.emailCliente").value("crojas@correo.cl"));
    }
}
