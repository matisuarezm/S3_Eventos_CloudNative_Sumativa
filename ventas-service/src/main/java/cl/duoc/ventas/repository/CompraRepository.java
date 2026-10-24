package cl.duoc.ventas.repository;

import cl.duoc.ventas.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findAllByOrderByFechaCompraDesc();
}
