package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.EstadoMesa;
import es.uca.orderflow.business.entities.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MesaRepository extends JpaRepository<Mesa, Long> {
    List<Mesa> findByEstado(EstadoMesa estado);
}
