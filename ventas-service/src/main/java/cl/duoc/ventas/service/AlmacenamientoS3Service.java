package cl.duoc.ventas.service;

import cl.duoc.ventas.exception.AlmacenamientoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Unica clase que conversa con Amazon S3.
 * El resto del codigo no conoce el SDK de AWS: solo pide "subir", "descargar", "existe" o "eliminar".
 */
@Service
public class AlmacenamientoS3Service {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoS3Service.class);

    private final S3Client s3;
    private final String bucket;

    public AlmacenamientoS3Service(S3Client s3, @Value("${app.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public String getBucket() {
        return bucket;
    }

    /** Sube (o sobrescribe) un archivo en la ruta indicada. */
    public void subir(String key, byte[] contenido, String contentType) {
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(contenido));
            log.info("Archivo subido a s3://{}/{} ({} bytes)", bucket, key, contenido.length);
        } catch (SdkException e) {
            throw new AlmacenamientoException("No se pudo subir el archivo a S3: " + e.getMessage(), e);
        }
    }

    /** Descarga el contenido completo de un archivo. */
    public byte[] descargar(String key) {
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build())
                    .asByteArray();
        } catch (SdkException e) {
            throw new AlmacenamientoException("No se pudo descargar el archivo de S3: " + e.getMessage(), e);
        }
    }

    /** Consulta si existe un archivo, sin descargarlo (HEAD). */
    public boolean existe(String key) {
        try {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new AlmacenamientoException("No se pudo consultar el archivo en S3: " + e.getMessage(), e);
        } catch (SdkException e) {
            throw new AlmacenamientoException("No se pudo consultar el archivo en S3: " + e.getMessage(), e);
        }
    }

    /** Elimina un archivo. */
    public void eliminar(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            log.info("Archivo eliminado de s3://{}/{}", bucket, key);
        } catch (SdkException e) {
            throw new AlmacenamientoException("No se pudo eliminar el archivo de S3: " + e.getMessage(), e);
        }
    }
}
