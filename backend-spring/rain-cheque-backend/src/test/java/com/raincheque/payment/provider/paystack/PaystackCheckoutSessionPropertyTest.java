package com.raincheque.payment.provider.paystack;

import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import com.raincheque.payment.dto.PaymentRequest;
import com.raincheque.payment.dto.PaymentResponse;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based test for checkout session response invariants.
 * Validates: Requirements 3.2, 3.4
 *
 * Property 5: Checkout session always produces valid response with PENDING status.
 * For valid payment requests and successful API responses, the resulting PaymentResponse
 * SHALL have status PENDING, a non-null checkoutUrl, and providerName equal to "PAYSTACK".
 */
// Feature: paystack-provider-integration, Property 5: Checkout session always produces valid response with PENDING status
class PaystackCheckoutSessionPropertyTest {

    private PaystackPaymentProvider provider;
    private PaystackApiClient mockApiClient;

    @BeforeTry
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        // Create provider with valid configuration
        PaymentProperties properties = new PaymentProperties();
        PaymentProperties.ProviderConfig config = new PaymentProperties.ProviderConfig();
        config.setEnabled(true);
        config.setApiKey("sk_test_fake_key_for_property_test");
        config.setWebhookSecret("whsec_fake_webhook_secret");
        properties.getProviders().put(PaymentProviderType.PAYSTACK, config);

        provider = new PaystackPaymentProvider(properties);

        // Replace the internal apiClient with a mock via reflection
        mockApiClient = mock(PaystackApiClient.class);
        Field apiClientField = PaystackPaymentProvider.class.getDeclaredField("apiClient");
        apiClientField.setAccessible(true);
        apiClientField.set(provider, mockApiClient);
    }

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 5: Checkout session always produces valid response with PENDING status
    void checkoutSessionAlwaysProducesValidResponseWithPendingStatus(
            @ForAll("validPaymentRequests") PaymentRequest request,
            @ForAll("validAuthorizationUrls") String authorizationUrl) throws Exception {

        // Arrange: mock Paystack API to return a successful initialize response
        PaystackInitializeResponse.PaystackInitializeData data =
                new PaystackInitializeResponse.PaystackInitializeData(
                        authorizationUrl,
                        "ACCESS_" + System.nanoTime(),
                        "REF_" + System.nanoTime()
                );
        PaystackInitializeResponse apiResponse = new PaystackInitializeResponse(true, "Authorization URL created", data);
        when(mockApiClient.initializeTransaction(any(Map.class))).thenReturn(apiResponse);

        // Act
        PaymentResponse response = provider.createCheckoutSession(request);

        // Assert: status is always PENDING
        assertThat(response.status())
                .as("Checkout session response must always have PENDING status")
                .isEqualTo(PaymentStatus.PENDING);

        // Assert: checkoutUrl is non-null
        assertThat(response.checkoutUrl())
                .as("Checkout session response must have a non-null checkoutUrl")
                .isNotNull();

        // Assert: provider name is "PAYSTACK"
        assertThat(response.providerName())
                .as("Checkout session response must have providerName 'PAYSTACK'")
                .isEqualTo("PAYSTACK");
    }

    @Property(tries = 100)
    // Feature: paystack-provider-integration, Property 5: Checkout session always produces valid response with PENDING status
    void checkoutSessionCheckoutUrlMatchesApiAuthorizationUrl(
            @ForAll("validPaymentRequests") PaymentRequest request,
            @ForAll("validAuthorizationUrls") String authorizationUrl) throws Exception {

        // Arrange: mock Paystack API
        PaystackInitializeResponse.PaystackInitializeData data =
                new PaystackInitializeResponse.PaystackInitializeData(
                        authorizationUrl,
                        "ACCESS_" + System.nanoTime(),
                        "REF_" + System.nanoTime()
                );
        PaystackInitializeResponse apiResponse = new PaystackInitializeResponse(true, "Authorization URL created", data);
        when(mockApiClient.initializeTransaction(any(Map.class))).thenReturn(apiResponse);

        // Act
        PaymentResponse response = provider.createCheckoutSession(request);

        // Assert: checkoutUrl matches the authorization_url from the API response
        assertThat(response.checkoutUrl())
                .as("Checkout session checkoutUrl must match the API authorization_url")
                .isEqualTo(authorizationUrl);
    }

    // --- Generators ---

    @Provide
    Arbitrary<PaymentRequest> validPaymentRequests() {
        return Combinators.combine(
                eventIds(),
                ticketIds(),
                validAmounts(),
                validCurrencies(),
                validUrls(),
                validUrls(),
                validEmails(),
                Arbitraries.just(PaymentProviderType.PAYSTACK)
        ).as(PaymentRequest::new);
    }

    @Provide
    Arbitrary<String> validAuthorizationUrls() {
        return Combinators.combine(
                Arbitraries.of("https://checkout.paystack.com/", "https://paystack.com/pay/"),
                Arbitraries.strings().alpha().ofMinLength(8).ofMaxLength(24)
        ).as((base, path) -> base + path.toLowerCase());
    }

    private Arbitrary<Long> eventIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }

    private Arbitrary<Long> ticketIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }

    private Arbitrary<BigDecimal> validAmounts() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("99999.99"))
                .ofScale(2);
    }

    private Arbitrary<String> validCurrencies() {
        return Arbitraries.of("ZAR", "USD", "GBP", "NGN", "GHS", "KES");
    }

    private Arbitrary<String> validUrls() {
        return Arbitraries.of(
                "https://example.com/success",
                "https://example.com/cancel",
                "https://mysite.co.za/payment/success",
                "https://mysite.co.za/payment/cancel",
                "https://app.raincheque.com/checkout/done",
                "https://app.raincheque.com/checkout/cancelled"
        );
    }

    private Arbitrary<String> validEmails() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                Arbitraries.of("example.com", "test.co.za", "mail.org", "company.io")
        ).as((local, domain) -> local.toLowerCase() + "@" + domain);
    }
}
