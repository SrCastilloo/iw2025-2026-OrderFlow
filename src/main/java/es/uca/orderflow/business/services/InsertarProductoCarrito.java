package es.uca.orderflow.business.services;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import es.uca.orderflow.business.entities.Carrito;
import es.uca.orderflow.business.entities.Detalle_Carrito;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.persistence.data.CarritoRepository;
import es.uca.orderflow.persistence.data.Detalle_CarritoRepository;
import es.uca.orderflow.persistence.data.ProductoRepository;

@Service
public class InsertarProductoCarrito {

    private final CarritoRepository carritoRepository;
    private final Detalle_CarritoRepository detalleCarritoRepository;
    private final ProductoRepository productoRepository;

    public InsertarProductoCarrito(CarritoRepository carritoRepository,
            Detalle_CarritoRepository detalleCarritoRepository,
            ProductoRepository productoRepository) {
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.productoRepository = productoRepository;
    }

    public Carrito meterProductoCarrito(Long clienteid, Long productoid, Integer cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }

        // comprobamos que el cliente tenga carrito (debe tener)
        Carrito c = carritoRepository.findByClienteId(clienteid).orElseThrow(
                () -> new IllegalArgumentException("El cliente no tiene carrito"));

        Producto p = productoRepository.findById(productoid).orElseThrow(
                () -> new IllegalArgumentException("El producto para meter en el carrito no existe"));

        if (cantidad > p.getStock()) {
            throw new IllegalArgumentException("No hay Stock Suficiente de este producto");
        }

        if (c.getCliente().getId() != clienteid) {
            throw new IllegalArgumentException("Este carrito no pertenece al cliente");
        }

        // buscamos si ya existe el producto en el carrito
        Detalle_Carrito detalle = detalleCarritoRepository.findByCarrito_IdAndProducto_Id(c.getId(), productoid)
                .orElse(null);

        // no existe el producto en el carrito, lo metemos
        if (detalle == null) {
            detalle = new Detalle_Carrito();
            detalle.setProducto(p);
            detalle.setCantidad(cantidad);
            detalle.setCarrito(c);
            detalle.setPrecioUnitario(p.getPrecio());
            detalle.setSubtotal(p.getPrecio().multiply(new BigDecimal(cantidad)));
            c.getDetalles().add(detalle); // metemos el producto en el carrito
            p.setStock(p.getStock() - cantidad); // actualizamos el stock
        }

        // el producto ya existe en el carrito, metemos la nueva cantidad
        else {
            int nuevaCantidad = detalle.getCantidad() + cantidad;
            detalle.setCantidad(nuevaCantidad);
            detalle.setSubtotal(p.getPrecio().multiply(new BigDecimal(cantidad)));
            detalle.setSubtotal(detalle.getPrecioUnitario().multiply(new BigDecimal(nuevaCantidad)));
            p.setStock(p.getStock() - nuevaCantidad); // actualizamos el stock

        }

        // recalculamos el total del carrito a partir de todos los detalles del mismo
        BigDecimal nuevoTotal = c.getDetalles()
                .stream()
                .map(Detalle_Carrito::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        carritoRepository.save(c);

        return c;
    }

}
