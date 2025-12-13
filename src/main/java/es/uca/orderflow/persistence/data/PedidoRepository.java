package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.services.PedidoEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import es.uca.orderflow.business.entities.Pedido;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.entities.Empleado;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Set;

public interface PedidoRepository extends JpaRepository<Pedido,Long> {

    Set<Pedido> findByCliente(Cliente c);
    Set<Pedido> findByEstado(PedidoEstado estado);

    /**
     * ⭐ MÉTODO AÑADIDO: Busca pedidos por repartidor y estado.
     * Necesario para el filtro "Mi reparto activo" y para el control de bloqueo.
     */
    Set<Pedido> findByRepartidorAndEstado(Empleado repartidor, PedidoEstado estado);
    void deleteByClienteId(Long id);
    Set<Pedido> findByClienteId(Long id);

    @Query("""
   select p from Pedido p
   where p.fechaRealizacion between :from and :to
     and p.paymentStatus = 'PAID'
     and p.estado <> es.uca.orderflow.business.services.PedidoEstado.CANCELADO
""")
    List<Pedido> findPagadosEntre(@Param("from") Date from, @Param("to") Date to);

}