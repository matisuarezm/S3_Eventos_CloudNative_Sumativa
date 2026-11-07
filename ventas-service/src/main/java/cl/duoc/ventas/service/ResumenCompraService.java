package cl.duoc.ventas.service;

import cl.duoc.ventas.dto.ArchivoS3Response;
import cl.duoc.ventas.exception.ArchivoInvalidoException;
import cl.duoc.ventas.exception.RecursoNoEncontradoException;
import cl.duoc.ventas.model.Compra;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Logica del resumen de compra: genera el PDF y lo administra en S3.
 *
 * Estructura en el bucket (una carpeta por resumen, con el numero de la compra):
 *   resumenes/{idCompra}/resumen-compra-{idCompra}.pdf
 */
@Service
public class ResumenCompraService {

    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024; // 5 MB

    private final CompraService compraService;
    private final ResumenPdfService pdfService;
    private final AlmacenamientoS3Service almacenamiento;

    public ResumenCompraService(CompraService compraService,
                                ResumenPdfService pdfService,
                                AlmacenamientoS3Service almacenamiento) {
        this.compraService = compraService;
        this.pdfService = pdfService;
        this.almacenamiento = almacenamiento;
    }

    /** Nombre del archivo que ve el usuario al descargar. */
    public String nombreArchivo(Long idCompra) {
        return "resumen-compra-" + idCompra + ".pdf";
    }

    /** Ruta (key) del archivo dentro del bucket: carpeta = numero del resumen. */
    public String rutaEnS3(Long idCompra) {
        return "resumenes/" + idCompra + "/" + nombreArchivo(idCompra);
    }

    /** Requisito 1: genera el PDF para descargarlo directamente (sin pasar por S3). */
    public byte[] generar(Long idCompra) {
        Compra compra = compraService.buscarEntidad(idCompra);
        return pdfService.generar(compra);
    }

    /** Requisito 2: genera el PDF y lo sube a S3 en la carpeta de la compra. */
    public ArchivoS3Response subir(Long idCompra) {
        byte[] pdf = generar(idCompra);
        String key = rutaEnS3(idCompra);
        almacenamiento.subir(key, pdf, CONTENT_TYPE_PDF);
        return respuesta(idCompra, key, pdf.length, "Resumen subido a S3");
    }

    /** Requisito 3a: descarga el resumen guardado en S3. */
    public byte[] descargar(Long idCompra) {
        String key = validarExistencia(idCompra);
        return almacenamiento.descargar(key);
    }

    /**
     * Requisito 3b: modifica el resumen existente en S3.
     * - Si se envia un archivo PDF, reemplaza el que estaba.
     * - Si no se envia archivo, se regenera con los datos actuales de la compra
     *   (por ejemplo, despues de corregir el nombre o email del cliente).
     */
    public ArchivoS3Response actualizar(Long idCompra, MultipartFile archivo) {
        String key = validarExistencia(idCompra);
        byte[] contenido = (archivo == null || archivo.isEmpty())
                ? generar(idCompra)
                : leerPdf(archivo);
        almacenamiento.subir(key, contenido, CONTENT_TYPE_PDF);
        String mensaje = (archivo == null || archivo.isEmpty())
                ? "Resumen regenerado con los datos actuales de la compra"
                : "Resumen reemplazado por el archivo enviado";
        return respuesta(idCompra, key, contenido.length, mensaje);
    }

    /** Requisito 3c: elimina el resumen de S3, validando primero que exista. */
    public ArchivoS3Response eliminar(Long idCompra) {
        String key = validarExistencia(idCompra);
        almacenamiento.eliminar(key);
        return respuesta(idCompra, key, 0, "Resumen eliminado de S3");
    }

    /** Verifica que la compra exista en la BD y que su resumen exista en S3. Devuelve la key. */
    private String validarExistencia(Long idCompra) {
        compraService.buscarEntidad(idCompra);
        String key = rutaEnS3(idCompra);
        if (!almacenamiento.existe(key)) {
            throw new RecursoNoEncontradoException(
                    "El resumen de la compra " + idCompra + " no existe en S3. Primero subalo con POST.");
        }
        return key;
    }

    private byte[] leerPdf(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase();
        boolean esPdf = CONTENT_TYPE_PDF.equals(archivo.getContentType()) || nombre.endsWith(".pdf");
        if (!esPdf) {
            throw new ArchivoInvalidoException("El archivo debe ser un PDF");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new ArchivoInvalidoException("El archivo no puede superar 5 MB");
        }
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new ArchivoInvalidoException("No se pudo leer el archivo enviado");
        }
    }

    private ArchivoS3Response respuesta(Long idCompra, String key, long tamano, String mensaje) {
        return new ArchivoS3Response(idCompra, almacenamiento.getBucket(), key, tamano, mensaje);
    }
}
