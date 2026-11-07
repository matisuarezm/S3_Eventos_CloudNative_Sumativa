package cl.duoc.ventas.exception;

/** Parametros de la peticion invalidos o incompletos. Responde 400. */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
