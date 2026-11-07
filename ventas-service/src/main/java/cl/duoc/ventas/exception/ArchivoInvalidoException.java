package cl.duoc.ventas.exception;

/** El archivo enviado no cumple las reglas (no es PDF, es muy grande, etc.). */
public class ArchivoInvalidoException extends RuntimeException {
    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
