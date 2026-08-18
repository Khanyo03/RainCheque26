package com.raincheque.payment;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for payment providers.
 * Binds to the "payment" prefix in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "payment")
@Data
public class PaymentProperties {

    private Map<PaymentProviderType, ProviderConfig> providers = new EnumMap<>(PaymentProviderType.class);

    private PaymentProviderType defaultProvider = PaymentProviderType.STRIPE;

    /**
     * Returns whether the given provider type is enabled in configuration.
     */
    public boolean isProviderEnabled(PaymentProviderType type) {
        ProviderConfig config = providers.get(type);
        return config != null && config.isEnabled();
    }

    /**
     * Validates that all enabled providers have their required credentials configured.
     * Throws IllegalStateException at startup if an enabled provider is missing credentials.
     */
    @PostConstruct
    public void validateCredentials() {
        for (Map.Entry<PaymentProviderType, ProviderConfig> entry : providers.entrySet()) {
            PaymentProviderType type = entry.getKey();
            ProviderConfig config = entry.getValue();

            if (config.isEnabled()) {
                if (config.getApiKey() == null || config.getApiKey().isBlank()) {
                    throw new IllegalStateException(
                            "Payment provider " + type + " is enabled but missing required apiKey credential");
                }
                if (config.getWebhookSecret() == null || config.getWebhookSecret().isBlank()) {
                    throw new IllegalStateException(
                            "Payment provider " + type + " is enabled but missing required webhookSecret credential");
                }
            }
        }
    }

    /**
     * Per-provider configuration containing credentials and feature flags.
     */
    @Data
    public static class ProviderConfig {
        private boolean enabled;
        private String apiKey;
        private String webhookSecret;
        private Map<String, String> properties = new HashMap<>();
    }
}
