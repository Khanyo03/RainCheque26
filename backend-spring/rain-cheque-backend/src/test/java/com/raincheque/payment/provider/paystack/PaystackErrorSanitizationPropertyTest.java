package com.raincheque.payment.provider.paystack;

import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based test for error sanitization in PaystackPaymentProvider.
 * Validates: Requirements 10.1, 10.2, 10.3
 */
// Feature: paystack-provider-integration, Property 4: Error sanitization never leaks secret keys
class PaystackErrorSanitizationPropertyTest {

    private static final Pattern SECRET_KEY_PATTERN = Pattern.compile("sk_(live|test)_[A-Za-z0-9]+");
    private static final String GENERIC_FALLBACK = "Payment operation failed. Please try again or contact support.";

    private PaystackPaymentProvider provider;

    @BeforeProperty
    void setUp() {
        PaymentProperties properties = mock(PaymentProperties.class);
        PaymentProperties.ProviderConfig config = mock(PaymentProperties.ProviderConfig.class);
        when(config.getApiKey()).thenReturn("sk_test_dummy");
        when(config.getWebhookSecret()).thenReturn("whsec_dummy");
        var providers = new java.util.HashMap<PaymentProviderType, PaymentProperties.ProviderConfig>();
        providers.put(PaymentProviderType.PAYSTACK, config);
        when(properties.getProviders()).thenReturn(providers);
        provider = new PaystackPaymentProvider(properties);
    }

    @Provide
    Arbitrary<String> secretKeys() {
        // Generate realistic Paystack secret key patterns
        Arbitrary<String> prefix = Arbitraries.of("sk_live_", "sk_test_");
        Arbitrary<String> suffix = Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(10)
                .ofMaxLength(40);
        return Combinators.combine(prefix, suffix).as((p, s) -> p + s);
    }

    @Provide
    Arbitrary<String> surroundingText() {
        return Arbitraries.strings()
                .ascii()
                .ofMinLength(0)
                .ofMaxLength(100)
                .filter(s -> !SECRET_KEY_PATTERN.matcher(s).find());
    }

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 4: Error sanitization never leaks secret keys
    void sanitizedOutputNeverContainsSecretKeyPatterns(
            @ForAll("secretKeys") String secretKey,
            @ForAll("surroundingText") String before,
            @ForAll("surroundingText") String after) {

        String input = before + secretKey + after;

        String result = provider.sanitizeErrorMessage(input);

        // The sanitized output must never contain any secret key pattern
        assertThat(SECRET_KEY_PATTERN.matcher(result).find())
                .as("Sanitized message should not contain secret key pattern. Input: '%s', Output: '%s'", input, result)
                .isFalse();
    }

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 4: Error sanitization never leaks secret keys
    void messageContainingSecretKeyReturnsGenericFallback(
            @ForAll("secretKeys") String secretKey,
            @ForAll("surroundingText") String before,
            @ForAll("surroundingText") String after) {

        String input = before + secretKey + after;

        String result = provider.sanitizeErrorMessage(input);

        // When a secret key is present, the output should be the generic fallback
        assertThat(result).isEqualTo(GENERIC_FALLBACK);
    }

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 4: Error sanitization never leaks secret keys
    void multipleSecretKeysInMessageAreAllRedacted(
            @ForAll("secretKeys") String key1,
            @ForAll("secretKeys") String key2,
            @ForAll("surroundingText") String separator) {

        String input = key1 + separator + key2;

        String result = provider.sanitizeErrorMessage(input);

        // No secret key patterns should survive sanitization
        assertThat(SECRET_KEY_PATTERN.matcher(result).find())
                .as("Sanitized message should not contain any secret key pattern")
                .isFalse();
        // Should return the generic fallback
        assertThat(result).isEqualTo(GENERIC_FALLBACK);
    }
}
