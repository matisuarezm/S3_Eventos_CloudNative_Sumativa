package cl.duoc.ventas.controller;

import cl.duoc.ventas.dto.ArchivoS3Response;
import cl.duoc.ventas.service.ResumenCompraService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints del resumen de compra (Semana 2).
 *
 *   GET    /api/compras/{id}/resumen      genera el PDF y lo descarga
 *   POST   /api/compras/{id}/resumen/s3   genera el PDF y lo sube a S3
 *   GET    /api/compras/{id}/resumen/s3   descarga el PDF guardado en S3
 *   PUT    /api/compras/{id}/resumen/s3   modifica (reemplaza o regenera) el PDF en S3
 *   DELETE /api/compras/{id}/resumen/s3   elimina el PDF de S3
 */
@RestController
@RequestMapping("/api/compras/{id}/resumen")
public class ResumenCompraController {

    private final ResumenCompraService service;

    public ResumenCompraController(ResumenCompraService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<byte[]> generar(@PathVariable Long id) {
        return pdf(service.generar(id), service.nombreArchivo(id));
    }

    @PostMapping("/s3")
    public ResponseEntity<ArchivoS3Response> subir(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.subir(id));
    }

    @GetMapping("/s3")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        return pdf(service.descargar(id), service.nombreArchivo(id));
    }

    @PutMapping("/s3")
    public ArchivoS3Response actualizar(@PathVariable Long id,
                                        @RequestParam(value = "archivo", required = false) MultipartFile archivo) {
        return service.actualizar(id, archivo);
    }

    @DeleteMapping("/s3")
    public ArchivoS3Response eliminar(@PathVariable Long id) {
        return service.eliminar(id);
    }

    /** Respuesta HTTP con un PDF que el navegador o Postman guardan como archivo. */
    private ResponseEntity<byte[]> pdf(byte[] contenido, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(contenido.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .body(contenido);
    }
}
