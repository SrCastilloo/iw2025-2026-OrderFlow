package es.uca.orderflow.persistence.data;

import org.springframework.data.jpa.repository.JpaRepository;

import es.uca.orderflow.business.entities.Detalle_Pedido;
import es.uca.orderflow.business.entities.Pedido;

import java.util.Set;

public interface Detalle_PedidoRepository extends JpaRepository<Detalle_Pedido, Long>{

    Set<Detalle_Pedido> findByPedido(Pedido p);  //detalles de un pedido  
    void deleteByPedido_id(Long id); //eliminar todos los registros perteneciente a un pedido
}
