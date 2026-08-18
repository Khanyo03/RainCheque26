package com.raincheque.payment.provider.paystack;

import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for Paystack status mapping totality and determinism.
 * Validates: Requirements 4.2, 4.3, 4.4, 4.5, 7.3, 7.4, 7.5
 */
// Feature: paystack-provider-integration, Property 2: Status mapping is total and deterministic
class PaystackStatusMappingPropertyTest {

    private final PaystackPaymentProvider provider;

    PaystackStatusMappingPropertyTest() {
        PaymentProperties properties = new PaymentProperties();
        PaymentProperties.ProviderConfig config = new PaymentProperties.ProviderConfig();
        config.setEnabled(true);
        config.setApiKey("sk_test_fake_key_for_unit_test");
        config.setWebhookSecret("fake_webhook_secret_for_unit_test");
        properties.getProviders().put(PaymentProviderType.PAYSTACK, config);
        provider = new PaystackPaymentProvider(properties);
    }

    // --- Transaction Status Mapping: Totality ---

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 2: Status mapping is total and deterministic
    void mapTransactionStatusNeverReturnsNullForKnownStatuses(
            @ForAll("knownTransactionStatuses") String status) {

        PaymentStatus result = provider.mapTransactionStatus(status);

        assertThat(result)
                .as("mapTransactionStatus('%s') must never return null", status)
                .isNotNull()
                .isInstanceOf(PaymentStatus.class);
    }

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 2: Status mapping is total and deterministic
    void mapTransactionStatusNeverReturnsNullForArbitraryStrings(
            @ForAll("arbitraryStatusStrings") String status) {

        PaymentStatus result = provider.mapTransactionStatus(status);

        assertThat(result)
                .as("mapTransactionStatus('%s') must never return null for arbitrary input", status)
                .isNotNull()
                .isInstanceOf(PaymentStatus.class);
    }

    // --- Transaction Status Mapping: Determinism ---

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 2: Status mapping is total and deterministic
    void mapTransactionStatusIsDeterministic(
            @ForAll("allTransactionStatuses") String status) {

        PaymentStatus first = provider.mapTransactionStatus(status);
        PaymentStatus second = provider.mapTransactionStatus(status);

        assertThat(first)
                .as("mapTransactionStatus('%s') must be deterministic (idempotent)", status)
                .isEqualTo(second);
    }

    // --- Webhook Event Type Mapping: Totality ---

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 2: Status mapping is total and deterministic
    void mapWebhookEventTypeNeverReturnsNullForKnownEvents(
            @ForAll("knownWebhookEventTypes") String eventType) {

        PaymentStatus result = provider.mapWebhookEventType(eventType);

        assertThat(result)
                .as("mapWebhookEventType('%s') must never return null", eventType)
                .isNotNull()
                .isInstanceOf(PaymentStatus.class);
    }

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 2: Status mapping is total and deterministic
    void mapWebhookEventTypeNeverReturnsNullForArbitraryStrings(
            @ForAll("arbitraryEventStrings") String eventType) {

        PaymentStatus result = provider.mapWebhookEventType(eventType);

        assertThat(result)
                .as("mapWebhookEventType('%s') must never return null for arbitrary input", eventType)
                .isNotNull()
                .isInstanceOf(PaymentStatus.class);
    }

    // --- Webhook Event Type Mapping: Determinism ---

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 2: Status mapping is total and deterministic
    void mapWebhookEventTypeIsDeterministic(
            @ForAll("allWebhookEventTypes") String eventType) {

        PaymentStatus first = provider.mapWebhookEventType(eventType);
        PaymentStatus second = provider.mapWebhookEventType(eventType);

        assertThat(first)
                .as("mapWebhookEventType('%s') must be deterministic (idempotent)", eventType)
                .isEqualTo(second);
    }

    // --- Null input handling ---

    @Example
    // Feature: paystack-provider-integration, Property 2: Status mapping is total and deterministic
    void mapTransactionStatusHandlesNullInput() {
        PaymentStatus result = provider.mapTransactionStatus(null);

        assertThat(result)
                .as("mapTransactionStatus(null) must never return null")
                .isNotNull()
                .isInstanceOf(PaymentStatus.class);
    }

    @Example
    // Feature: paystack-provider-integration, Property 2: Status mapping is total and deterministic
    void mapWebhookEventTypeHandlesNullInput() {
        PaymentStatus result = provider.mapWebhookEventType(null);

        assertThat(result)
                .as("mapWebhookEventType(null) must never return null")
                .isNotNull()
                .isInstanceOf(PaymentStatus.class);
    }

    // --- Known status correctness ---

    @Example
    // Feature: paystack-provider-integration, Property 2: Status mapping is total and deterministic
    void mapTransactionStatusMapsKnownValuesCorrectly() {
        assertThat(provider.mapTransactionStatus("success")).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(provider.mapTransactionStatus("failed")).isEqualTo(PaymentStatus.FAILED);
        assertThat(provider.mapTransactionStatus("abandoned")).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(provider.mapTransactionStatus("pending")).isEqualTo(PaymentStatus.PROCESSING);
    }

    @Example
    // Feature: paystack-provider-integration, Property 2: Status mapping is total and deterministic
    void mapWebhookEventTypeMapsKnownValuesCorrectly() {
        assertThat(provider.mapWebhookEventType("charge.success")).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(provider.mapWebhookEventType("charge.failed")).isEqualTo(PaymentStatus.FAILED);
        assertThat(provider.mapWebhookEventType("refund.processed")).isEqualTo(PaymentStatus.REFUNDED);
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> knownTransactionStatuses() {
        return Arbitraries.of("success", "failed", "abandoned", "pending");
    }

    @Provide
    Arbitrary<String> knownWebhookEventTypes() {
        return Arbitraries.of("charge.success", "charge.failed", "refund.processed");
    }

    @Provide
    Arbitrary<String> allTransactionStatuses() {
        return Arbitraries.oneOf(
                Arbitraries.of("success", "failed", "abandoned", "pending"),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30),
                Arbitraries.of("", "unknown", "PENDING", "SUCCESS", "reversed")
        );
    }

    @Provide
    Arbitrary<String> allWebhookEventTypes() {
        return Arbitraries.oneOf(
                Arbitraries.of("charge.success", "charge.failed", "refund.processed"),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(30),
                Arbitraries.of("", "charge.unknown", "refund.pending", "transfer.success")
        );
    }

    @Provide
    Arbitrary<String> arbitraryStatusStrings() {
        return Arbitraries.oneOf(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
                Arbitraries.of(""),
                Arbitraries.strings().withCharRange('!', '~').ofMinLength(1).ofMaxLength(30),
                Arbitraries.of(
                        "pending", "completed", "refunded", "failed",
                        "SUCCESS", "FAILED", "ABANDONED", "PENDING",
                        "unknown_status", "partially_refunded"
                )
        );
    }

    @Provide
    Arbitrary<String> arbitraryEventStrings() {
        return Arbitraries.oneOf(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
                Arbitraries.of(""),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(30),
                Arbitraries.of(
                        "charge.pending", "transfer.success", "refund.failed",
                        "CHARGE.SUCCESS", "charge_success", "invoice.created",
                        "subscription.created", "unknown.event"
                )
        );
    }
}
