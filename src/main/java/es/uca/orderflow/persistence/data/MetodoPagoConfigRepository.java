package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.MetodoPagoConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetodoPagoConfigRepository extends JpaRepository<MetodoPagoConfig, Long> {
}
