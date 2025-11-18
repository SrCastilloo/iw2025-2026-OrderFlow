// src/main/java/es/uca/orderflow/business/services/GestionarPedido.java
package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.*;
import es.uca.orderflow.persistence.data.Detalle_PedidoRepository;
import es.uca.orderflow.persistence.data.PedidoRepository;
import es.uca.orderflow.persistence.data.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class GestionarPedido {

    private final PedidoRepository pedidoRepository;
    private final Detalle_PedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository;
    private final CarritoQueryService carritoQueryService;
    private final QuitarProductoCarrito quitarProductoCarrito; // <- lo usamos para vaciar

    /**
     * Crea un Pedido con sus líneas a partir del carrito del cliente.
     * Devuelve el id del pedido creado.
     */
    @Transactional
    public Long crearPedidoDesdeCarrito(Cliente cliente,
                                        Carrito carrito,
                                        String direccionEnvio,
                                        PaymentMethod metodo,
                                        String txId) {

        // 1) Crear cabecera del pedido
        Pedido p = new Pedido();
        p.setCliente(cliente);
        p.setFechaRealizacion(new Date());
        p.setPaymentMethod(metodo);
        p.setPaymentStatus("PAID");
        p.setPaymentTxnId(txId);
        // si tienes el enum:
        // p.setEstado(PedidoEstado.PREPARACION);
        p = pedidoRepository.save(p);

        // 2) Crear líneas desde el resumen del carrito
        var resumen = carritoQueryService.obtenerResumen(cliente.getId());
        Pedido finalP = p;
        resumen.items().forEach(li -> {
            var prod = productoRepository.findById(li.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + li.productoId()));

            Detalle_Pedido dp = new Detalle_Pedido();
            dp.setPedido(finalP);
            dp.setProducto(prod);
            dp.setCantidad(li.cantidad());
            // li.precioUnitario() y li.subtotal() ya son BigDecimal -> asigna directo
            dp.setPrecioUnitario(li.precioUnitario());
            dp.setImporte(li.subtotal());

            detallePedidoRepository.save(dp);
            finalP.getDetallespedido().add(dp);
        });

        // 3) Vaciar carrito usando el caso de uso existente
        resumen.items().forEach(li ->
                quitarProductoCarrito.eliminarProducto(cliente.getId(), li.productoId())
        );

        return p.getId();
    }

    public void save(Pedido pedido){
        pedidoRepository.save(pedido);
    }  

    public Set<Pedido> pedidos_por_estado(PedidoEstado estado) {
        return pedidoRepository.findByEstado(estado);
    }
}
