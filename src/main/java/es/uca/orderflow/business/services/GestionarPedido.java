// src/main/java/es/uca/orderflow/business/services/GestionarPedido.java
package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.*;
import es.uca.orderflow.persistence.data.Detalle_PedidoRepository;
import es.uca.orderflow.persistence.data.PedidoRepository;
import es.uca.orderflow.persistence.data.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Set;

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


    /**
     * Crea un Pedido con sus líneas a partir del carrito temporal gestionado por el recepcionista.
     * Este método se usa para pedidos en local o por teléfono.
     * @param cliente El cliente asociado al pedido (puede ser un cliente "Invitado" recién creado).
     * @param productosConCantidad El carrito temporal (Mapa de Producto a Cantidad).
     * @param metodo El método de pago (asumido en local).
     * @param paymentStatus El estado del pago (normalmente "PAID" en local).
     * @return El Pedido creado y guardado.
     */
    @Transactional
    public Pedido crearPedidoRecepcionista(
            Cliente cliente,
            Map<Producto, Integer> productosConCantidad,
            PaymentMethod metodo,
            String paymentStatus)
    {
        if (productosConCantidad == null || productosConCantidad.isEmpty()) {
            throw new IllegalArgumentException("El carrito del recepcionista no puede estar vacío.");
        }

        // 1. Crear cabecera del pedido
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setFechaRealizacion(new Date());
        pedido.setPaymentMethod(metodo);
        pedido.setPaymentStatus(paymentStatus);
        pedido.setEstado(PedidoEstado.PREPARACION); // Asumo este es el estado inicial

        // Guardamos la cabecera para obtener el ID
        Pedido savedPedido = pedidoRepository.save(pedido);

        // 2. Crear las líneas Detalle_Pedido
        for (Map.Entry<Producto, Integer> entry : productosConCantidad.entrySet()) {
            Producto producto = entry.getKey();
            Integer cantidad = entry.getValue();

            // Los productos del mapa ya están cargados, usamos su precio
            BigDecimal precioUnitario = producto.getPrecio();
            BigDecimal importe = precioUnitario.multiply(BigDecimal.valueOf(cantidad));

            Detalle_Pedido detalle = new Detalle_Pedido();
            detalle.setPedido(savedPedido);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setImporte(importe);

            detallePedidoRepository.save(detalle);
            savedPedido.getDetallespedido().add(detalle);
        }

        // 3. Opcional: Persistir el Pedido con los detalles adjuntos (aunque ya se han guardado los detalles)
        return pedidoRepository.save(savedPedido);
    }
}
