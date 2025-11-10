package es.uca.orderflow.business.services;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentProviderFactory {
    private final Map<PaymentMethod, PaymentProvider> map;

    public PaymentProviderFactory(java.util.List<PaymentProvider> providers) {
        this.map = providers.stream().collect(java.util.stream.Collectors.toMap(PaymentProvider::method, p -> p));
    }

    public PaymentProvider get(PaymentMethod m) {
        var p = map.get(m);
        if (p == null) throw new IllegalArgumentException("Proveedor de pago no disponible: " + m);
        return p;
    }
}
