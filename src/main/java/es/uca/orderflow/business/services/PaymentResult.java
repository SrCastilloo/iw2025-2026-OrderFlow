package es.uca.orderflow.business.services;

import java.math.BigDecimal;

public record PaymentResult(boolean success, String txId, BigDecimal amount) {}
