package es.uca.orderflow.persistence.data;
import org.springframework.data.jpa.repository.JpaRepository;

import es.uca.orderflow.business.entities.Cliente;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long>
{
    Optional<Cliente> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    Optional<Cliente> findByCorreoIgnoreCase(String correo);
}