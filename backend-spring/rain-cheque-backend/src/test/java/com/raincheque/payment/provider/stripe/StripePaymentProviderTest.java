package com.raincheque.payment.provider.stripe;

import com.raincheque.payment.PaymentException;
import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import com.raincheque.payment.dto.PaymentRequest;
import com.raincheque.payment.dto.PaymentResponse;
import com.raincheque.payment.dto.PaymentWebhookEvent;
import com.raincheque.payment.dto.RefundRequest;
import com.stripe.exception.ApiException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StripePaymentProvider.
 * Tests status mapping, error sanitization, and provider operations using MockedStatic for Stripe SDK.
 * Requirements: 3.1, 3.2, 3.3, 3.5, 3.6
 */
class StripePaymentProviderTest {

    private StripePaymentProvider provider;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties();
        PaymentProperties.ProviderConfig stripeConfig = new PaymentProperties.ProviderConfig();
        stripeConfig.setEnabled(true);
        stripeConfig.setApiKey("sk_test_unit_test_key");
        stripeConfig.setWebhookSecret("whsec_unit_test_secret");
        properties.getProviders().put(PaymentProviderType.STRIPE, stripeConfig);
        provider = new StripePaymentProvider(properties);
    }

    // -------------------------------------------------------------------------
    // Status Mapping Tests (Requirements 3.2)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PaymentIntent Status Mapping")
    class PaymentIntentStatusMapping {

        @Test
        @DisplayName("maps 'succeeded' to SUCCESS")
        void mapsSucceededToSuccess() {
            assertThat(provider.mapPaymentIntentStatus("succeeded")).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("maps 'canceled' to CANCELLED")
        void mapsCanceledToCancelled() {
            assertThat(provider.mapPaymentIntentStatus("canceled")).isEqualTo(PaymentStatus.CANCELLED);
        }

        @Test
        @DisplayName("maps 'processing' to PROCESSING")
        void mapsProcessingToProcessing() {
            assertThat(provider.mapPaymentIntentStatus("processing")).isEqualTo(PaymentStatus.PROCESSING);
        }

        @Test
        @DisplayName("maps 'requires_payment_method' to PROCESSING")
        void mapsRequiresPaymentMethodToProcessing() {
            assertThat(provider.mapPaymentIntentStatus("requires_payment_method")).isEqualTo(PaymentStatus.PROCESSING);
        }

        @Test
        @DisplayName("maps 'requires_confirmation' to PROCESSING")
        void mapsRequiresConfirmationToProcessing() {
            assertThat(provider.mapPaymentIntentStatus("requires_confirmation")).isEqualTo(PaymentStatus.PROCESSING);
        }

        @Test
        @DisplayName("maps 'requires_action' to PROCESSING")
        void mapsRequiresActionToProcessing() {
            assertThat(provider.mapPaymentIntentStatus("requires_action")).isEqualTo(PaymentStatus.PROCESSING);
        }

        @Test
        @DisplayName("maps 'requires_capture' to PROCESSING")
        void mapsRequiresCaptureToProcessing() {
            assertThat(provider.mapPaymentIntentStatus("requires_capture")).isEqualTo(PaymentStatus.PROCESSING);
        }

        @Test
        @DisplayName("maps unknown status to PENDING")
        void mapsUnknownToPending() {
            assertThat(provider.mapPaymentIntentStatus("some_unknown_status")).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("maps null to PENDING")
        void mapsNullToPending() {
            assertThat(provider.mapPaymentIntentStatus(null)).isEqualTo(PaymentStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Checkout Session Status Mapping")
    class CheckoutSessionStatusMapping {

        @Test
        @DisplayName("maps 'open' to PENDING")
        void mapsOpenToPending() {
            assertThat(provider.mapCheckoutSessionStatus("open")).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("maps 'complete' to SUCCESS")
        void mapsCompleteToSuccess() {
            assertThat(provider.mapCheckoutSessionStatus("complete")).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("maps 'expired' to EXPIRED")
        void mapsExpiredToExpired() {
            assertThat(provider.mapCheckoutSessionStatus("expired")).isEqualTo(PaymentStatus.EXPIRED);
        }

        @Test
        @DisplayName("maps unknown status to PENDING")
        void mapsUnknownToPending() {
            assertThat(provider.mapCheckoutSessionStatus("unknown_session_status")).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("maps null to PENDING")
        void mapsNullToPending() {
            assertThat(provider.mapCheckoutSessionStatus(null)).isEqualTo(PaymentStatus.PENDING);
        }
    }

    // -------------------------------------------------------------------------
    // Error Sanitization Tests (Requirements 3.6)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Error Message Sanitization")
    class ErrorSanitization {

        @Test
        @DisplayName("removes live API keys from messages")
        void removesLiveApiKeys() {
            String message = "Invalid API Key provided: sk_live_51HG8bCKp0RfAbCdEfGh123456";
            String sanitized = provider.sanitizeErrorMessage(message);
            assertThat(sanitized).doesNotContain("sk_live_");
        }

        @Test
        @DisplayName("removes test API keys from messages")
        void removesTestApiKeys() {
            String message = "Authentication failed with key sk_test_ABCDef123456789abcdef";
            String sanitized = provider.sanitizeErrorMessage(message);
            assertThat(sanitized).doesNotContain("sk_test_");
        }

        @Test
        @DisplayName("removes publishable keys from messages")
        void removesPublishableKeys() {
            String message = "Error with pk_live_testKey123456789";
            String sanitized = provider.sanitizeErrorMessage(message);
            assertThat(sanitized).doesNotContain("pk_live_");
        }

        @Test
        @DisplayName("removes webhook secret patterns")
        void removesWebhookSecrets() {
            String message = "Signature mismatch for whsec_abcdefgh123456789";
            String sanitized = provider.sanitizeErrorMessage(message);
            assertThat(sanitized).doesNotContain("whsec_");
        }

        @Test
        @DisplayName("removes Stripe internal IDs (pi_, cs_, ch_, re_)")
        void removesStripeInternalIds() {
            String message = "No such payment_intent: pi_3MtwBw2eZvKYlo2C0Ojz";
            String sanitized = provider.sanitizeErrorMessage(message);
            assertThat(sanitized).doesNotContain("pi_");
        }

        @Test
        @DisplayName("returns generic fallback for null input")
        void returnsGenericFallbackForNull() {
            String sanitized = provider.sanitizeErrorMessage(null);
            assertThat(sanitized).isNotNull().isNotBlank();
            assertThat(sanitized).isEqualTo("Payment operation failed");
        }

        @Test
        @DisplayName("returns safe message when sensitive data detected")
        void returnsSafeMessageWhenSensitiveDataPresent() {
            String message = "Failed to charge pi_abc123 using sk_test_key123";
            String sanitized = provider.sanitizeErrorMessage(message);
            assertThat(sanitized).doesNotContain("pi_");
            assertThat(sanitized).doesNotContain("sk_test_");
            assertThat(sanitized).contains("Payment operation failed");
        }

        @Test
        @DisplayName("preserves safe messages unchanged")
        void preservesSafeMessages() {
            String message = "Amount must be greater than zero";
            String sanitized = provider.sanitizeErrorMessage(message);
            assertThat(sanitized).isEqualTo("Amount must be greater than zero");
        }
    }

    // -------------------------------------------------------------------------
    // createCheckoutSession Tests (Requirement 3.1)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createCheckoutSession")
    class CreateCheckoutSession {

        @Test
        @DisplayName("returns PaymentResponse with checkout URL and PENDING status")
        void returnsPaymentResponseWithCheckoutUrlAndPendingStatus() throws Exception {
            PaymentRequest request = new PaymentRequest(
                    1L, 2L, new BigDecimal("100.00"), "ZAR",
                    "https://example.com/success", "https://example.com/cancel",
                    "test@example.com", PaymentProviderType.STRIPE
            );

            Session mockSession = mock(Session.class);
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_abc123");
            when(mockSession.getId()).thenReturn("cs_test_abc123");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenReturn(mockSession);

                PaymentResponse response = provider.createCheckoutSession(request);

                assertThat(response).isNotNull();
                assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_abc123");
                assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
                assertThat(response.providerName()).isEqualTo("STRIPE");
                assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
                assertThat(response.currency()).isEqualTo("ZAR");
            }
        }

        @Test
        @DisplayName("wraps StripeException in PaymentException with sanitized message")
        void wrapsStripeExceptionInPaymentException() {
            PaymentRequest request = new PaymentRequest(
                    1L, 2L, new BigDecimal("50.00"), "USD",
                    "https://example.com/success", "https://example.com/cancel",
                    "test@example.com", PaymentProviderType.STRIPE
            );

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenThrow(new InvalidRequestException(
                                "Invalid key sk_test_fakekeyValue123", "param",
                                "req_123", "type", 400, null));

                assertThatThrownBy(() -> provider.createCheckoutSession(request))
                        .isInstanceOf(PaymentException.class)
                        .hasMessageNotContaining("sk_test_");
            }
        }
    }

    // -------------------------------------------------------------------------
    // verifyPayment Tests (Requirement 3.2)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("verifyPayment")
    class VerifyPayment {

        @Test
        @DisplayName("maps completed session with succeeded intent to SUCCESS")
        void mapsCompletedSessionToSuccess() throws Exception {
            Session mockSession = mock(Session.class);
            when(mockSession.getStatus()).thenReturn("complete");
            when(mockSession.getPaymentIntent()).thenReturn("pi_test_intent123");
            when(mockSession.getAmountTotal()).thenReturn(5000L);
            when(mockSession.getCurrency()).thenReturn("zar");

            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getStatus()).thenReturn("succeeded");
            when(mockIntent.getCreated()).thenReturn(1700000000L);

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class);
                 MockedStatic<PaymentIntent> intentMock = mockStatic(PaymentIntent.class)) {

                sessionMock.when(() -> Session.retrieve("cs_test_session"))
                        .thenReturn(mockSession);
                intentMock.when(() -> PaymentIntent.retrieve("pi_test_intent123"))
                        .thenReturn(mockIntent);

                PaymentResponse response = provider.verifyPayment("cs_test_session");

                assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
                assertThat(response.providerName()).isEqualTo("STRIPE");
                assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("50"));
                assertThat(response.currency()).isEqualTo("ZAR");
                assertThat(response.paidAt()).isNotNull();
            }
        }

        @Test
        @DisplayName("maps open session to PENDING")
        void mapsOpenSessionToPending() throws Exception {
            Session mockSession = mock(Session.class);
            when(mockSession.getStatus()).thenReturn("open");
            when(mockSession.getPaymentIntent()).thenReturn(null);
            when(mockSession.getAmountTotal()).thenReturn(3000L);
            when(mockSession.getCurrency()).thenReturn("usd");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.retrieve("cs_test_open_session"))
                        .thenReturn(mockSession);

                PaymentResponse response = provider.verifyPayment("cs_test_open_session");

                assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
            }
        }

        @Test
        @DisplayName("maps expired session to EXPIRED")
        void mapsExpiredSessionToExpired() throws Exception {
            Session mockSession = mock(Session.class);
            when(mockSession.getStatus()).thenReturn("expired");
            when(mockSession.getPaymentIntent()).thenReturn(null);
            when(mockSession.getAmountTotal()).thenReturn(2000L);
            when(mockSession.getCurrency()).thenReturn("eur");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.retrieve("cs_test_expired_session"))
                        .thenReturn(mockSession);

                PaymentResponse response = provider.verifyPayment("cs_test_expired_session");

                assertThat(response.status()).isEqualTo(PaymentStatus.EXPIRED);
            }
        }

        @Test
        @DisplayName("wraps Stripe API errors in PaymentException")
        void wrapsStripeApiErrors() {
            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.retrieve("cs_test_bad"))
                        .thenThrow(new InvalidRequestException(
                                "No such checkout session: cs_test_bad", "id",
                                "req_456", "invalid_request_error", 404, null));

                assertThatThrownBy(() -> provider.verifyPayment("cs_test_bad"))
                        .isInstanceOf(PaymentException.class)
                        .hasMessageNotContaining("cs_test_");
            }
        }
    }

    // -------------------------------------------------------------------------
    // refundPayment Tests (Requirement 3.3)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("refundPayment")
    class RefundPaymentTests {

        @Test
        @DisplayName("returns REFUNDED status on successful refund")
        void returnsRefundedStatus() throws Exception {
            Session mockSession = mock(Session.class);
            when(mockSession.getPaymentIntent()).thenReturn("pi_test_refund_intent");

            Refund mockRefund = mock(Refund.class);
            when(mockRefund.getCurrency()).thenReturn("zar");

            RefundRequest refundRequest = new RefundRequest(new BigDecimal("25.00"), "Customer requested");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class);
                 MockedStatic<Refund> refundMock = mockStatic(Refund.class)) {

                sessionMock.when(() -> Session.retrieve("cs_test_refund"))
                        .thenReturn(mockSession);
                refundMock.when(() -> Refund.create(any(RefundCreateParams.class)))
                        .thenReturn(mockRefund);

                PaymentResponse response = provider.refundPayment("cs_test_refund", refundRequest);

                assertThat(response.status()).isEqualTo(PaymentStatus.REFUNDED);
                assertThat(response.providerName()).isEqualTo("STRIPE");
                assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("25.00"));
                assertThat(response.refundedAt()).isNotNull();
            }
        }

        @Test
        @DisplayName("throws PaymentException when no payment intent associated")
        void throwsWhenNoPaymentIntent() {
            Session mockSession = mock(Session.class);
            when(mockSession.getPaymentIntent()).thenReturn(null);

            RefundRequest refundRequest = new RefundRequest(new BigDecimal("10.00"), null);

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.retrieve("cs_test_no_pi"))
                        .thenReturn(mockSession);

                assertThatThrownBy(() -> provider.refundPayment("cs_test_no_pi", refundRequest))
                        .isInstanceOf(PaymentException.class)
                        .hasMessageContaining("Cannot refund");
            }
        }

        @Test
        @DisplayName("wraps Stripe refund errors in PaymentException with sanitized message")
        void wrapsStripeRefundErrors() {
            Session mockSession = mock(Session.class);
            when(mockSession.getPaymentIntent()).thenReturn("pi_test_refund_err");

            RefundRequest refundRequest = new RefundRequest(new BigDecimal("10.00"), null);

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class);
                 MockedStatic<Refund> refundMock = mockStatic(Refund.class)) {

                sessionMock.when(() -> Session.retrieve("cs_test_refund_err"))
                        .thenReturn(mockSession);
                refundMock.when(() -> Refund.create(any(RefundCreateParams.class)))
                        .thenThrow(new InvalidRequestException(
                                "Charge ch_3MtwBw2eZvKYlo2C has already been refunded", "charge",
                                "req_789", "invalid_request_error", 400, null));

                assertThatThrownBy(() -> provider.refundPayment("cs_test_refund_err", refundRequest))
                        .isInstanceOf(PaymentException.class)
                        .hasMessageNotContaining("ch_");
            }
        }
    }

    // -------------------------------------------------------------------------
    // constructWebhookEvent Tests (Requirement 3.5)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("constructWebhookEvent")
    class ConstructWebhookEvent {

        @Test
        @DisplayName("verifies signature and normalizes checkout.session.completed event")
        void verifiesAndNormalizesCheckoutCompleted() throws Exception {
            String payload = "{\"type\":\"checkout.session.completed\"}";
            String signature = "t=1234567890,v1=abc123";

            // Use deep stubbing for Event.getData().getObject() chain
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_webhook_session");
            Event mockEvent = mock(Event.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
            when(mockEvent.getType()).thenReturn("checkout.session.completed");
            when(mockEvent.getCreated()).thenReturn(1700000000L);
            when(mockEvent.getId()).thenReturn("evt_test_123");
            when(mockEvent.getData().getObject()).thenReturn(mockSession);

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(payload, signature, "whsec_unit_test_secret"))
                        .thenReturn(mockEvent);

                PaymentWebhookEvent result = provider.constructWebhookEvent(payload, signature);

                assertThat(result).isNotNull();
                assertThat(result.eventType()).isEqualTo("checkout.session.completed");
                assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
                assertThat(result.provider()).isEqualTo(PaymentProviderType.STRIPE);
                assertThat(result.paymentId()).isEqualTo("cs_test_webhook_session");
                assertThat(result.rawPayload()).isEqualTo(payload);
                assertThat(result.timestamp()).isNotNull();
            }
        }

        @Test
        @DisplayName("normalizes payment_intent.payment_failed event to FAILED status")
        void normalizesPaymentFailedEvent() throws Exception {
            String payload = "{\"type\":\"payment_intent.payment_failed\"}";
            String signature = "t=1234567890,v1=def456";

            Event mockEvent = mock(Event.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
            when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
            when(mockEvent.getCreated()).thenReturn(1700000100L);
            when(mockEvent.getId()).thenReturn("evt_test_456");

            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_test_webhook_intent");
            when(mockEvent.getData().getObject()).thenReturn(mockIntent);

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(payload, signature, "whsec_unit_test_secret"))
                        .thenReturn(mockEvent);

                PaymentWebhookEvent result = provider.constructWebhookEvent(payload, signature);

                assertThat(result.eventType()).isEqualTo("payment_intent.payment_failed");
                assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
                assertThat(result.paymentId()).isEqualTo("pi_test_webhook_intent");
            }
        }

        @Test
        @DisplayName("throws PaymentException when signature verification fails")
        void throwsOnSignatureVerificationFailure() {
            String payload = "{\"type\":\"checkout.session.completed\"}";
            String badSignature = "invalid_signature";

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(payload, badSignature, "whsec_unit_test_secret"))
                        .thenThrow(new SignatureVerificationException(
                                "No signatures found matching expected signature for payload", "sig_header"));

                assertThatThrownBy(() -> provider.constructWebhookEvent(payload, badSignature))
                        .isInstanceOf(PaymentException.class)
                        .hasMessage("Webhook signature verification failed");
            }
        }

        @Test
        @DisplayName("normalizes charge.refunded event to REFUNDED status")
        void normalizesChargeRefundedEvent() throws Exception {
            String payload = "{\"type\":\"charge.refunded\"}";
            String signature = "t=1234567890,v1=ghi789";

            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("charge.refunded");
            when(mockEvent.getCreated()).thenReturn(1700000200L);
            when(mockEvent.getId()).thenReturn("evt_test_789");

            // No data object — fallback to event ID
            when(mockEvent.getData()).thenReturn(null);

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(payload, signature, "whsec_unit_test_secret"))
                        .thenReturn(mockEvent);

                PaymentWebhookEvent result = provider.constructWebhookEvent(payload, signature);

                assertThat(result.eventType()).isEqualTo("charge.refunded");
                assertThat(result.status()).isEqualTo(PaymentStatus.REFUNDED);
                assertThat(result.paymentId()).isEqualTo("evt_test_789");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Stripe Exception Mapping Tests (Requirement 3.6)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Stripe Exception to PaymentException Mapping")
    class ExceptionMapping {

        @Test
        @DisplayName("AuthenticationException is wrapped with sanitized message")
        void authenticationExceptionWrapped() {
            PaymentRequest request = new PaymentRequest(
                    1L, 2L, new BigDecimal("10.00"), "USD",
                    "https://example.com/success", "https://example.com/cancel",
                    "test@example.com", PaymentProviderType.STRIPE
            );

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenThrow(new AuthenticationException(
                                "Invalid API Key provided: sk_test_123abc", "req_abc",
                                "authentication_error", 401));

                assertThatThrownBy(() -> provider.createCheckoutSession(request))
                        .isInstanceOf(PaymentException.class)
                        .satisfies(ex -> {
                            assertThat(ex.getMessage()).doesNotContain("sk_test_");
                            assertThat(ex.getMessage()).doesNotContain("sk_live_");
                        });
            }
        }

        @Test
        @DisplayName("InvalidRequestException with internal IDs is sanitized")
        void invalidRequestExceptionSanitized() {
            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.retrieve("cs_test_invalid"))
                        .thenThrow(new InvalidRequestException(
                                "No such checkout session: cs_test_invalid", "id",
                                "req_def", "invalid_request_error", 404, null));

                assertThatThrownBy(() -> provider.verifyPayment("cs_test_invalid"))
                        .isInstanceOf(PaymentException.class)
                        .satisfies(ex -> {
                            assertThat(ex.getMessage()).doesNotContain("cs_test_");
                            assertThat(ex.getMessage()).doesNotContain("req_def");
                        });
            }
        }

        @Test
        @DisplayName("Generic API error is wrapped in PaymentException")
        void genericApiErrorWrapped() {
            PaymentRequest request = new PaymentRequest(
                    1L, 2L, new BigDecimal("10.00"), "USD",
                    "https://example.com/success", "https://example.com/cancel",
                    "test@example.com", PaymentProviderType.STRIPE
            );

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenThrow(new ApiException(
                                "Rate limit exceeded", "req_rate", "rate_limit", 429, null));

                assertThatThrownBy(() -> provider.createCheckoutSession(request))
                        .isInstanceOf(PaymentException.class);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Provider Type Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getProviderType returns STRIPE")
    void getProviderTypeReturnsStripe() {
        assertThat(provider.getProviderType()).isEqualTo(PaymentProviderType.STRIPE);
    }
}
