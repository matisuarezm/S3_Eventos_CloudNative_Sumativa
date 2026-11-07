package cl.duoc.ventas.exception;

/** La operacion choca con el estado actual (ej: tickets ya generados). Responde 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
