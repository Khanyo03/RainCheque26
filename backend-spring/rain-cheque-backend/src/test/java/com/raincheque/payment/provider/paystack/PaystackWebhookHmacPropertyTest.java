package com.raincheque.payment.provider.paystack;

import com.raincheque.payment.PaymentException;
import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for webhook HMAC SHA-512 verification.
 * Validates: Requirements 7.1, 7.2
 *
 * Property 3: Webhook HMAC verification rejects tampered payloads
 * For any valid webhook payload and secret key, computing the HMAC SHA-512 signature
 * and then modifying any single byte of the payload SHALL cause signature verification to fail.
 */
// Feature: paystack-provider-integration, Property 3: Webhook HMAC verification rejects tampered payloads
class PaystackWebhookHmacPropertyTest {

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 3: Webhook HMAC verification rejects tampered payloads
    void tamperedPayloadIsRejectedByHmacVerification(
            @ForAll("webhookPayloads") String payload,
            @ForAll("webhookSecrets") String secret,
            @ForAll @IntRange(min = 0, max = Integer.MAX_VALUE) int mutationSeed) {

        // Build provider with the generated secret
        PaystackPaymentProvider provider = buildProvider(secret);

        // Compute valid HMAC SHA-512 signature for the original payload
        String validSignature = computeHmacSha512(payload, secret);

        // Mutate a single byte of the payload
        String tamperedPayload = mutatePayload(payload, mutationSeed);

        // Verify that the tampered payload is rejected
        assertThatThrownBy(() -> provider.constructWebhookEvent(tamperedPayload, validSignature))
                .isInstanceOf(PaymentException.class);
    }

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 3: Webhook HMAC verification rejects tampered payloads
    void invalidSignatureIsRejected(
            @ForAll("webhookPayloads") String payload,
            @ForAll("webhookSecrets") String secret,
            @ForAll("webhookSecrets") String wrongSecret) {

        // Skip if secrets happen to be identical
        Assume.that(!secret.equals(wrongSecret));

        PaystackPaymentProvider provider = buildProvider(secret);

        // Compute signature with a different secret
        String wrongSignature = computeHmacSha512(payload, wrongSecret);

        // Verify that the wrong signature is rejected
        assertThatThrownBy(() -> provider.constructWebhookEvent(payload, wrongSignature))
                .isInstanceOf(PaymentException.class);
    }

    // --- Helper Methods ---

    private PaystackPaymentProvider buildProvider(String webhookSecret) {
        PaymentProperties properties = new PaymentProperties();
        Map<PaymentProviderType, PaymentProperties.ProviderConfig> providers = new EnumMap<>(PaymentProviderType.class);
        PaymentProperties.ProviderConfig config = new PaymentProperties.ProviderConfig();
        config.setEnabled(true);
        config.setApiKey("sk_test_dummykey123456");
        config.setWebhookSecret(webhookSecret);
        providers.put(PaymentProviderType.PAYSTACK, config);
        properties.setProviders(providers);
        return new PaystackPaymentProvider(properties);
    }

    private String computeHmacSha512(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC", e);
        }
    }

    private String mutatePayload(String payload, int seed) {
        char[] chars = payload.toCharArray();
        int index = Math.abs(seed) % chars.length;
        // Flip a bit to ensure the character changes
        chars[index] = (char) (chars[index] ^ 1);
        return new String(chars);
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> webhookPayloads() {
        // Generate realistic webhook JSON payloads with varying content
        Arbitrary<String> references = Arbitraries.strings().alpha().numeric()
                .ofMinLength(10).ofMaxLength(24);
        Arbitrary<String> statuses = Arbitraries.of("success", "failed", "pending", "abandoned");
        Arbitrary<String> events = Arbitraries.of("charge.success", "charge.failed", "refund.processed");
        Arbitrary<Long> amounts = Arbitraries.longs().between(100, 10000000);

        return Combinators.combine(events, references, statuses, amounts)
                .as((event, ref, status, amount) ->
                        "{\"event\":\"" + event + "\",\"data\":{\"reference\":\"" + ref +
                                "\",\"status\":\"" + status + "\",\"amount\":" + amount +
                                ",\"currency\":\"ZAR\",\"paid_at\":\"2024-01-15T10:30:00.000Z\"}}");
    }

    @Provide
    Arbitrary<String> webhookSecrets() {
        // Generate secrets of varying length similar to Paystack webhook secrets
        return Arbitraries.strings().alpha().numeric().ofMinLength(16).ofMaxLength(64);
    }
}
