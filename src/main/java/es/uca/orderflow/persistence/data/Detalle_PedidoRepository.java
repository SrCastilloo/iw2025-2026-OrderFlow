package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.Detalle_Pedido;
import es.uca.orderflow.business.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface Detalle_PedidoRepository extends JpaRepository<Detalle_Pedido, Long> {

    Set<Detalle_Pedido> findByPedido(Pedido p);

    void deleteByPedido_Id(Long id);

    @Query("select (count(dp) > 0) from Detalle_Pedido dp where dp.producto.id = :productoId")
    boolean existsAnyByProductoId(@Param("productoId") Long productoId);
}
