package es.uca.orderflow.persistence.data;
import org.springframework.data.jpa.repository.JpaRepository;

import es.uca.orderflow.business.entities.Pedido;
import es.uca.orderflow.business.entities.Cliente;

import java.util.Set;

public interface PedidoRepository extends JpaRepository<Pedido,Long> {

    Set<Pedido> findByCliente(Cliente c); //buscar los pedidos de un cliente
}
