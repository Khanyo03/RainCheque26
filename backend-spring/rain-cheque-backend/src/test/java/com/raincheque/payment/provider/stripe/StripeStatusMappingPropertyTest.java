package com.raincheque.payment.provider.stripe;

import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for Stripe status mapping.
 * Validates: Requirements 3.2
 */
// Feature: payment-provider-abstraction, Property 3: Stripe status mapping covers all provider statuses
class StripeStatusMappingPropertyTest {

    private final StripePaymentProvider provider;

    StripeStatusMappingPropertyTest() {
        // Create a minimal StripePaymentProvider with mock config for testing mapping methods
        PaymentProperties properties = new PaymentProperties();
        PaymentProperties.ProviderConfig stripeConfig = new PaymentProperties.ProviderConfig();
        stripeConfig.setEnabled(true);
        stripeConfig.setApiKey("sk_test_fake_key_for_unit_test");
        stripeConfig.setWebhookSecret("whsec_fake_secret_for_unit_test");
        properties.getProviders().put(PaymentProviderType.STRIPE, stripeConfig);
        provider = new StripePaymentProvider(properties);
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 3: Stripe status mapping covers all provider statuses
    void mapPaymentIntentStatusNeverReturnsNullForKnownStatuses(
            @ForAll("knownPaymentIntentStatuses") String status) {

        PaymentStatus result = provider.mapPaymentIntentStatus(status);

        assertThat(result)
                .as("mapPaymentIntentStatus('%s') must never return null", status)
                .isNotNull()
                .isInstanceOf(PaymentStatus.class);
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 3: Stripe status mapping covers all provider statuses
    void mapCheckoutSessionStatusNeverReturnsNullForKnownStatuses(
            @ForAll("knownCheckoutSessionStatuses") String status) {

        PaymentStatus result = provider.mapCheckoutSessionStatus(status);

        assertThat(result)
                .as("mapCheckoutSessionStatus('%s') must never return null", status)
                .isNotNull()
                .isInstanceOf(PaymentStatus.class);
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 3: Stripe status mapping covers all provider statuses
    void mapPaymentIntentStatusNeverReturnsNullForArbitraryStrings(
            @ForAll("arbitraryStatusStrings") String status) {

        PaymentStatus result = provider.mapPaymentIntentStatus(status);

        assertThat(result)
                .as("mapPaymentIntentStatus('%s') must never return null for arbitrary input", status)
                .isNotNull()
                .isInstanceOf(PaymentStatus.class);
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 3: Stripe status mapping covers all provider statuses
    void mapCheckoutSessionStatusNeverReturnsNullForArbitraryStrings(
            @ForAll("arbitraryStatusStrings") String status) {

        PaymentStatus result = provider.mapCheckoutSessionStatus(status);

        assertThat(result)
                .as("mapCheckoutSessionStatus('%s') must never return null for arbitrary input", status)
                .isNotNull()
                .isInstanceOf(PaymentStatus.class);
    }

    @Example
    // Feature: payment-provider-abstraction, Property 3: Stripe status mapping covers all provider statuses
    void mapPaymentIntentStatusHandlesNullInput() {
        PaymentStatus result = provider.mapPaymentIntentStatus(null);

        assertThat(result)
                .as("mapPaymentIntentStatus(null) must never return null")
                .isNotNull()
                .isInstanceOf(PaymentStatus.class);
    }

    @Example
    // Feature: payment-provider-abstraction, Property 3: Stripe status mapping covers all provider statuses
    void mapCheckoutSessionStatusHandlesNullInput() {
        PaymentStatus result = provider.mapCheckoutSessionStatus(null);

        assertThat(result)
                .as("mapCheckoutSessionStatus(null) must never return null")
                .isNotNull()
                .isInstanceOf(PaymentStatus.class);
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> knownPaymentIntentStatuses() {
        return Arbitraries.of(
                "requires_payment_method",
                "requires_confirmation",
                "requires_action",
                "processing",
                "requires_capture",
                "succeeded",
                "canceled"
        );
    }

    @Provide
    Arbitrary<String> knownCheckoutSessionStatuses() {
        return Arbitraries.of(
                "open",
                "complete",
                "expired"
        );
    }

    @Provide
    Arbitrary<String> arbitraryStatusStrings() {
        return Arbitraries.oneOf(
                // Random alphanumeric strings (unknown statuses)
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
                // Empty string
                Arbitraries.of(""),
                // Strings with special characters
                Arbitraries.strings().withCharRange('!', '~').ofMinLength(1).ofMaxLength(30),
                // Status-like strings that are not real Stripe statuses
                Arbitraries.of(
                        "pending", "completed", "refunded", "failed",
                        "SUCCEEDED", "CANCELED", "OPEN", "COMPLETE",
                        "unknown_status", "requires_something_else"
                )
        );
    }
}
