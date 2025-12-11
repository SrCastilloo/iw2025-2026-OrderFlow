package es.uca.orderflow.business.services;


import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.Producto_Ingrediente;
import es.uca.orderflow.persistence.data.ProductoRepository;
import es.uca.orderflow.persistence.data.Producto_IngredienteRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.OptimisticLockingFailureException;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GestionarProducto {
    private final ProductoRepository productoRepository;
    private final Producto_IngredienteRepository producto_IngredienteRepository;
    public GestionarProducto(ProductoRepository productoRepository,Producto_IngredienteRepository producto_IngredienteRepository
    ) {this.productoRepository = productoRepository;
    this.producto_IngredienteRepository = producto_IngredienteRepository;
    }

    public Producto crearProducto(Producto producto) {

        productoRepository.save(producto);

        return producto;
    }

    @Transactional
    public Producto actualizarProducto(Producto producto, List<Producto_Ingrediente> relaciones) {

        if (producto.getId() == null || !productoRepository.existsById(producto.getId())) {
            throw new IllegalArgumentException("No existe el producto con id: " + producto.getId());
        }

        try {
            // 1) Guardamos el producto (usando @Version para control de concurrencia)
            Producto actualizado = productoRepository.save(producto);

            // 2) quitamos las relaciones del producto con los ingredientes
            producto_IngredienteRepository.hardDeleteByProductoId(actualizado.getId());
            producto_IngredienteRepository.flush();

            Map<Long, Producto_Ingrediente> porIngrediente = new LinkedHashMap<>();
            for (Producto_Ingrediente pi : relaciones) {
                if (pi.getIngrediente() == null || pi.getIngrediente().getId() == null) {
                    throw new IllegalArgumentException("Ingrediente inválido en la fila.");
                }

                pi.setId(null);
                pi.setProducto(actualizado);
                porIngrediente.put(pi.getIngrediente().getId(), pi);
            }

            List<Producto_Ingrediente> aGuardar = new ArrayList<>(porIngrediente.values());
            if (aGuardar.isEmpty()) {
                throw new IllegalArgumentException("Añade al menos un ingrediente.");
            }

            producto_IngredienteRepository.saveAll(aGuardar);

            return actualizado;

        } catch (OptimisticLockingFailureException ex) {
            throw new IllegalStateException(
                    "El producto ha sido modificado por otro dueño. " +
                            "Recarga la página para ver los cambios y vuelve a intentarlo."
            );
        }
    }



    public List<Producto> consultarProductos()
    {
        return productoRepository.findAll();
    }


    public Producto eliminarProducto(Producto producto)
    {
        if(!productoRepository.existsById(producto.getId()))
            throw new RuntimeException("No existe el producto con el id: " + producto.getId());

        productoRepository.deleteById(producto.getId());
        return producto;
    }


    public Producto guardarProducto_Ingrediente(Producto producto, List<Producto_Ingrediente> relaciones)
    {
        Producto p =  productoRepository.save(producto);
        producto_IngredienteRepository.saveAll(relaciones);



        return p;
    }

    public Producto buscarProductoPorId(Long idProducto)
    {
        return productoRepository.findById(idProducto).orElse(null);
    }

    public List<Producto_Ingrediente> encontrarIngredientesPorProductoId(Long idProducto)
    {
        return producto_IngredienteRepository.findByProductoIdWithIngrediente(idProducto);
    }

}
