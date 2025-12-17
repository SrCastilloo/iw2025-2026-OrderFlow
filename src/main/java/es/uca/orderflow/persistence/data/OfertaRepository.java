package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.Oferta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {
    List<Oferta> findByActivaTrue();
    List<Oferta> findAllByOrderByIdDesc();
}
