package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.Ingrediente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredienteRepository extends JpaRepository<Ingrediente, Long> {}
