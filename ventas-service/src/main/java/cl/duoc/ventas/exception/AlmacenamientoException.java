package cl.duoc.ventas.exception;

/** Error al comunicarse con Amazon S3 (credenciales vencidas, bucket inexistente, red, etc.). */
public class AlmacenamientoException extends RuntimeException {
    public AlmacenamientoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
