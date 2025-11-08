package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.Producto_Ingrediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface Producto_IngredienteRepository extends JpaRepository<Producto_Ingrediente, Long> {

    List<Producto_Ingrediente> findByProductoId(Long productoId);

    void deleteByProductoId(Long productoId);

    @Query("""
        select pi
        from Producto_Ingrediente pi
        join fetch pi.ingrediente
        where pi.producto.id = :productoId
    """)
    List<Producto_Ingrediente> findByProductoIdWithIngrediente(@Param("productoId") Long productoId);
}





