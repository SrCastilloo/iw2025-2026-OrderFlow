package es.uca.orderflow.business.services;

import java.math.BigDecimal;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import es.uca.orderflow.business.entities.Carrito;
import es.uca.orderflow.business.entities.Cliente;
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

    @Transactional
    public void meterProductoCarrito(Long clienteId, Long productoId, Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }

        // Asegura carrito (si no lo tienes ya)
        Carrito c = carritoRepository.findByClienteId(clienteId).orElseGet(() -> {
            Carrito nuevo = new Carrito();
            Cliente cli = new Cliente();
            cli.setId(clienteId);
            nuevo.setCliente(cli);
            nuevo.setPrecio_total(BigDecimal.ZERO);
            return carritoRepository.save(nuevo);
        });

        Producto p = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        if (cantidad > p.getStock()) {
            throw new IllegalArgumentException("No hay stock suficiente");
        }

        if (!c.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("Este carrito no pertenece al cliente");
        }

        // Buscar/crear la línea SIN tocar c.getDetalles()
        Detalle_Carrito det = detalleCarritoRepository
                .findByCarrito_IdAndProducto_Id(c.getId(), p.getId())
                .orElse(null);

        if (det == null) {
            det = new Detalle_Carrito();
            det.setCarrito(c);
            det.setProducto(p);
            det.setCantidad(cantidad);
            det.setPrecioUnitario(p.getPrecio());
            det.setSubtotal(p.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
        } else {
            int nuevaCantidad = det.getCantidad() + cantidad;
            det.setCantidad(nuevaCantidad);
            det.setPrecioUnitario(p.getPrecio()); // opcional: actualiza precio
            det.setSubtotal(det.getPrecioUnitario().multiply(BigDecimal.valueOf(nuevaCantidad)));
        }
        detalleCarritoRepository.save(det);

        // Actualiza stock SOLO con lo añadido ahora
        p.setStock(p.getStock() - cantidad);
        productoRepository.save(p);

        // Recalcula total por consulta (evita iterar la colección LAZY)
        BigDecimal total = detalleCarritoRepository.sumSubtotalByCarritoId(c.getId());
        c.setPrecio_total(total);
        carritoRepository.save(c);

    }
}
