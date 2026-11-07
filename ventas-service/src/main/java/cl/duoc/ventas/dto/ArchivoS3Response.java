package cl.duoc.ventas.dto;

/**
 * Respuesta de las operaciones sobre el resumen en S3: donde quedo el archivo y que paso.
 */
public record ArchivoS3Response(
        Long idCompra,
        String bucket,
        String key,
        long tamanoBytes,
        String mensaje
) {
}
