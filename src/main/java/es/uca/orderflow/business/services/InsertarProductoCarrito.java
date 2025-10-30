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
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }

        Carrito c = carritoRepository.findByClienteId(clienteid).orElseThrow(
                () -> new IllegalArgumentException("El cliente no tiene carrito"));

        Producto p = productoRepository.findById(productoid).orElseThrow(
                () -> new IllegalArgumentException("El producto para meter en el carrito no existe"));

        if (cantidad > p.getStock()) {
            throw new IllegalArgumentException("No hay Stock Suficiente de este producto");
        }

        
        if (!c.getCliente().getId().equals(clienteid)) {
            throw new IllegalArgumentException("Este carrito no pertenece al cliente");
        }

        // Buscamos si ya existe el producto en el carrito
        Detalle_Carrito detalle = detalleCarritoRepository
                .findByCarrito_IdAndProducto_Id(c.getId(), productoid)
                .orElse(null);

        if (detalle == null) {
            // No existe: lo creamos
            detalle = new Detalle_Carrito();
            detalle.setProducto(p);
            detalle.setCantidad(cantidad);
            detalle.setCarrito(c);
            detalle.setPrecioUnitario(p.getPrecio());
            detalle.setSubtotal(p.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
            c.getDetalles().add(detalle);

            // stock: solo restamos la cantidad nueva
            p.setStock(p.getStock() - cantidad);
        } else {
            // Ya existe: sumamos cantidad
            int nuevaCantidad = detalle.getCantidad() + cantidad;
            detalle.setCantidad(nuevaCantidad);

            // Subtotal = precioUnitario * nuevaCantidad (no hace falta la línea intermedia)
            detalle.setSubtotal(detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(nuevaCantidad)));

            // stock: restar SOLO lo que se añade ahora (cantidad), no la nuevaCantidad
            // completa
            p.setStock(p.getStock() - cantidad);
        }

        // Recalcular total del carrito
        BigDecimal nuevoTotal = c.getDetalles().stream()
                .map(Detalle_Carrito::getSubtotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //  AQUÍ ESTABA TU BUG: te faltaba asignarlo al carrito
        c.setPrecio_total(nuevoTotal);

        // Guardamos cambios. Como hay @Transactional, con esto basta para carrito y
        // detalles (cascade).
        carritoRepository.save(c);
        // El producto p viene gestionado por JPA; con la transacción se sincroniza. Si
        // quieres ser explícito:
        // productoRepository.save(p);

        return c;
    }

}
