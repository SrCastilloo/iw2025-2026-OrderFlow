package es.uca.orderflow.business.services;


import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.Producto_Ingrediente;
import es.uca.orderflow.persistence.data.ProductoRepository;
import es.uca.orderflow.persistence.data.Producto_IngredienteRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public Producto actualizarProducto(Producto producto,List<Producto_Ingrediente> relaciones) //recibimos el producto ya modificado con sus ingredientes
    {
        //no existe el producto con el id indicado
        if(!productoRepository.existsById(producto.getId()))
            throw new RuntimeException("No existe el producto con el id: " + producto.getId());

        //guardamos el producto
        Producto actualizado  =  productoRepository.save(producto);

        for(Producto_Ingrediente ingrediente : relaciones)
        {
            producto_IngredienteRepository.deleteByProductoId(producto.getId());
        }


        for (Producto_Ingrediente pi : relaciones) {
            pi.setProducto(actualizado);
        }

        producto_IngredienteRepository.saveAll(relaciones);
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
