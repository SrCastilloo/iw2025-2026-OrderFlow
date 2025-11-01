package es.uca.orderflow.persistence.data;


import es.uca.orderflow.business.entities.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
}
