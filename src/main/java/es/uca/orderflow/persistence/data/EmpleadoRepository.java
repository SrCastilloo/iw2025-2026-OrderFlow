package es.uca.orderflow.persistence.data;


import es.uca.orderflow.business.entities.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    Optional<Empleado> findByCorreoIgnoreCase(String correo);

}
