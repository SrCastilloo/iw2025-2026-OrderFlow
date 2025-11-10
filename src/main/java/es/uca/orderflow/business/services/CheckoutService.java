package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.Carrito;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.persistence.data.CarritoRepository;
import es.uca.orderflow.persistence.data.ClienteRepository;
import es.uca.orderflow.persistence.data.Detalle_CarritoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CheckoutService {

    private final ClienteRepository clienteRepo;
    private final CarritoRepository carritoRepo;
    private final Detalle_CarritoRepository detalleRepo;
    private final PaymentProviderFactory providerFactory;
    private final GestionarPedido gestionarPedido;

    public CheckoutService(ClienteRepository clienteRepo,
                           CarritoRepository carritoRepo,
                           Detalle_CarritoRepository detalleRepo,
                           PaymentProviderFactory providerFactory,
                           GestionarPedido gestionarPedido) {
        this.clienteRepo = clienteRepo;
        this.carritoRepo = carritoRepo;
        this.detalleRepo = detalleRepo;
        this.providerFactory = providerFactory;
        this.gestionarPedido = gestionarPedido;
    }

    @Transactional
    public CheckoutResult checkout(Long clienteId, PaymentMethod method, PaymentRequest request) {
        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        Carrito carrito = carritoRepo.findByClienteId(clienteId)
                .orElseThrow(() -> new IllegalStateException("No hay carrito"));

        BigDecimal total = detalleRepo.sumSubtotalByCarritoId(carrito.getId());
        if (total == null || total.signum() <= 0) {
            throw new IllegalStateException("El carrito está vacío");
        }

        // 1) Cobrar con el proveedor elegido
        PaymentProvider provider = providerFactory.get(method);
        var payRes = provider.charge(cliente, carrito, total, request);
        if (!payRes.success()) throw new IllegalStateException("Pago rechazado");

        // 2) Crear pedido
        Long orderId = gestionarPedido.crearPedidoDesdeCarrito(cliente, carrito, request.address(), method, payRes.txId());

        // 3) Vaciar carrito
        detalleRepo.deleteByCarrito_Id(carrito.getId());
        carrito.setPrecio_total(BigDecimal.ZERO);
        carritoRepo.save(carrito);

        return new CheckoutResult(orderId, payRes.txId());
    }
}
