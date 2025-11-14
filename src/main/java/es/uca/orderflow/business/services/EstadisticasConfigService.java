// src/main/java/es/uca/orderflow/business/services/EstadisticasConfigService.java
package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.EstadisticasConfig;
import es.uca.orderflow.integration.payments.ExternalPaymentsApi;
import es.uca.orderflow.persistence.data.EstadisticasConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class EstadisticasConfigService {

    private final EstadisticasConfigRepository repo;

    private EstadisticasConfig getOrCreate() {
        return repo.findById(1L).orElseGet(() -> {
            var cfg = EstadisticasConfig.builder()
                    .fullUnlocked(false)
                    .unlockedAt(null)
                    .unlockedBy(null)
                    .priceCents(1990) // 19,90 €
                    .build();
            cfg.setId(null);
            return repo.save(cfg);
        });
    }

    public boolean isUnlocked() {
        return getOrCreate().isFullUnlocked();
    }

    public int getPriceCents() { return getOrCreate().getPriceCents(); }

    @Transactional
    public UnlockResult unlockAll(String ownerEmail, String tokenSource) {
        var cfg = getOrCreate();
        if (cfg.isFullUnlocked()) {
            return new UnlockResult(true, "Ya estaba desbloqueado.", null);
        }
        BigDecimal amount = BigDecimal.valueOf(cfg.getPriceCents() / 100.0);
        ExternalPaymentsApi api = new ExternalPaymentsApi();
        var resp = api.createCharge(new ExternalPaymentsApi.ChargeRequest(
                amount, "EUR", "Desbloqueo módulo de estadísticas",
                tokenSource==null ? "tok_fake" : tokenSource
        ));
        if (!resp.isSuccess()) {
            return new UnlockResult(false, "Pago rechazado: " + resp.getFailureReason(), null);
        }
        cfg.setFullUnlocked(true);
        cfg.setUnlockedAt(new Date());
        cfg.setUnlockedBy(ownerEmail);
        repo.save(cfg);
        return new UnlockResult(true, "Módulo de estadísticas desbloqueado.", resp.getTransactionId());
    }

    public record UnlockResult(boolean success, String message, String txnId) {}
}
