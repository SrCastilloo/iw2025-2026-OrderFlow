package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.Detalle_Pedido;
import es.uca.orderflow.business.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface Detalle_PedidoRepository extends JpaRepository<Detalle_Pedido, Long> {

    Set<Detalle_Pedido> findByPedido(Pedido p);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Detalle_Pedido dp where dp.pedido.id = :pedidoId")
    void deleteByPedido_Id(@Param("pedidoId") Long pedidoId);


    @Query(value = "select count(1) from detalle_pedido where producto_id = :productoId", nativeQuery = true)
    long countByProductoIdNative(@Param("productoId") Long productoId);
    
    @Query("select (count(dp) > 0) from Detalle_Pedido dp where dp.producto.id = :productoId")
    boolean existsAnyByProductoId(@Param("productoId") Long productoId);
}
