package es.uca.orderflow.business.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import es.uca.orderflow.business.entities.Carrito;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.persistence.data.CarritoRepository;
import es.uca.orderflow.persistence.data.ProductoRepository;

@Service
@RequiredArgsConstructor
public class DemoScriptService {

    private final RegistrarCliente registrar;
    private final IdentificarCliente identificar;
    private final ProductoRepository productoRepository;
    private final CarritoRepository carritoRepository;
    private final InsertarProductoCarrito insertarProductoCarrito;

    @Transactional
    public void runDemo() {
        // 1) Cliente
        Cliente c = new Cliente();
        c.setNombre("Test");
        c.setApellidos("User");
        c.setCorreo("test+" + System.currentTimeMillis() + "@demo.com");
        c.setContrasena("123456");

        registrar.registroCliente(c);
        identificar.identificaCliente(c.getCorreo(), "123456");

        // 2) Productos (guardar uno a uno y reutilizar el devuelto para tener ID)
        Producto p1 = new Producto();
        p1.setNombre("Ratón inalámbrico");
        p1.setPrecio(15.99);
        p1.setStock(50);
        p1 = productoRepository.save(p1);

        Producto p2 = new Producto();
        p2.setNombre("Teclado mecánico");
        p2.setPrecio(59.90);
        p2.setStock(30);
        p2 = productoRepository.save(p2);

        Producto p3 = new Producto();
        p3.setNombre("Monitor 24'' FHD");
        p3.setPrecio(129.99);
        p3.setStock(20);
        p3 = productoRepository.save(p3);

        // 3) Añadir al carrito
        insertarProductoCarrito.meterProductoCarrito(c.getId(), p1.getId(), 2);
        insertarProductoCarrito.meterProductoCarrito(c.getId(), p2.getId(), 1);
        insertarProductoCarrito.meterProductoCarrito(c.getId(), p3.getId(), 3);

        // 4) Mostrar carrito
        Carrito carrito = carritoRepository.findByClienteId(c.getId())
                .orElseThrow(() -> new IllegalStateException("No se encontró el carrito del cliente " + c.getId()));

        carrito.getDetalles().forEach(d ->
            System.out.println(" - " + d.getProducto().getNombre() + " x" + d.getCantidad()
                + " = " + d.getSubtotal() + "€")
        );
        System.out.println("💰 Total: " + carrito.getPrecio_total() + "€");
    }
}
