package es.uca.orderflow.persistence.data;
import es.uca.orderflow.business.entities.ProductoTipo;
import org.springframework.data.jpa.repository.JpaRepository;

import es.uca.orderflow.business.entities.Producto;

import java.util.List;

import es.uca.orderflow.presentation.dto.ProductoCardDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ProductoRepository extends JpaRepository<Producto,Long> {
    List<Producto> findByTipo(ProductoTipo tipo);
    List<Producto> findByActivoTrue();
    List<Producto> findByTipoAndActivoTrue(ProductoTipo tipo);
    List<Producto> findTop10ByActivoTrueAndTipoOrderByLastModifiedDateDesc(ProductoTipo tipo);
    List<Producto> findTop10ByActivoTrueAndTipoNotOrderByLastModifiedDateDesc(ProductoTipo tipo);
    @Query("""
        select new es.uca.orderflow.presentation.dto.ProductoCardDTO(
            p.id, p.nombre, p.descripcion, p.precio, p.foto, p.tipo
        )
        from Producto p
        where p.activo = true
          and (
               :q is null or :q = '' or
               lower(p.nombre) like lower(concat('%', :q, '%')) or
               lower(p.descripcion) like lower(concat('%', :q, '%'))
          )
        """)
    Page<ProductoCardDTO> findCatalogoCards(@Param("q") String q, Pageable pageable);

}
