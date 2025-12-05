package es.uca.orderflow.persistence.data;
import es.uca.orderflow.business.entities.Carrito;
import es.uca.orderflow.business.entities.Detalle_Carrito;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional; 
public interface  Detalle_CarritoRepository extends JpaRepository<Detalle_Carrito, Long> {
        
    //borrar todos los productos de un carrito
    void deleteByCarrito_Id(Long carritoId);


    //todos los detalles de un carrito
    List<Detalle_Carrito> findByCarrito_Id(Long carritoId);
    Optional<Detalle_Carrito> findByCarrito_IdAndProducto_Id(Long carritoId, Long productoId);
    int countByCarrito_Id(Long carritoId);
    @Query("select coalesce(sum(d.subtotal), 0) from Detalle_Carrito d where d.carrito.id = :carritoId")
    BigDecimal sumSubtotalByCarritoId(@Param("carritoId") Long carritoId);


    List<Detalle_Carrito> findByCarrito(Carrito carrito);

}
