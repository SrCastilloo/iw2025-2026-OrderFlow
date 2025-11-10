package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.Carrito;
import es.uca.orderflow.business.entities.Cliente;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TarjetaPaymentProvider implements PaymentProvider {
    @Override public PaymentMethod method() { return PaymentMethod.TARJETA; }
    @Override
    public PaymentResult charge(Cliente cliente, Carrito carrito, BigDecimal amount, PaymentRequest request) {
        // Simulación: aquí llamarías al PSP y validarías request.opaqueToken, etc.
        String tx = "TX-" + UUID.randomUUID();
        return new PaymentResult(true, tx, amount);
    }
}

@Component
class BizumPaymentProvider implements PaymentProvider {
    @Override public PaymentMethod method() { return PaymentMethod.BIZUM; }
    @Override
    public PaymentResult charge(Cliente cliente, Carrito carrito, BigDecimal amount, PaymentRequest request) {
        return new PaymentResult(true, "BIZ-" + UUID.randomUUID(), amount);
    }
}

@Component
class PaypalPaymentProvider implements PaymentProvider {
    @Override public PaymentMethod method() { return PaymentMethod.PAYPAL; }
    @Override
    public PaymentResult charge(Cliente cliente, Carrito carrito, BigDecimal amount, PaymentRequest request) {
        return new PaymentResult(true, "PP-" + UUID.randomUUID(), amount);
    }
}
