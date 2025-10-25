package es.uca.orderflow.persistence.data;
import org.springframework.data.jpa.repository.JpaRepository;

import es.uca.orderflow.business.entities.Carrito;
import es.uca.orderflow.business.entities.Cliente;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito,Long> {

    Optional<Carrito>  findByCliente(Cliente cliente); //carrito de un cliente  
    Optional<Carrito> findByClienteId(Long clienteId);
    boolean existsByCliente(Cliente c);
}
