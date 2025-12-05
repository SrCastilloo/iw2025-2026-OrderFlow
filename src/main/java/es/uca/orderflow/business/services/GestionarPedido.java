package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.*;
import es.uca.orderflow.persistence.data.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GestionarPedido {

    private final PedidoRepository pedidoRepository;
    private final Detalle_PedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository;
    private final CarritoQueryService carritoQueryService;
    private final QuitarProductoCarrito quitarProductoCarrito;
    private final Detalle_CarritoRepository detalleCarritoRepository;
    private final CarritoRepository carritoRepository;

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

        Pedido p = new Pedido();
        p.setCliente(cliente);
        p.setFechaRealizacion(new Date());
        p.setPaymentMethod(metodo);
        p.setPaymentStatus("PAID");
        p.setPaymentTxnId(txId);
        p.setEstado(PedidoEstado.PENDIENTE);
        p.setDireccionEnvio(direccionEnvio);
        p = pedidoRepository.save(p);

        var resumen = carritoQueryService.obtenerResumen(cliente.getId());
        Pedido finalP = p;
        resumen.items().forEach(li -> {
            var prod = productoRepository.findById(li.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + li.productoId()));

            Detalle_Pedido dp = new Detalle_Pedido();
            dp.setPedido(finalP);
            dp.setProducto(prod);
            dp.setCantidad(li.cantidad());
            dp.setPrecioUnitario(li.precioUnitario());
            dp.setImporte(li.subtotal());

            detallePedidoRepository.save(dp);
            finalP.getDetallespedido().add(dp);
        });

        resumen.items().forEach(li ->
                quitarProductoCarrito.eliminarProducto(cliente.getId(), li.productoId())
        );

        return p.getId();
    }

    @Transactional
    public void cancelarPedido(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new NoSuchElementException("Pedido con ID " + pedidoId + " no encontrado."));

        if (pedido.getEstado() != PedidoEstado.PENDIENTE) {
            throw new IllegalStateException("El pedido #" + pedidoId + " no puede ser cancelado. Su estado actual es: " + pedido.getEstado().name());
        }

        pedido.setEstado(PedidoEstado.CANCELADO);
        pedidoRepository.save(pedido);

    }


    public void save(Pedido pedido){
        pedidoRepository.save(pedido);
    }

    public Set<Pedido> pedidos_por_estado(PedidoEstado estado) {
        return pedidoRepository.findByEstado(estado);
    }


    public boolean repartidorTienePedidoActivo(Empleado repartidor) {
        Set<Pedido> activos = pedidoRepository.findByRepartidorAndEstado(repartidor, PedidoEstado.EN_REPARTO);
        return !activos.isEmpty();
    }


    public Set<Pedido> pedidos_por_repartidor_y_estado(Empleado repartidor, PedidoEstado estado) {
        return pedidoRepository.findByRepartidorAndEstado(repartidor, estado);
    }


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
        pedido.setEstado(PedidoEstado.PREPARACION);

        Pedido savedPedido = pedidoRepository.save(pedido);

        for (Map.Entry<Producto, Integer> entry : productosConCantidad.entrySet()) {
            Producto producto = entry.getKey();
            Integer cantidad = entry.getValue();

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

        return pedidoRepository.save(savedPedido);
    }

    @Transactional
    public void cargarPedidoEnCarrito(Long pedidoId, Cliente cliente) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new NoSuchElementException("Pedido con ID " + pedidoId + " no encontrado."));

        if (pedido.getEstado() != PedidoEstado.PENDIENTE) {
            throw new IllegalStateException("El pedido #" + pedidoId + " no puede ser modificado. Su estado actual es: " + pedido.getEstado().name());
        }
        if (!pedido.getCliente().getId().equals(cliente.getId())) {
            throw new SecurityException("El pedido no pertenece al cliente actual.");
        }

        Carrito carrito = cliente.getCarrito();
        if (carrito == null) {
            throw new NoSuchElementException("Carrito no encontrado para el cliente.");
        }


        // ⭐ CORRECCIÓN CLAVE: Eliminación más segura
        // 1. Buscar todas las líneas actuales del carrito
        var lineasACancelar = detalleCarritoRepository.findByCarrito(carrito);
        detalleCarritoRepository.deleteAll(lineasACancelar);

        BigDecimal nuevoTotal = BigDecimal.ZERO;

        var detallesPedido = detallePedidoRepository.findByPedido(pedido);

        for (var d : detallesPedido) {
            Detalle_Carrito dc = new Detalle_Carrito();
            dc.setCarrito(carrito);
            dc.setProducto(d.getProducto());
            dc.setCantidad(d.getCantidad());
            dc.setPrecioUnitario(d.getPrecioUnitario());
            dc.setSubtotal(d.getImporte());

            detalleCarritoRepository.save(dc);
            nuevoTotal = nuevoTotal.add(d.getImporte());
        }

        carrito.setPrecio_total(nuevoTotal);
        carritoRepository.save(carrito);

    }

    @Transactional
    public Long finalizarModificacionPedido(Long pedidoId, Cliente cliente) {
        // 1. Obtener Pedido y Carrito y Validar Estado
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new NoSuchElementException("Pedido con ID " + pedidoId + " no encontrado."));

        if (pedido.getEstado() != PedidoEstado.PENDIENTE) {
            throw new IllegalStateException("El pedido #" + pedidoId + " no está en estado PENDIENTE para ser modificado.");
        }
        if (!pedido.getCliente().getId().equals(cliente.getId())) {
            throw new SecurityException("El pedido no pertenece al cliente actual.");
        }

        Carrito carrito = cliente.getCarrito();
        if (carrito == null) {
            throw new NoSuchElementException("Carrito no encontrado para el cliente.");
        }

        detallePedidoRepository.deleteByPedido_id(pedido.getId());

        // 3. Obtener las nuevas líneas del carrito y crear Detalle_Pedido
        var nuevasLineasCarrito = detalleCarritoRepository.findByCarrito(carrito);

        if (nuevasLineasCarrito.isEmpty()) {
            throw new IllegalArgumentException("El carrito no puede estar vacío al finalizar la modificación.");
        }

        BigDecimal nuevoTotalPedido = BigDecimal.ZERO;

        for (var dc : nuevasLineasCarrito) {
            Detalle_Pedido dp = new Detalle_Pedido();
            dp.setPedido(pedido);
            dp.setProducto(dc.getProducto());
            dp.setCantidad(dc.getCantidad());
            dp.setPrecioUnitario(dc.getPrecioUnitario());
            dp.setImporte(dc.getSubtotal());

            detallePedidoRepository.save(dp);
            nuevoTotalPedido = nuevoTotalPedido.add(dc.getSubtotal());
        }

        // 4. Actualizar cabecera del Pedido y cambiar estado
        pedido.setEstado(PedidoEstado.PREPARACION); // Vuelve a estado normal de procesamiento
        pedidoRepository.save(pedido);

        // 5. Vaciar el carrito
        detalleCarritoRepository.deleteByCarrito_Id(carrito.getId());
        carrito.setPrecio_total(BigDecimal.ZERO); // Asumo setPrecioTotal para el total del carrito
        carritoRepository.save(carrito);

        return pedido.getId();
    }

}