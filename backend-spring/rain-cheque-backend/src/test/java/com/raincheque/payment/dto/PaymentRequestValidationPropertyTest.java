package com.raincheque.payment.dto;

import com.raincheque.payment.PaymentProviderType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for PaymentRequest Jakarta Bean Validation.
 * Validates: Requirements 6.1, 6.4
 *
 * Property 8: For any PaymentRequest with at least one invalid field (null required field,
 * negative amount, invalid email, invalid URL, wrong currency length), Jakarta Bean Validation
 * produces at least one violation.
 */
// Feature: payment-provider-abstraction, Property 8: PaymentRequest validation rejects all invalid fields
class PaymentRequestValidationPropertyTest {

    private Validator validator;

    @BeforeTry
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 8: PaymentRequest validation rejects all invalid fields
    void invalidPaymentRequestAlwaysHasViolations(
            @ForAll("invalidPaymentRequests") PaymentRequest request) {

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations)
                .as("PaymentRequest with invalid fields should have at least one violation")
                .isNotEmpty();
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 8: PaymentRequest validation rejects all invalid fields
    void validPaymentRequestHasNoViolations(
            @ForAll("validPaymentRequests") PaymentRequest request) {

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations)
                .as("Fully valid PaymentRequest should produce no violations, but got: " + violations)
                .isEmpty();
    }

    // --- Generators ---

    @Provide
    Arbitrary<PaymentRequest> invalidPaymentRequests() {
        // Generate requests where at least one field is invalid.
        // We pick one of multiple invalid field strategies to cover all constraint types.
        return Arbitraries.oneOf(
                requestWithNullEventId(),
                requestWithNullTicketId(),
                requestWithNullAmount(),
                requestWithZeroOrNegativeAmount(),
                requestWithInvalidCurrencyLength(),
                requestWithBlankSuccessUrl(),
                requestWithInvalidSuccessUrl(),
                requestWithBlankCancelUrl(),
                requestWithInvalidCancelUrl(),
                requestWithBlankEmail(),
                requestWithInvalidEmail()
        );
    }

    @Provide
    Arbitrary<PaymentRequest> validPaymentRequests() {
        return Combinators.combine(
                validEventIds(),
                validTicketIds(),
                validAmounts(),
                validCurrencies(),
                validUrls(),
                validUrls(),
                validEmails(),
                optionalProviderTypes()
        ).as(PaymentRequest::new);
    }

    // --- Invalid field generators ---

    private Arbitrary<PaymentRequest> requestWithNullEventId() {
        return Combinators.combine(
                validTicketIds(),
                validAmounts(),
                validCurrencies(),
                validUrls(),
                validUrls(),
                validEmails(),
                optionalProviderTypes()
        ).as((ticketId, amount, currency, successUrl, cancelUrl, email, provider) ->
                new PaymentRequest(null, ticketId, amount, currency, successUrl, cancelUrl, email, provider));
    }

    private Arbitrary<PaymentRequest> requestWithNullTicketId() {
        return Combinators.combine(
                validEventIds(),
                validAmounts(),
                validCurrencies(),
                validUrls(),
                validUrls(),
                validEmails(),
                optionalProviderTypes()
        ).as((eventId, amount, currency, successUrl, cancelUrl, email, provider) ->
                new PaymentRequest(eventId, null, amount, currency, successUrl, cancelUrl, email, provider));
    }

    private Arbitrary<PaymentRequest> requestWithNullAmount() {
        return Combinators.combine(
                validEventIds(),
                validTicketIds(),
                validCurrencies(),
                validUrls(),
                validUrls(),
                validEmails(),
                optionalProviderTypes()
        ).as((eventId, ticketId, currency, successUrl, cancelUrl, email, provider) ->
                new PaymentRequest(eventId, ticketId, null, currency, successUrl, cancelUrl, email, provider));
    }

    private Arbitrary<PaymentRequest> requestWithZeroOrNegativeAmount() {
        return Combinators.combine(
                validEventIds(),
                validTicketIds(),
                invalidAmounts(),
                validCurrencies(),
                validUrls(),
                validUrls(),
                validEmails(),
                optionalProviderTypes()
        ).as(PaymentRequest::new);
    }

    private Arbitrary<PaymentRequest> requestWithInvalidCurrencyLength() {
        return Combinators.combine(
                validEventIds(),
                validTicketIds(),
                validAmounts(),
                invalidCurrencies(),
                validUrls(),
                validUrls(),
                validEmails(),
                optionalProviderTypes()
        ).as(PaymentRequest::new);
    }

    private Arbitrary<PaymentRequest> requestWithBlankSuccessUrl() {
        return Combinators.combine(
                validEventIds(),
                validTicketIds(),
                validAmounts(),
                validCurrencies(),
                blankStrings(),
                validUrls(),
                validEmails(),
                optionalProviderTypes()
        ).as(PaymentRequest::new);
    }

    private Arbitrary<PaymentRequest> requestWithInvalidSuccessUrl() {
        return Combinators.combine(
                validEventIds(),
                validTicketIds(),
                validAmounts(),
                validCurrencies(),
                invalidUrls(),
                validUrls(),
                validEmails(),
                optionalProviderTypes()
        ).as(PaymentRequest::new);
    }

    private Arbitrary<PaymentRequest> requestWithBlankCancelUrl() {
        return Combinators.combine(
                validEventIds(),
                validTicketIds(),
                validAmounts(),
                validCurrencies(),
                validUrls(),
                blankStrings(),
                validEmails(),
                optionalProviderTypes()
        ).as(PaymentRequest::new);
    }

    private Arbitrary<PaymentRequest> requestWithInvalidCancelUrl() {
        return Combinators.combine(
                validEventIds(),
                validTicketIds(),
                validAmounts(),
                validCurrencies(),
                validUrls(),
                invalidUrls(),
                validEmails(),
                optionalProviderTypes()
        ).as(PaymentRequest::new);
    }

    private Arbitrary<PaymentRequest> requestWithBlankEmail() {
        return Combinators.combine(
                validEventIds(),
                validTicketIds(),
                validAmounts(),
                validCurrencies(),
                validUrls(),
                validUrls(),
                blankStrings(),
                optionalProviderTypes()
        ).as(PaymentRequest::new);
    }

    private Arbitrary<PaymentRequest> requestWithInvalidEmail() {
        return Combinators.combine(
                validEventIds(),
                validTicketIds(),
                validAmounts(),
                validCurrencies(),
                validUrls(),
                validUrls(),
                invalidEmails(),
                optionalProviderTypes()
        ).as(PaymentRequest::new);
    }

    // --- Valid field generators ---

    private Arbitrary<Long> validEventIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }

    private Arbitrary<Long> validTicketIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }

    private Arbitrary<BigDecimal> validAmounts() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("99999.99"))
                .ofScale(2);
    }

    private Arbitrary<String> validCurrencies() {
        return Arbitraries.of("USD", "EUR", "GBP", "ZAR", "AUD", "CAD", "JPY", "CHF");
    }

    private Arbitrary<String> validUrls() {
        return Arbitraries.of(
                "https://example.com/success",
                "https://example.com/cancel",
                "https://mysite.co.za/payment/done",
                "https://app.raincheque.com/checkout/complete"
        );
    }

    private Arbitrary<String> validEmails() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(8),
                Arbitraries.of("example.com", "test.co.za", "mail.org", "company.io")
        ).as((local, domain) -> local.toLowerCase() + "@" + domain);
    }

    private Arbitrary<PaymentProviderType> optionalProviderTypes() {
        return Arbitraries.of(PaymentProviderType.values()).injectNull(0.3);
    }

    // --- Invalid field generators ---

    private Arbitrary<BigDecimal> invalidAmounts() {
        return Arbitraries.oneOf(
                Arbitraries.just(BigDecimal.ZERO),
                Arbitraries.bigDecimals()
                        .between(new BigDecimal("-10000.00"), new BigDecimal("-0.01"))
                        .ofScale(2),
                Arbitraries.just(new BigDecimal("0.001")),
                Arbitraries.just(new BigDecimal("0.009"))
        );
    }

    private Arbitrary<String> invalidCurrencies() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),         // empty (also blank -> triggers @NotBlank)
                Arbitraries.just("U"),        // 1 char
                Arbitraries.just("US"),       // 2 chars
                Arbitraries.just("USDD"),     // 4 chars
                Arbitraries.just("ABCDE"),    // 5 chars
                Arbitraries.strings().alpha().ofMinLength(4).ofMaxLength(10)  // random 4+ chars
        );
    }

    private Arbitrary<String> invalidUrls() {
        return Arbitraries.oneOf(
                Arbitraries.just("not-a-url"),
                Arbitraries.just("randomtext"),
                Arbitraries.just("www.no-protocol.com/path"),
                Arbitraries.just("just some text with spaces")
        );
    }

    private Arbitrary<String> invalidEmails() {
        // These are guaranteed to fail Hibernate's @Email validator
        return Arbitraries.oneOf(
                Arbitraries.just("noatsignatall"),
                Arbitraries.just("double@@example.com"),
                Arbitraries.just("has space@example.com"),
                Arbitraries.just("@"),
                Arbitraries.just("user name@example.com")
        );
    }

    private Arbitrary<String> blankStrings() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.just("\t"),
                Arbitraries.just("\n")
        );
    }
}
