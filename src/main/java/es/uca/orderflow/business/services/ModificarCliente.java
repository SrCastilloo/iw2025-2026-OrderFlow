package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.Carrito;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.entities.Pedido;
import es.uca.orderflow.persistence.data.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class ModificarCliente {
    final private ClienteRepository clienteRepository;
    final private PedidoRepository pedidoRepository;
    final private CarritoRepository carritoRepository;
    private final Detalle_PedidoRepository detallePedidoRepository;
    private final Detalle_CarritoRepository detalleCarritoRepository;


    public ModificarCliente(ClienteRepository clienteRepository,PedidoRepository pedidoRepository,CarritoRepository carritoRepository, Detalle_PedidoRepository
                            det,Detalle_CarritoRepository detalleCarritoRepository)
    {this.clienteRepository = clienteRepository;
    this.pedidoRepository = pedidoRepository;
    this.carritoRepository = carritoRepository;
    this.detallePedidoRepository = det;
    this.detalleCarritoRepository = detalleCarritoRepository;
    }

    public Cliente modificarCliente(Cliente c) {return clienteRepository.save(c);}

    public Cliente ObetenerCliente(long id)  {return clienteRepository.findById(id).orElse(null);}

    @Transactional
    public void eliminarCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RuntimeException("Cliente no encontrado");
        }

        // Eliminar los pedidos del cliente
        Set<Pedido> pedidos = pedidoRepository.findByClienteId(clienteId);
        for (Pedido pedido : pedidos) {
            // Primero eliminamos los detalles de los pedidos
            detallePedidoRepository.deleteByPedido_Id(pedido.getId());
        }
        // Luego eliminamos los pedidos
        pedidoRepository.deleteByClienteId(clienteId);

        // Eliminar el carrito asociado al cliente
        Carrito carrito = carritoRepository.findByClienteId(clienteId).orElse(null);
        if (carrito != null) {
            // Eliminar los detalles del carrito antes de eliminar el carrito
            detalleCarritoRepository.deleteByCarrito_Id(carrito.getId());
            carritoRepository.deleteById(carrito.getId());
        }

        // Finalmente eliminar el cliente
        clienteRepository.deleteById(clienteId);
    }
}


