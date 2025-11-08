package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.*;
import es.uca.orderflow.persistence.data.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class GestionarCarrito {
    private CarritoRepository carritoRepository;
    private Detalle_CarritoRepository detalleCarritoRepository;
    private ClienteRepository clienteRepository;
    private Detalle_PedidoRepository detallePedido;

    public GestionarCarrito(CarritoRepository carritoRepository, Detalle_CarritoRepository detalleCarritoRepository
    , ClienteRepository clienteRepository, Detalle_PedidoRepository detallePedido) {
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.clienteRepository = clienteRepository;
        this.detallePedido = detallePedido;
    }

    public List<Detalle_Carrito> GetCarrito(long clienteId) {
        Carrito carrito = carritoRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no tiene carrito")); // esto de aqui es por el Optional del repositorio

        return detalleCarritoRepository.findByCarrito_Id(carrito.getId());
    }

    public void eliminarProductoCarrito(long productoId, long carritoId) {
        Carrito carrito = carritoRepository.findById(carritoId).orElseThrow(() ->
                new IllegalArgumentException("El producto no está en el carrito"));

        Detalle_Carrito detalle = detalleCarritoRepository.findByCarrito_IdAndProducto_Id(carritoId, productoId)
                .orElseThrow(() -> new IllegalArgumentException("El producto no está en el carrito"));

        carrito.getDetalles().remove(detalle);

        detalleCarritoRepository.delete(detalle);

        BigDecimal nuevoTotal = carrito.getDetalles().stream()
                .map(Detalle_Carrito::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        carrito.setPrecio_total(nuevoTotal);

        carritoRepository.save(carrito);
    }
}

