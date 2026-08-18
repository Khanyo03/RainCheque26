package com.raincheque.payment.provider;

import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.ProviderUnavailableException;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for PaymentProviderFactory.
 * Validates: Requirements 2.1, 2.2, 2.3
 */
// Feature: payment-provider-abstraction, Property 1: Factory resolves registered providers correctly
// Feature: payment-provider-abstraction, Property 2: Factory rejects unavailable providers
class PaymentProviderFactoryPropertyTest {

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 1: Factory resolves registered providers correctly
    void factoryResolvesRegisteredProvidersCorrectly(
            @ForAll("enabledProviderSubsets") Set<PaymentProviderType> enabledTypes) {

        // Arrange: build mock providers and properties for the enabled types
        List<PaymentProvider> providerList = enabledTypes.stream()
                .map(this::createMockProvider)
                .collect(Collectors.toList());

        PaymentProperties properties = buildProperties(enabledTypes);

        PaymentProviderFactory factory = new PaymentProviderFactory(providerList, properties);

        // Act & Assert: for each enabled type, getProvider returns a provider with matching type
        for (PaymentProviderType type : enabledTypes) {
            PaymentProvider resolved = factory.getProvider(type);
            assertThat(resolved).isNotNull();
            assertThat(resolved.getProviderType()).isEqualTo(type);
        }
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 2: Factory rejects unavailable providers
    void factoryRejectsUnavailableProviders(
            @ForAll("enabledProviderSubsets") Set<PaymentProviderType> enabledTypes,
            @ForAll("providerTypes") PaymentProviderType requestedType) {

        // Only test when the requested type is NOT in the enabled set
        Assume.that(!enabledTypes.contains(requestedType));

        // Arrange: build mock providers and properties for the enabled types
        List<PaymentProvider> providerList = enabledTypes.stream()
                .map(this::createMockProvider)
                .collect(Collectors.toList());

        PaymentProperties properties = buildProperties(enabledTypes);

        PaymentProviderFactory factory = new PaymentProviderFactory(providerList, properties);

        // Act & Assert: requesting an unavailable provider throws ProviderUnavailableException
        assertThatThrownBy(() -> factory.getProvider(requestedType))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessageContaining(requestedType.toString());
    }

    // --- Helper methods ---

    private PaymentProvider createMockProvider(PaymentProviderType type) {
        PaymentProvider mock = mock(PaymentProvider.class);
        when(mock.getProviderType()).thenReturn(type);
        return mock;
    }

    private PaymentProperties buildProperties(Set<PaymentProviderType> enabledTypes) {
        PaymentProperties properties = new PaymentProperties();
        Map<PaymentProviderType, PaymentProperties.ProviderConfig> providers = new EnumMap<>(PaymentProviderType.class);

        for (PaymentProviderType type : PaymentProviderType.values()) {
            PaymentProperties.ProviderConfig config = new PaymentProperties.ProviderConfig();
            config.setEnabled(enabledTypes.contains(type));
            if (enabledTypes.contains(type)) {
                config.setApiKey("test-api-key-" + type.name());
                config.setWebhookSecret("test-webhook-secret-" + type.name());
            }
            providers.put(type, config);
        }

        properties.setProviders(providers);
        return properties;
    }

    // --- Generators ---

    @Provide
    Arbitrary<Set<PaymentProviderType>> enabledProviderSubsets() {
        return Arbitraries.of(PaymentProviderType.values())
                .set()
                .ofMinSize(0)
                .ofMaxSize(PaymentProviderType.values().length);
    }

    @Provide
    Arbitrary<PaymentProviderType> providerTypes() {
        return Arbitraries.of(PaymentProviderType.values());
    }
}
