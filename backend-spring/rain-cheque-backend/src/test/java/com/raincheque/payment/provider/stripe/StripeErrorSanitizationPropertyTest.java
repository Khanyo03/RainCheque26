package com.raincheque.payment.provider.stripe;

import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import net.jqwik.api.*;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for provider error sanitization.
 * Validates: Requirements 3.6, 9.2, 9.3
 *
 * Property 4: No PaymentException message thrown by the provider contains API keys
 * (sk_test_/sk_live_/pk_ patterns), webhook secrets (whsec_ patterns), or internal
 * Stripe IDs (pi_/cs_/ch_/re_ patterns).
 */
// Feature: payment-provider-abstraction, Property 4: Provider error sanitization
class StripeErrorSanitizationPropertyTest {

    private final StripePaymentProvider provider;

    StripeErrorSanitizationPropertyTest() {
        PaymentProperties properties = new PaymentProperties();
        Map<PaymentProviderType, PaymentProperties.ProviderConfig> providers = new EnumMap<>(PaymentProviderType.class);
        PaymentProperties.ProviderConfig stripeConfig = new PaymentProperties.ProviderConfig();
        stripeConfig.setEnabled(true);
        stripeConfig.setApiKey("sk_test_dummy");
        stripeConfig.setWebhookSecret("whsec_dummy");
        providers.put(PaymentProviderType.STRIPE, stripeConfig);
        properties.setProviders(providers);
        provider = new StripePaymentProvider(properties);
    }

    @Property(tries = 200)
    // Feature: payment-provider-abstraction, Property 4: Provider error sanitization
    void sanitizedMessageNeverContainsApiKeys(
            @ForAll("messagesWithApiKeys") String rawMessage) {

        String sanitized = provider.sanitizeErrorMessage(rawMessage);

        assertThat(sanitized).doesNotContainPattern("(sk|pk|rk)_(live|test)_[A-Za-z0-9]+");
    }

    @Property(tries = 200)
    // Feature: payment-provider-abstraction, Property 4: Provider error sanitization
    void sanitizedMessageNeverContainsWebhookSecrets(
            @ForAll("messagesWithWebhookSecrets") String rawMessage) {

        String sanitized = provider.sanitizeErrorMessage(rawMessage);

        assertThat(sanitized).doesNotContainPattern("whsec_[A-Za-z0-9]+");
    }

    @Property(tries = 200)
    // Feature: payment-provider-abstraction, Property 4: Provider error sanitization
    void sanitizedMessageNeverContainsStripeInternalIds(
            @ForAll("messagesWithStripeIds") String rawMessage) {

        String sanitized = provider.sanitizeErrorMessage(rawMessage);

        assertThat(sanitized).doesNotContainPattern("\\b(pi|cs|ch|re)_[A-Za-z0-9]+");
    }

    @Property(tries = 200)
    // Feature: payment-provider-abstraction, Property 4: Provider error sanitization
    void sanitizedMessageNeverContainsAnySensitivePattern(
            @ForAll("messagesWithMixedSensitiveData") String rawMessage) {

        String sanitized = provider.sanitizeErrorMessage(rawMessage);

        assertThat(sanitized).doesNotContainPattern("(sk|pk|rk)_(live|test)_[A-Za-z0-9]+");
        assertThat(sanitized).doesNotContainPattern("whsec_[A-Za-z0-9]+");
        assertThat(sanitized).doesNotContainPattern("\\b(pi|cs|ch|re)_[A-Za-z0-9]+");
    }

    @net.jqwik.api.Example
    // Feature: payment-provider-abstraction, Property 4: Provider error sanitization
    void nullMessageReturnsGenericFallback() {
        String sanitized = provider.sanitizeErrorMessage(null);

        assertThat(sanitized).isNotNull();
        assertThat(sanitized).isNotBlank();
        assertThat(sanitized).doesNotContainPattern("(sk|pk|rk)_(live|test)_[A-Za-z0-9]+");
        assertThat(sanitized).doesNotContainPattern("whsec_[A-Za-z0-9]+");
        assertThat(sanitized).doesNotContainPattern("\\b(pi|cs|ch|re)_[A-Za-z0-9]+");
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> messagesWithApiKeys() {
        Arbitrary<String> prefixes = Arbitraries.of("sk_test_", "sk_live_", "pk_test_", "pk_live_", "rk_test_", "rk_live_");
        Arbitrary<String> suffixes = Arbitraries.strings().alpha().numeric().ofMinLength(10).ofMaxLength(32);
        Arbitrary<String> surroundingText = Arbitraries.of(
                "Invalid API key: ",
                "Authentication failed for key ",
                "Error processing with ",
                "Request failed using ",
                " caused payment failure",
                " is not a valid key"
        );

        return Combinators.combine(surroundingText, prefixes, suffixes)
                .as((text, prefix, suffix) -> text + prefix + suffix);
    }

    @Provide
    Arbitrary<String> messagesWithWebhookSecrets() {
        Arbitrary<String> suffixes = Arbitraries.strings().alpha().numeric().ofMinLength(10).ofMaxLength(32);
        Arbitrary<String> surroundingText = Arbitraries.of(
                "Webhook secret mismatch: whsec_",
                "Invalid signature for secret whsec_",
                "Failed to verify using whsec_",
                "Signature computed with whsec_"
        );

        return Combinators.combine(surroundingText, suffixes)
                .as((text, suffix) -> text + suffix);
    }

    @Provide
    Arbitrary<String> messagesWithStripeIds() {
        Arbitrary<String> idPrefixes = Arbitraries.of("pi_", "cs_", "ch_", "re_");
        Arbitrary<String> idSuffixes = Arbitraries.strings().alpha().numeric().ofMinLength(12).ofMaxLength(26);
        Arbitrary<String> surroundingText = Arbitraries.of(
                "No such payment_intent: ",
                "Checkout session not found: ",
                "Charge already refunded: ",
                "Refund failed for ",
                "Cannot cancel ",
                "Resource not found: "
        );

        return Combinators.combine(surroundingText, idPrefixes, idSuffixes)
                .as((text, prefix, suffix) -> text + prefix + suffix);
    }

    @Provide
    Arbitrary<String> messagesWithMixedSensitiveData() {
        return Arbitraries.oneOf(
                messagesWithApiKeys(),
                messagesWithWebhookSecrets(),
                messagesWithStripeIds(),
                // Messages that combine multiple sensitive patterns
                Combinators.combine(
                        Arbitraries.of("pi_", "cs_", "ch_", "re_"),
                        Arbitraries.strings().alpha().numeric().ofMinLength(12).ofMaxLength(20),
                        Arbitraries.of("sk_test_", "sk_live_"),
                        Arbitraries.strings().alpha().numeric().ofMinLength(10).ofMaxLength(24)
                ).as((idPrefix, idSuffix, keyPrefix, keySuffix) ->
                        "Error for " + idPrefix + idSuffix + " with key " + keyPrefix + keySuffix),
                // Messages with all three types
                Combinators.combine(
                        Arbitraries.of("pi_", "cs_"),
                        Arbitraries.strings().alpha().numeric().ofMinLength(12).ofMaxLength(20),
                        Arbitraries.of("sk_test_", "pk_live_"),
                        Arbitraries.strings().alpha().numeric().ofMinLength(10).ofMaxLength(20),
                        Arbitraries.strings().alpha().numeric().ofMinLength(8).ofMaxLength(16)
                ).as((idPrefix, idSuffix, keyPrefix, keySuffix, whsecSuffix) ->
                        idPrefix + idSuffix + " auth " + keyPrefix + keySuffix + " whsec_" + whsecSuffix)
        );
    }
}
