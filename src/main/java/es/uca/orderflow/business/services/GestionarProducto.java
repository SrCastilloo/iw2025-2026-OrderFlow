package es.uca.orderflow.business.services;


import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.Producto_Ingrediente;
import es.uca.orderflow.persistence.data.ProductoRepository;
import es.uca.orderflow.persistence.data.Producto_IngredienteRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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

        // 1) Guardar el producto (queda managed)
        Producto actualizado = productoRepository.save(producto);

        // 2) BORRADO DURO de todas las relaciones del producto (una vez)
        producto_IngredienteRepository.hardDeleteByProductoId(actualizado.getId());
        // Forzar que el DELETE baje a BD antes de insertar (belt & suspenders)
        producto_IngredienteRepository.flush();

        // 3) Deduplicar por ingrediente_id y normalizar
        Map<Long, Producto_Ingrediente> porIngrediente = new LinkedHashMap<>();
        for (Producto_Ingrediente pi : relaciones) {
            if (pi.getIngrediente() == null || pi.getIngrediente().getId() == null) {
                throw new IllegalArgumentException("Ingrediente inválido en la fila.");
            }
            // Normaliza: asegurar INSERT limpio
            pi.setId(null);                 // MUY IMPORTANTE si viene con id desde la UI
            pi.setProducto(actualizado);    // asigna el producto managed
            porIngrediente.put(pi.getIngrediente().getId(), pi); // pisa duplicados
        }

        List<Producto_Ingrediente> aGuardar = new ArrayList<>(porIngrediente.values());
        if (aGuardar.isEmpty()) {
            throw new IllegalArgumentException("Añade al menos un ingrediente.");
        }

        // 4) Insertar nuevas relaciones
        producto_IngredienteRepository.saveAll(aGuardar);

        return actualizado;
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

}
