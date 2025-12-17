package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.Carrito;
import es.uca.orderflow.persistence.data.CarritoRepository;
import es.uca.orderflow.persistence.data.Detalle_CarritoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class QuitarProductoCarrito {

    private final CarritoRepository carritoRepository;
    private final Detalle_CarritoRepository detalleRepo;

    public QuitarProductoCarrito(CarritoRepository carritoRepository, Detalle_CarritoRepository detalleRepo) {
        this.carritoRepository = carritoRepository;
        this.detalleRepo = detalleRepo;
    }

    @Transactional
    public void eliminarProducto(Long clienteId, Long productoId) {
        Carrito c = carritoRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No hay carrito"));
        var det = detalleRepo.findByCarrito_IdAndProducto_Id(c.getId(), productoId)
                .orElseThrow(() -> new IllegalArgumentException("La línea no existe"));
        detalleRepo.delete(det);

        // actualiza total
        c.setPrecio_total(detalleRepo.sumSubtotalByCarritoId(c.getId()));
        carritoRepository.save(c);
    }
}
