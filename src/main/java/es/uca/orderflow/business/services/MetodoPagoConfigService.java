// src/main/java/es/uca/orderflow/business/services/MetodoPagoConfigService.java
package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.MetodoPagoConfig;
import es.uca.orderflow.integration.payments.ExternalPaymentsApi;
import es.uca.orderflow.persistence.data.MetodoPagoConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// src/main/java/es/uca/orderflow/business/services/MetodoPagoConfigService.java
@Service
@RequiredArgsConstructor
public class MetodoPagoConfigService {

    private final MetodoPagoConfigRepository repo;

    private MetodoPagoConfig getOrCreate() {
        return repo.findById(1L).orElseGet(() -> {
            MetodoPagoConfig cfg = MetodoPagoConfig.builder()
                    .tarjetaEnabled(true)
                    .paypalEnabled(false)
                    .bizumEnabled(false)
                    .transferenciaEnabled(false)
                    .fullUnlocked(false)
                    .unlockedAt(null)
                    .unlockedBy(null)
                    .priceCents(4900)
                    .build();
            cfg.setId(null);
            return repo.save(cfg);
        });
    }

    /** Métodos visibles para el cliente */
    public List<PaymentMethod> getDisponibles() {
        MetodoPagoConfig c = getOrCreate();

        // 🔒 Si el pack está bloqueado, forzamos a mostrar solo TARJETA
        if (!c.isFullUnlocked()) {
            return c.isTarjetaEnabled()
                    ? List.of(PaymentMethod.TARJETA)
                    : List.of();
        }

        // 🔓 Pack desbloqueado: devolvemos según flags
        List<PaymentMethod> out = new java.util.ArrayList<>(4);
        if (c.isTarjetaEnabled())       out.add(PaymentMethod.TARJETA);
        if (c.isPaypalEnabled())        out.add(PaymentMethod.PAYPAL);
        if (c.isBizumEnabled())         out.add(PaymentMethod.BIZUM);
        if (c.isTransferenciaEnabled()) out.add(PaymentMethod.TRANSFERENCIA);
        return out;
    }

    public PaymentMethod getPredeterminado() {
        var disp = getDisponibles();
        if (disp.contains(PaymentMethod.TARJETA)) return PaymentMethod.TARJETA;
        return disp.isEmpty() ? null : disp.get(0);
    }

    public MetodoPagoConfig getConfig() { return getOrCreate(); }

    @Transactional
    public void bloquearTodo() {
        var cfg = getOrCreate();
        cfg.setPaypalEnabled(false);
        cfg.setBizumEnabled(false);
        cfg.setTransferenciaEnabled(false);
        cfg.setFullUnlocked(false);
        cfg.setUnlockedAt(null);
        cfg.setUnlockedBy(null);
        repo.save(cfg);
    }

    @Transactional
    public UnlockResult unlockAll(String ownerEmail, String tokenSource) {
        var cfg = getOrCreate();
        if (cfg.isFullUnlocked()) {
            return new UnlockResult(true, "Ya estaba desbloqueado.", null);
        }
        var amount = java.math.BigDecimal.valueOf(cfg.getPriceCents() / 100.0);
        var api = new es.uca.orderflow.integration.payments.ExternalPaymentsApi();
        var resp = api.createCharge(new es.uca.orderflow.integration.payments.ExternalPaymentsApi.ChargeRequest(
                amount, "EUR", "Desbloqueo métodos de pago",
                tokenSource == null ? "tok_fake" : tokenSource
        ));
        if (!resp.isSuccess()) {
            return new UnlockResult(false, "Pago rechazado: " + resp.getFailureReason(), null);
        }
        cfg.setPaypalEnabled(true);
        cfg.setBizumEnabled(true);
        cfg.setTransferenciaEnabled(true);
        cfg.setFullUnlocked(true);
        cfg.setUnlockedAt(new java.util.Date());
        cfg.setUnlockedBy(ownerEmail);
        repo.save(cfg);
        return new UnlockResult(true, "Desbloqueo correcto.", resp.getTransactionId());
    }

    public record UnlockResult(boolean success, String message, String txnId) {}
}
