package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.Carrito;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.persistence.data.CarritoRepository;
import es.uca.orderflow.persistence.data.Detalle_CarritoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class GestionarCarritoCliente {

    private final CarritoRepository carritoRepository;
    private final Detalle_CarritoRepository detalleCarritoRepository;

    public GestionarCarritoCliente(CarritoRepository carritoRepository,
                                   Detalle_CarritoRepository detalleCarritoRepository) {
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
    }

    @Transactional
    public Carrito asegurarCarrito(Long clienteId) {
        return carritoRepository.findByClienteId(clienteId).orElseGet(() -> {
            Carrito c = new Carrito();
            Cliente cli = new Cliente();
            cli.setId(clienteId);
            c.setCliente(cli);
            c.setPrecio_total(BigDecimal.ZERO);
            return carritoRepository.save(c);
        });
    }

    public int contarLineas(Long clienteId) {
        return carritoRepository.findByClienteId(clienteId)
                .map(c -> detalleCarritoRepository.countByCarrito_Id(c.getId())) // <-- NO tocar c.getDetalles()
                .orElse(0);
    }
}

