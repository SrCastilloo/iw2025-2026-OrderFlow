package es.uca.orderflow.persistence.data;
import es.uca.orderflow.business.entities.Detalle_Carrito;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface  Detalle_CarritoRepository extends JpaRepository<Detalle_Carrito, Long> {
        
    //borrar todos los productos de un carrito
    void deleteByCarrito_Id(Long carritoId);


    //todos los detalles de un carrito
    List<Detalle_Carrito> findByCarrito_Id(Long carritoId);

}
