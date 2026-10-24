package cl.duoc.ventas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Compra de entradas para un evento. Se persiste en la tabla COMPRAS de Oracle Cloud.
 * Guarda una copia del nombre y precio del evento al momento de la compra,
 * asi el resumen no cambia si despues se modifica el evento.
 */
@Entity
@Table(name = "COMPRAS")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "EVENTO_ID", nullable = false)
    private Long eventoId;

    @Column(name = "NOMBRE_EVENTO", nullable = false, length = 150)
    private String nombreEvento;

    @Column(name = "FECHA_EVENTO", nullable = false)
    private LocalDateTime fechaEvento;

    @Column(name = "NOMBRE_CLIENTE", nullable = false, length = 120)
    private String nombreCliente;

    @Column(name = "EMAIL_CLIENTE", nullable = false, length = 150)
    private String emailCliente;

    @Column(name = "CANTIDAD_ENTRADAS", nullable = false)
    private Integer cantidadEntradas;

    @Column(name = "PRECIO_UNITARIO", nullable = false)
    private Long precioUnitario;

    @Column(name = "PRECIO_TOTAL", nullable = false)
    private Long precioTotal;

    @Column(name = "FECHA_COMPRA", nullable = false)
    private LocalDateTime fechaCompra;

    public Compra() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public String getNombreEvento() { return nombreEvento; }
    public void setNombreEvento(String nombreEvento) { this.nombreEvento = nombreEvento; }

    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDateTime fechaEvento) { this.fechaEvento = fechaEvento; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getEmailCliente() { return emailCliente; }
    public void setEmailCliente(String emailCliente) { this.emailCliente = emailCliente; }

    public Integer getCantidadEntradas() { return cantidadEntradas; }
    public void setCantidadEntradas(Integer cantidadEntradas) { this.cantidadEntradas = cantidadEntradas; }

    public Long getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Long precioUnitario) { this.precioUnitario = precioUnitario; }

    public Long getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(Long precioTotal) { this.precioTotal = precioTotal; }

    public LocalDateTime getFechaCompra() { return fechaCompra; }
    public void setFechaCompra(LocalDateTime fechaCompra) { this.fechaCompra = fechaCompra; }
}
