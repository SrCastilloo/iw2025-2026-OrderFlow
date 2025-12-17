package es.uca.orderflow.persistence.data;
import es.uca.orderflow.business.entities.ProductoTipo;
import org.springframework.data.jpa.repository.JpaRepository;

import es.uca.orderflow.business.entities.Producto;

import java.util.List;


public interface ProductoRepository extends JpaRepository<Producto,Long> {
    List<Producto> findByTipo(ProductoTipo tipo);
    List<Producto> findByActivoTrue();
    List<Producto> findByTipoAndActivoTrue(ProductoTipo tipo);

}
