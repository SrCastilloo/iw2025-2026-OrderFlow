package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.Caja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    Optional<Caja> findFirstByAbiertaTrueOrderByOpenedAtDesc();

    List<Caja> findTop10ByOrderByOpenedAtDesc();
}
