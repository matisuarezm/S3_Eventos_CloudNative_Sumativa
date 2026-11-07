package cl.duoc.ventas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Ticket personalizado de acceso a un evento. Se genera uno por cada entrada de una compra.
 * La tabla TICKETS guarda los datos y el historial (donde esta el archivo y cuando se movio).
 */
@Entity
@Table(name = "TICKETS")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Codigo legible del ticket, por ejemplo TK-15-2 (compra 15, entrada 2). */
    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "COMPRA_ID", nullable = false)
    private Long compraId;

    @Column(name = "EVENTO_ID", nullable = false)
    private Long eventoId;

    @Column(name = "NOMBRE_EVENTO", nullable = false, length = 150)
    private String nombreEvento;

    @Column(name = "FECHA_EVENTO", nullable = false)
    private LocalDateTime fechaEvento;

    /** Usuario dueno del ticket: el email del cliente que compro. */
    @Column(nullable = false, length = 150)
    private String usuario;

    /** Nombre impreso en el ticket (personalizable). */
    @Column(name = "NOMBRE_ASISTENTE", nullable = false, length = 120)
    private String nombreAsistente;

    @Column(nullable = false, length = 40)
    private String sector;

    @Column(name = "NUMERO_ENTRADA", nullable = false)
    private Integer numeroEntrada;

    @Column(name = "TOTAL_ENTRADAS", nullable = false)
    private Integer totalEntradas;

    @Column(nullable = false)
    private Long precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTicket estado;

    /** Ruta relativa del archivo dentro del EFS (y tambien key en S3). */
    @Column(name = "RUTA_ARCHIVO", length = 300)
    private String rutaArchivo;

    @Column(name = "FECHA_GENERACION", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "FECHA_ACTUALIZACION")
    private LocalDateTime fechaActualizacion;

    @Column(name = "FECHA_SUBIDA_S3")
    private LocalDateTime fechaSubidaS3;

    public Ticket() {
    }

    /** Crea el ticket numero {numero} de una compra, con los datos del comprador. */
    public static Ticket desdeCompra(Compra compra, int numero) {
        Ticket t = new Ticket();
        t.codigo = "TK-" + compra.getId() + "-" + numero;
        t.compraId = compra.getId();
        t.eventoId = compra.getEventoId();
        t.nombreEvento = compra.getNombreEvento();
        t.fechaEvento = compra.getFechaEvento();
        t.usuario = compra.getEmailCliente();
        t.nombreAsistente = compra.getNombreCliente();
        t.sector = "General";
        t.numeroEntrada = numero;
        t.totalEntradas = compra.getCantidadEntradas();
        t.precio = compra.getPrecioUnitario();
        t.estado = EstadoTicket.GENERADO_EFS;
        t.fechaGeneracion = LocalDateTime.now();
        return t;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Long getCompraId() { return compraId; }
    public void setCompraId(Long compraId) { this.compraId = compraId; }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public String getNombreEvento() { return nombreEvento; }
    public void setNombreEvento(String nombreEvento) { this.nombreEvento = nombreEvento; }

    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDateTime fechaEvento) { this.fechaEvento = fechaEvento; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getNombreAsistente() { return nombreAsistente; }
    public void setNombreAsistente(String nombreAsistente) { this.nombreAsistente = nombreAsistente; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public Integer getNumeroEntrada() { return numeroEntrada; }
    public void setNumeroEntrada(Integer numeroEntrada) { this.numeroEntrada = numeroEntrada; }

    public Integer getTotalEntradas() { return totalEntradas; }
    public void setTotalEntradas(Integer totalEntradas) { this.totalEntradas = totalEntradas; }

    public Long getPrecio() { return precio; }
    public void setPrecio(Long precio) { this.precio = precio; }

    public EstadoTicket getEstado() { return estado; }
    public void setEstado(EstadoTicket estado) { this.estado = estado; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }

    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public LocalDateTime getFechaSubidaS3() { return fechaSubidaS3; }
    public void setFechaSubidaS3(LocalDateTime fechaSubidaS3) { this.fechaSubidaS3 = fechaSubidaS3; }
}
