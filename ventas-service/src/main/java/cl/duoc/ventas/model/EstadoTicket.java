package cl.duoc.ventas.model;

/**
 * Ciclo de vida de un ticket:
 * GENERADO_EFS -> el PDF esta en el almacenamiento temporal (EFS)
 * SUBIDO_S3    -> el PDF ya esta en el almacenamiento definitivo (S3) y se borro de EFS
 */
public enum EstadoTicket {
    GENERADO_EFS,
    SUBIDO_S3
}
