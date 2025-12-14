package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.MenuComposicion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuComposicionRepository extends JpaRepository<MenuComposicion, Long> {
    List<MenuComposicion> findByMenuProducto_Id(Long menuProductoId);
    void deleteByMenuProducto_Id(Long menuId);}
