package es.uca.orderflow.persistence.data;
import org.springframework.data.jpa.repository.JpaRepository;

import es.uca.orderflow.business.entities.Producto;


public interface ProductoRepository extends JpaRepository<Producto,Long> {}
