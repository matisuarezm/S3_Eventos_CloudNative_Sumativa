package cl.duoc.eventos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Evento organizado por la empresa. Se persiste en la tabla EVENTOS de Oracle Cloud.
 */
@Entity
@Table(name = "EVENTOS")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 150)
    private String lugar;

    @Column(name = "FECHA_EVENTO", nullable = false)
    private LocalDateTime fecha;

    /** Precio de una entrada en pesos chilenos (CLP, sin decimales). */
    @Column(name = "PRECIO_ENTRADA", nullable = false)
    private Long precioEntrada;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(name = "ENTRADAS_DISPONIBLES", nullable = false)
    private Integer entradasDisponibles;

    public Evento() {
    }

    public Evento(String nombre, String descripcion, String lugar, LocalDateTime fecha,
                  Long precioEntrada, Integer capacidad) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.fecha = fecha;
        this.precioEntrada = precioEntrada;
        this.capacidad = capacidad;
        this.entradasDisponibles = capacidad;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Long getPrecioEntrada() { return precioEntrada; }
    public void setPrecioEntrada(Long precioEntrada) { this.precioEntrada = precioEntrada; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public Integer getEntradasDisponibles() { return entradasDisponibles; }
    public void setEntradasDisponibles(Integer entradasDisponibles) { this.entradasDisponibles = entradasDisponibles; }
}
