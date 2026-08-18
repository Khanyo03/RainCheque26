package com.raincheque.payment.provider;

import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.ProviderUnavailableException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory that resolves PaymentProvider implementations at runtime based on provider type.
 * <p>
 * Spring auto-discovers all PaymentProvider beans and this factory filters them by
 * enabled status from configuration, building an internal lookup map. Adding a new
 * provider requires only a new bean — no changes to this factory class.
 */
@Component
public class PaymentProviderFactory {

    private final Map<PaymentProviderType, PaymentProvider> providers;

    /**
     * Constructs the factory by filtering the injected provider list against configuration.
     * Only providers whose type is enabled in {@link PaymentProperties} are registered.
     *
     * @param providerList all PaymentProvider beans discovered by Spring
     * @param properties   payment configuration containing per-provider enabled flags
     */
    public PaymentProviderFactory(List<PaymentProvider> providerList, PaymentProperties properties) {
        this.providers = providerList.stream()
                .filter(p -> properties.isProviderEnabled(p.getProviderType()))
                .collect(Collectors.toMap(
                        PaymentProvider::getProviderType,
                        Function.identity(),
                        (existing, duplicate) -> existing,
                        () -> new EnumMap<>(PaymentProviderType.class)
                ));
    }

    /**
     * Returns the PaymentProvider implementation for the given type.
     *
     * @param type the payment provider type to resolve
     * @return the corresponding PaymentProvider implementation
     * @throws ProviderUnavailableException if the provider is not registered or is disabled
     */
    public PaymentProvider getProvider(PaymentProviderType type) {
        PaymentProvider provider = providers.get(type);
        if (provider == null) {
            throw new ProviderUnavailableException("Payment provider not available: " + type);
        }
        return provider;
    }
}
