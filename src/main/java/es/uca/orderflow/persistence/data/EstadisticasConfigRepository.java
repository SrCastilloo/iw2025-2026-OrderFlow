// src/main/java/es/uca/orderflow/persistence/data/EstadisticasConfigRepository.java
package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.EstadisticasConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadisticasConfigRepository extends JpaRepository<EstadisticasConfig, Long> {
}
