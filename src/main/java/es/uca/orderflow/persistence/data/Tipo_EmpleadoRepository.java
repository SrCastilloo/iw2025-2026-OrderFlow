package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.Tipo_Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Tipo_EmpleadoRepository extends JpaRepository<Tipo_Empleado, Long> {
        Optional<Tipo_Empleado> findByNombreIgnoreCase(String nombre); //buscamos el tipo por el nombre (cocinero por ejemplo)
}

