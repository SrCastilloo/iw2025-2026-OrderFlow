// src/main/java/es/uca/orderflow/integration/payments/ExternalPaymentsApi.java
package es.uca.orderflow.integration.payments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

public class ExternalPaymentsApi {

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ChargeRequest {
        private BigDecimal amount;    // 49.00, etc.
        private String currency;      // "EUR"
        private String description;   // "Unlock payment methods"
        private String sourceToken;   // "tok_test_visa" o similar
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ChargeResponse {
        private boolean success;
        private String transactionId;
        private String failureReason;
    }

    // Simula un cobro exitoso 95% de las veces
    public ChargeResponse createCharge(ChargeRequest req) {
        boolean ok = Math.random() > 0.05;
        if (ok) {
            return new ChargeResponse(true, "txn_" + UUID.randomUUID(), null);
        } else {
            return new ChargeResponse(false, null, "Card declined");
        }
    }
}
