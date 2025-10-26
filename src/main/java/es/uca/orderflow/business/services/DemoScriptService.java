package es.uca.orderflow.business.services;

import java.util.List;

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
        Cliente c = new Cliente();
        c.setNombre("Test");
        c.setApellidos("User");
        c.setCorreo("test+" + System.currentTimeMillis() + "@demo.com");
        c.setContrasena("123456");

        registrar.registroCliente(c);
        identificar.identificaCliente(c.getCorreo(), "123456");

        Producto p1 = new Producto(); p1.setNombre("Ratón inalámbrico"); p1.setPrecio(15.99); p1.setStock(50);
        Producto p2 = new Producto(); p2.setNombre("Teclado mecánico");  p2.setPrecio(59.90); p2.setStock(30);
        Producto p3 = new Producto(); p3.setNombre("Monitor 24'' FHD");  p3.setPrecio(129.99); p3.setStock(20);
        productoRepository.saveAll(List.of(p1, p2, p3));

        insertarProductoCarrito.meterProductoCarrito(c.getId(), p1.getId(), 2);
        insertarProductoCarrito.meterProductoCarrito(c.getId(), p2.getId(), 1);
        insertarProductoCarrito.meterProductoCarrito(c.getId(), p3.getId(), 3);

        Carrito carrito = carritoRepository.findByClienteId(c.getId())
                .orElseThrow();

        carrito.getDetalles().forEach(d ->
            System.out.println(" - " + d.getProducto().getNombre() + " x" + d.getCantidad()
                + " = " + d.getSubtotal() + "€")
        );
        System.out.println("💰 Total: " + carrito.getPrecio_total() + "€");
    }
}
