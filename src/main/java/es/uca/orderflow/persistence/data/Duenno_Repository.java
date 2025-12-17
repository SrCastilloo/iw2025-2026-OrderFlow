package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.Duenno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Duenno_Repository extends JpaRepository<Duenno, Long> {
    Optional<Duenno> findByCorreo(String correo);
    boolean existsByCorreoIgnoreCase(String correo);
    Optional<Duenno> findByCorreoIgnoreCase(String correo);
}
