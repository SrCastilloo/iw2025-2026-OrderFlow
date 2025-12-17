package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.Carrito;
import es.uca.orderflow.persistence.data.CarritoRepository;
import es.uca.orderflow.persistence.data.Detalle_CarritoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;

@Service
public class CarritoQueryService {

    private final CarritoRepository carritoRepository;
    private final Detalle_CarritoRepository detalleRepo;

    public CarritoQueryService(CarritoRepository carritoRepository, Detalle_CarritoRepository detalleRepo) {
        this.carritoRepository = carritoRepository;
        this.detalleRepo = detalleRepo;
    }

    @Transactional
    public ResumenCarritoDTO obtenerResumen(Long clienteId) {
        Carrito c = carritoRepository.findByClienteId(clienteId)
                .orElse(null);
        if (c == null) {
            return new ResumenCarritoDTO(new ArrayList<>(), BigDecimal.ZERO);
        }
        var detalles = detalleRepo.findByCarrito_Id(c.getId());
        var items = detalles.stream().map(d -> new es.uca.orderflow.business.services.LineaCarritoDTO(
                d.getProducto().getId(),
                d.getProducto().getNombre(),
                d.getProducto().getFoto(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getSubtotal()
        )).toList();

        BigDecimal total = detalleRepo.sumSubtotalByCarritoId(c.getId());
        return new ResumenCarritoDTO(items, total);
    }
}
