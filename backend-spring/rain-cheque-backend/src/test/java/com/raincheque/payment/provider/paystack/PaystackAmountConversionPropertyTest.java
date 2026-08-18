package com.raincheque.payment.provider.paystack;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for Paystack amount conversion round-trip.
 * Validates: Requirements 3.1, 5.1, 8.1
 */
// Feature: paystack-provider-integration, Property 1: Amount conversion round-trip
class PaystackAmountConversionPropertyTest {

    @Property(tries = 200)
    // Feature: paystack-provider-integration, Property 1: Amount conversion round-trip
    void amountConversionRoundTrip(@ForAll("randsAmounts") BigDecimal originalAmount) {
        // Convert rands to kobo (multiply by 100, truncate to long) — same as PaystackPaymentProvider
        long kobo = originalAmount.multiply(BigDecimal.valueOf(100)).longValue();

        // Convert kobo back to rands (divide by 100) — same as PaystackPaymentProvider
        BigDecimal convertedBack = BigDecimal.valueOf(kobo).divide(BigDecimal.valueOf(100));

        // The round-trip should produce the original amount (with scale normalized)
        assertThat(convertedBack.compareTo(originalAmount))
                .as("Converting %s to kobo (%d) and back should produce the original amount, got %s",
                        originalAmount, kobo, convertedBack)
                .isEqualTo(0);
    }

    /**
     * Generates random BigDecimal values with up to 2 decimal places,
     * representing valid payment amounts in rands.
     * Range: 0.01 to 999999.99 (positive amounts with at most 2 decimal places)
     */
    @Provide
    Arbitrary<BigDecimal> randsAmounts() {
        // Generate amounts as longs in kobo (1 to 99999999), then divide by 100
        // This ensures we always have at most 2 decimal places
        return Arbitraries.longs().between(1, 99_999_999L)
                .map(kobo -> BigDecimal.valueOf(kobo, 2)); // scale of 2 = divide by 100
    }
}
