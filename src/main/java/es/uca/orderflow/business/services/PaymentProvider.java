package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.entities.Carrito;
import java.math.BigDecimal;

public interface PaymentProvider {
    PaymentMethod method();
    PaymentResult charge(Cliente cliente, Carrito carrito, BigDecimal amount, PaymentRequest request);
}