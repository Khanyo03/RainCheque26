package com.raincheque.payment.provider.stripe;

import com.raincheque.payment.PaymentException;
import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import com.raincheque.payment.dto.PaymentRequest;
import com.raincheque.payment.dto.PaymentResponse;
import com.raincheque.payment.dto.PaymentWebhookEvent;
import com.raincheque.payment.dto.RefundRequest;
import com.raincheque.payment.provider.PaymentProvider;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

/**
 * Stripe implementation of the PaymentProvider interface.
 * <p>
 * Encapsulates all Stripe SDK interactions. All Stripe-specific types are confined
 * to this class and its package — platform DTOs are used at the boundary.
 */
@Component
@ConditionalOnProperty(name = "payment.providers.STRIPE.enabled", havingValue = "true")
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    private final String apiKey;
    private final String webhookSecret;

    public StripePaymentProvider(PaymentProperties properties) {
        PaymentProperties.ProviderConfig config = properties.getProviders().get(PaymentProviderType.STRIPE);
        this.apiKey = config.getApiKey();
        this.webhookSecret = config.getWebhookSecret();
        Stripe.apiKey = this.apiKey;
    }

    @Override
    public PaymentResponse createCheckoutSession(PaymentRequest request) throws PaymentException {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomerEmail(request.customerEmail())
                    .setSuccessUrl(request.successUrl())
                    .setCancelUrl(request.cancelUrl())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(request.currency().toLowerCase())
                                                    .setUnitAmount(request.amount().multiply(BigDecimal.valueOf(100)).longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Ticket Payment")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("eventId", String.valueOf(request.eventId()))
                    .putMetadata("ticketId", String.valueOf(request.ticketId()))
                    .build();

            Session session = Session.create(params);

            log.info("Created Stripe checkout session for event={}, ticket={}", request.eventId(), request.ticketId());

            return new PaymentResponse(
                    null,
                    session.getUrl(),
                    PaymentProviderType.STRIPE.name(),
                    PaymentStatus.PENDING,
                    request.amount(),
                    request.currency(),
                    LocalDateTime.now(),
                    null,
                    null
            );
        } catch (StripeException e) {
            log.error("Failed to create Stripe checkout session: {}", e.getMessage());
            throw new PaymentException(sanitizeErrorMessage(e.getMessage()), e);
        }
    }

    @Override
    public PaymentResponse verifyPayment(String providerReference) throws PaymentException {
        try {
            Session session = Session.retrieve(providerReference);
            PaymentStatus status = mapCheckoutSessionStatus(session.getStatus());

            // If session is complete and has a payment intent, get more details
            BigDecimal amount = BigDecimal.ZERO;
            String currency = "";
            LocalDateTime paidAt = null;

            if (session.getAmountTotal() != null) {
                amount = BigDecimal.valueOf(session.getAmountTotal()).divide(BigDecimal.valueOf(100));
            }
            if (session.getCurrency() != null) {
                currency = session.getCurrency().toUpperCase();
            }

            if ("complete".equals(session.getStatus()) && session.getPaymentIntent() != null) {
                String piId = session.getPaymentIntent();
                PaymentIntent pi = PaymentIntent.retrieve(piId);
                status = mapPaymentIntentStatus(pi.getStatus());
                if (pi.getCreated() != null) {
                    paidAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(pi.getCreated()), ZoneOffset.UTC);
                }
            }

            log.info("Verified Stripe payment reference={}, status={}", providerReference, status);

            return new PaymentResponse(
                    null,
                    null,
                    PaymentProviderType.STRIPE.name(),
                    status,
                    amount,
                    currency,
                    null,
                    paidAt,
                    null
            );
        } catch (StripeException e) {
            log.error("Failed to verify Stripe payment: {}", sanitizeErrorMessage(e.getMessage()));
            throw new PaymentException(sanitizeErrorMessage(e.getMessage()), e);
        }
    }

    @Override
    public PaymentResponse refundPayment(String providerReference, RefundRequest request) throws PaymentException {
        try {
            // Retrieve session to get payment intent
            Session session = Session.retrieve(providerReference);
            String paymentIntentId = session.getPaymentIntent();

            if (paymentIntentId == null || paymentIntentId.isBlank()) {
                throw new PaymentException("Cannot refund: no payment intent associated with session");
            }

            RefundCreateParams.Builder refundBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(request.amount().multiply(BigDecimal.valueOf(100)).longValue());

            if (request.reason() != null && !request.reason().isBlank()) {
                refundBuilder.putMetadata("reason", request.reason());
            }

            Refund refund = Refund.create(refundBuilder.build());

            log.info("Processed Stripe refund for reference={}, amount={}", providerReference, request.amount());

            return new PaymentResponse(
                    null,
                    null,
                    PaymentProviderType.STRIPE.name(),
                    PaymentStatus.REFUNDED,
                    request.amount(),
                    refund.getCurrency() != null ? refund.getCurrency().toUpperCase() : "",
                    null,
                    null,
                    LocalDateTime.now()
            );
        } catch (StripeException e) {
            log.error("Failed to process Stripe refund: {}", sanitizeErrorMessage(e.getMessage()));
            throw new PaymentException(sanitizeErrorMessage(e.getMessage()), e);
        }
    }

    @Override
    public PaymentResponse cancelPayment(String providerReference) throws PaymentException {
        try {
            // Retrieve session to get payment intent
            Session session = Session.retrieve(providerReference);
            String paymentIntentId = session.getPaymentIntent();

            if (paymentIntentId == null || paymentIntentId.isBlank()) {
                throw new PaymentException("Cannot cancel: no payment intent associated with session");
            }

            PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
            pi.cancel(PaymentIntentCancelParams.builder().build());

            log.info("Cancelled Stripe payment for reference={}", providerReference);

            return new PaymentResponse(
                    null,
                    null,
                    PaymentProviderType.STRIPE.name(),
                    PaymentStatus.CANCELLED,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        } catch (StripeException e) {
            log.error("Failed to cancel Stripe payment: {}", sanitizeErrorMessage(e.getMessage()));
            throw new PaymentException(sanitizeErrorMessage(e.getMessage()), e);
        }
    }

    @Override
    public PaymentResponse retrievePayment(String providerReference) throws PaymentException {
        try {
            Session session = Session.retrieve(providerReference);
            PaymentStatus status = mapCheckoutSessionStatus(session.getStatus());

            BigDecimal amount = BigDecimal.ZERO;
            String currency = "";
            LocalDateTime paidAt = null;

            if (session.getAmountTotal() != null) {
                amount = BigDecimal.valueOf(session.getAmountTotal()).divide(BigDecimal.valueOf(100));
            }
            if (session.getCurrency() != null) {
                currency = session.getCurrency().toUpperCase();
            }

            if ("complete".equals(session.getStatus()) && session.getPaymentIntent() != null) {
                PaymentIntent pi = PaymentIntent.retrieve(session.getPaymentIntent());
                status = mapPaymentIntentStatus(pi.getStatus());
                if (pi.getCreated() != null) {
                    paidAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(pi.getCreated()), ZoneOffset.UTC);
                }
            }

            log.info("Retrieved Stripe payment reference={}, status={}", providerReference, status);

            return new PaymentResponse(
                    null,
                    session.getUrl(),
                    PaymentProviderType.STRIPE.name(),
                    status,
                    amount,
                    currency,
                    null,
                    paidAt,
                    null
            );
        } catch (StripeException e) {
            log.error("Failed to retrieve Stripe payment: {}", sanitizeErrorMessage(e.getMessage()));
            throw new PaymentException(sanitizeErrorMessage(e.getMessage()), e);
        }
    }

    @Override
    public List<PaymentResponse> getPaymentHistory(String providerReference) throws PaymentException {
        try {
            // Retrieve the session to return its current state as a single-item history
            Session session = Session.retrieve(providerReference);
            PaymentStatus status = mapCheckoutSessionStatus(session.getStatus());

            BigDecimal amount = BigDecimal.ZERO;
            String currency = "";

            if (session.getAmountTotal() != null) {
                amount = BigDecimal.valueOf(session.getAmountTotal()).divide(BigDecimal.valueOf(100));
            }
            if (session.getCurrency() != null) {
                currency = session.getCurrency().toUpperCase();
            }

            PaymentResponse response = new PaymentResponse(
                    null,
                    session.getUrl(),
                    PaymentProviderType.STRIPE.name(),
                    status,
                    amount,
                    currency,
                    null,
                    null,
                    null
            );

            log.info("Retrieved payment history for reference={}", providerReference);

            return Collections.singletonList(response);
        } catch (StripeException e) {
            log.error("Failed to retrieve Stripe payment history: {}", sanitizeErrorMessage(e.getMessage()));
            throw new PaymentException(sanitizeErrorMessage(e.getMessage()), e);
        }
    }

    @Override
    public PaymentWebhookEvent constructWebhookEvent(String payload, String signature) throws PaymentException {
        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);

            String eventType = event.getType();
            PaymentStatus status = mapWebhookEventType(eventType);
            String paymentId = extractPaymentIdFromEvent(event);

            LocalDateTime timestamp = event.getCreated() != null
                    ? LocalDateTime.ofInstant(Instant.ofEpochSecond(event.getCreated()), ZoneOffset.UTC)
                    : LocalDateTime.now();

            log.info("Processed Stripe webhook event type={}, status={}", eventType, status);

            return new PaymentWebhookEvent(
                    eventType,
                    paymentId,
                    PaymentProviderType.STRIPE,
                    status,
                    payload,
                    timestamp
            );
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed");
            throw new PaymentException("Webhook signature verification failed", e);
        } catch (Exception e) {
            log.error("Failed to construct Stripe webhook event: {}", sanitizeErrorMessage(e.getMessage()));
            throw new PaymentException("Failed to process webhook event", e);
        }
    }

    @Override
    public PaymentProviderType getProviderType() {
        return PaymentProviderType.STRIPE;
    }

    // --- Status Mapping Methods ---

    /**
     * Maps Stripe PaymentIntent status to platform PaymentStatus.
     */
    PaymentStatus mapPaymentIntentStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (stripeStatus) {
            case "requires_payment_method", "requires_confirmation", "requires_action", "requires_capture" ->
                    PaymentStatus.PROCESSING;
            case "processing" -> PaymentStatus.PROCESSING;
            case "succeeded" -> PaymentStatus.SUCCESS;
            case "canceled" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.PENDING;
        };
    }

    /**
     * Maps Stripe Checkout Session status to platform PaymentStatus.
     */
    PaymentStatus mapCheckoutSessionStatus(String sessionStatus) {
        if (sessionStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (sessionStatus) {
            case "open" -> PaymentStatus.PENDING;
            case "complete" -> PaymentStatus.SUCCESS;
            case "expired" -> PaymentStatus.EXPIRED;
            default -> PaymentStatus.PENDING;
        };
    }

    /**
     * Maps Stripe webhook event type to platform PaymentStatus.
     */
    private PaymentStatus mapWebhookEventType(String eventType) {
        if (eventType == null) {
            return PaymentStatus.PENDING;
        }
        return switch (eventType) {
            case "checkout.session.completed" -> PaymentStatus.SUCCESS;
            case "payment_intent.succeeded" -> PaymentStatus.SUCCESS;
            case "payment_intent.payment_failed" -> PaymentStatus.FAILED;
            case "payment_intent.canceled" -> PaymentStatus.CANCELLED;
            case "charge.refunded" -> PaymentStatus.REFUNDED;
            case "checkout.session.expired" -> PaymentStatus.EXPIRED;
            default -> PaymentStatus.PROCESSING;
        };
    }

    /**
     * Extracts the payment/session ID from a Stripe webhook event.
     */
    private String extractPaymentIdFromEvent(Event event) {
        if (event.getData() != null && event.getData().getObject() != null) {
            StripeObject dataObject = event.getData().getObject();
            if (dataObject instanceof Session session) {
                return session.getId();
            } else if (dataObject instanceof PaymentIntent pi) {
                return pi.getId();
            }
        }
        // Fallback: use event ID
        return event.getId();
    }

    /**
     * Sanitizes error messages to remove API keys, internal Stripe IDs, and sensitive data.
     * Returns a user-safe message suitable for external consumption.
     */
    String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "Payment operation failed";
        }

        String sanitized = message;

        // Remove API key patterns (sk_live_xxx, sk_test_xxx, pk_live_xxx, pk_test_xxx)
        sanitized = sanitized.replaceAll("(sk|pk|rk)_(live|test)_[A-Za-z0-9]+", "[REDACTED]");

        // Remove webhook secret patterns (whsec_xxx)
        sanitized = sanitized.replaceAll("whsec_[A-Za-z0-9]+", "[REDACTED]");

        // Remove Stripe internal object IDs (pi_xxx, cs_xxx, ch_xxx, re_xxx, etc.)
        sanitized = sanitized.replaceAll("\\b(pi|cs|ch|re|sub|cus|pm|evt|sess)_[A-Za-z0-9]+", "[REDACTED]");

        // If the message has been heavily redacted, provide a generic message
        if (sanitized.contains("[REDACTED]")) {
            return "Payment operation failed. Please try again or contact support.";
        }

        return sanitized;
    }
}
