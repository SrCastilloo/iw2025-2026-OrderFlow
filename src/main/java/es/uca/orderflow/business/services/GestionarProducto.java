package es.uca.orderflow.business.services;


import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.persistence.data.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GestionarProducto {
    private final ProductoRepository productoRepository;
    public GestionarProducto(ProductoRepository productoRepository) {this.productoRepository = productoRepository;}

    public Producto crearProducto(Producto producto) {

        productoRepository.save(producto);

        return producto;
    }


    public Producto actualizarProducto(Producto producto) //recibimos el producto ya modificado
    {
        //no existe el producto con el id indicado
        if(!productoRepository.existsById(producto.getId()))
            throw new RuntimeException("No existe el producto con el id: " + producto.getId());

        //guardamos el producto
        return productoRepository.save(producto);
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
