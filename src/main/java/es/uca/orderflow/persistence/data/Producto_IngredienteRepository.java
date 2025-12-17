package es.uca.orderflow.persistence.data;

import es.uca.orderflow.business.entities.Producto_Ingrediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface Producto_IngredienteRepository extends JpaRepository<Producto_Ingrediente, Long> {

    // Para obtener relaciones por producto (derivado válido)
    List<Producto_Ingrediente> findByProducto_Id(Long productoId);

    // Borrado físico de la tabla intermedia
    @Modifying
    @Query("delete from Producto_Ingrediente pi where pi.producto.id = :productoId")
    void hardDeleteByProductoId(@Param("productoId") Long productoId);

    // Si quieres forzar traer ingrediente (evitar lazy), usa JOIN FETCH
    @Query("""
        select pi
        from Producto_Ingrediente pi
        join fetch pi.ingrediente
        where pi.producto.id = :productoId
    """)
    List<Producto_Ingrediente> findByProductoIdWithIngrediente(@Param("productoId") Long productoId);
}
