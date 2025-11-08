package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.*;
import es.uca.orderflow.persistence.data.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class GestionarPedido {
    private PedidoRepository pedidoRepository;
    private ClienteRepository clienteRepository;
    private Detalle_PedidoRepository detallePedido;

    public GestionarPedido(PedidoRepository pedidoRepository, Detalle_CarritoRepository detalleCarritoRepository
            , ClienteRepository clienteRepository, Detalle_PedidoRepository detallePedido) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.detallePedido = detallePedido;
    }

    public Set<Pedido> getPedidosCliente(long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        return pedidoRepository.findByCliente(cliente);
    }

    public Set<Detalle_Pedido> getProductos(long clienteId, long pedidoId) {
        // 1️⃣ Obtener cliente
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        // 2️⃣ Obtener pedido
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        // 4️⃣ Obtener los detalles del pedido
        return detallePedido.findByPedido(pedido);
    }

    /*public void eliminarProductoCarrito(long clienteId, long productoId, long pedidoId) {
        Carrito carrito = carritoRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Carrito no encontrado"));

        Detalle_Carrito detalle_carrito =

    }*/
}
