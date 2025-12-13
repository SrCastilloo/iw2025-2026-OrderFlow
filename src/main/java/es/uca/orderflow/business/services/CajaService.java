package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.*;
import es.uca.orderflow.persistence.data.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class CajaService {

    private static final BigDecimal IVA_RATE = new BigDecimal("0.21");

    private final CajaRepository cajaRepository;
    private final PedidoRepository pedidoRepository;
    private final Detalle_PedidoRepository detallePedidoRepository;

    @Transactional(readOnly = true)
    public boolean isCajaAbierta() {
        return cajaRepository.findFirstByAbiertaTrueOrderByOpenedAtDesc().isPresent();
    }

    @Transactional(readOnly = true)
    public Caja getCajaAbiertaOrNull() {
        return cajaRepository.findFirstByAbiertaTrueOrderByOpenedAtDesc().orElse(null);
    }

    @Transactional
    public Caja abrirCaja(Empleado empleado) {
        // si ya hay una abierta, devolvemos esa
        var abierta = cajaRepository.findFirstByAbiertaTrueOrderByOpenedAtDesc();
        if (abierta.isPresent()) return abierta.get();

        Caja c = new Caja();
        c.setOpenedAt(new Date());
        c.setAbierta(true);
        c.setOpenedBy(empleado);
        c.setTotalBase(BigDecimal.ZERO);
        c.setTotalIva(BigDecimal.ZERO);
        c.setTotalConIva(BigDecimal.ZERO);
        c.setNumPedidos(0);

        return cajaRepository.save(c);
    }

    @Transactional
    public Caja cerrarCaja(Empleado empleado) {
        Caja c = cajaRepository.findFirstByAbiertaTrueOrderByOpenedAtDesc()
                .orElseThrow(() -> new IllegalStateException("No hay una caja abierta."));

        Date from = c.getOpenedAt();
        Date to = new Date();

        var pedidos = pedidoRepository.findPagadosEntre(from, to);

        BigDecimal base = BigDecimal.ZERO;
        for (Pedido p : pedidos) {
            for (Detalle_Pedido dp : detallePedidoRepository.findByPedido(p)) {
                if (dp.getImporte() != null) base = base.add(dp.getImporte());
            }
        }

        base = base.setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva = base.multiply(IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = base.add(iva).setScale(2, RoundingMode.HALF_UP);

        c.setTotalBase(base);
        c.setTotalIva(iva);
        c.setTotalConIva(total);
        c.setNumPedidos(pedidos.size());
        c.setClosedAt(to);
        c.setAbierta(false);
        c.setClosedBy(empleado);

        return cajaRepository.save(c);
    }

    public void assertCajaAbierta() {
        if (!isCajaAbierta()) {
            throw new IllegalStateException("La caja está cerrada. No se pueden realizar pedidos.");
        }
    }
}
