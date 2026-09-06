package com.raincheque.payment.provider.paystack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raincheque.payment.PaymentException;
import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import com.raincheque.payment.dto.PaymentRequest;
import com.raincheque.payment.dto.PaymentResponse;
import com.raincheque.payment.dto.PaymentWebhookEvent;
import com.raincheque.payment.dto.RefundRequest;
import com.raincheque.payment.provider.PaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Paystack implementation of the PaymentProvider interface.
 * <p>
 * Encapsulates all Paystack API interactions using RestClient via PaystackApiClient.
 * All Paystack-specific types are confined to this class and its package —
 * platform DTOs are used at the boundary.
 */
@Component
@ConditionalOnProperty(name = "payment.providers.PAYSTACK.enabled", havingValue = "true")
@Slf4j
public class PaystackPaymentProvider implements PaymentProvider {

    private final PaystackApiClient apiClient;
    private final String webhookSecret;
    private final ObjectMapper objectMapper;

    public PaystackPaymentProvider(PaymentProperties properties) {
        PaymentProperties.ProviderConfig config = properties.getProviders().get(PaymentProviderType.PAYSTACK);
        this.apiClient = new PaystackApiClient(config.getApiKey());
        this.webhookSecret = config.getWebhookSecret();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public PaymentResponse createCheckoutSession(PaymentRequest request) throws PaymentException {
        return createCheckoutSession(request, UUID.randomUUID().toString());
    }

    @Override
    public PaymentResponse createCheckoutSession(PaymentRequest request, String reference) throws PaymentException {
        try {
            long amountInKobo = request.amount().multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, Object> body = new HashMap<>();
            body.put("amount", amountInKobo);
            body.put("email", request.customerEmail());
            body.put("currency", request.currency().toUpperCase());
            body.put("reference", reference);
            body.put("callback_url", request.successUrl());

            Map<String, String> metadata = new HashMap<>();
            metadata.put("eventId", String.valueOf(request.eventId()));
            metadata.put("ticketId", String.valueOf(request.ticketId()));
            body.put("metadata", metadata);

            PaystackInitializeResponse response = apiClient.initializeTransaction(body);

            log.info("Created Paystack checkout session for event={}, ticket={}, reference={}",
                    request.eventId(), request.ticketId(), reference);

            return new PaymentResponse(
                    null,
                    response.data().authorization_url(),
                    PaymentProviderType.PAYSTACK.name(),
                    PaymentStatus.PENDING,
                    request.amount(),
                    request.currency(),
                    LocalDateTime.now(),
                    null,
                    null
            );
        } catch (Exception e) {
            log.error("Failed to create Paystack checkout session: {}", sanitizeErrorMessage(e.getMessage()));
            throw new PaymentException(sanitizeErrorMessage(e.getMessage()), e);
        }
    }

    @Override
    public PaymentResponse verifyPayment(String providerReference) throws PaymentException {
        try {
            PaystackVerifyResponse response = apiClient.verifyTransaction(providerReference);
            PaystackVerifyResponse.PaystackVerifyData data = response.data();

            PaymentStatus status = mapTransactionStatus(data.status());
            BigDecimal amount = BigDecimal.valueOf(data.amount()).divide(BigDecimal.valueOf(100));
            LocalDateTime paidAt = parsePaidAt(data.paid_at());

            log.info("Verified Paystack payment reference={}, status={}", providerReference, status);

            return new PaymentResponse(
                    null,
                    null,
                    PaymentProviderType.PAYSTACK.name(),
                    status,
                    amount,
                    data.currency() != null ? data.currency().toUpperCase() : "",
                    null,
                    paidAt,
                    null
            );
        } catch (Exception e) {
            log.error("Failed to verify Paystack payment: {}", sanitizeErrorMessage(e.getMessage()));
            throw new PaymentException(sanitizeErrorMessage(e.getMessage()), e);
        }
    }

    @Override
    public PaymentResponse refundPayment(String providerReference, RefundRequest request) throws PaymentException {
        try {
            long amountInKobo = request.amount().multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, Object> body = new HashMap<>();
            body.put("transaction", providerReference);
            body.put("amount", amountInKobo);

            PaystackRefundResponse response = apiClient.createRefund(body);

            log.info("Processed Paystack refund for reference={}, amount={}", providerReference, request.amount());

            return new PaymentResponse(
                    null,
                    null,
                    PaymentProviderType.PAYSTACK.name(),
                    PaymentStatus.REFUNDED,
                    request.amount(),
                    response.data().currency() != null ? response.data().currency().toUpperCase() : "",
                    null,
                    null,
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("Failed to process Paystack refund: {}", sanitizeErrorMessage(e.getMessage()));
            throw new PaymentException(sanitizeErrorMessage(e.getMessage()), e);
        }
    }

    @Override
    public PaymentResponse cancelPayment(String providerReference) throws PaymentException {
        try {
            PaystackVerifyResponse response = apiClient.verifyTransaction(providerReference);
            String transactionStatus = response.data().status();

            if ("success".equals(transactionStatus)) {
                throw new PaymentException("Cannot cancel a completed payment");
            }

            log.info("Cancelled Paystack payment for reference={}", providerReference);

            return new PaymentResponse(
                    null,
                    null,
                    PaymentProviderType.PAYSTACK.name(),
                    PaymentStatus.CANCELLED,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to cancel Paystack payment: {}", sanitizeErrorMessage(e.getMessage()));
            throw new PaymentException(sanitizeErrorMessage(e.getMessage()), e);
        }
    }

    @Override
    public PaymentResponse retrievePayment(String providerReference) throws PaymentException {
        try {
            PaystackVerifyResponse response = apiClient.verifyTransaction(providerReference);
            PaystackVerifyResponse.PaystackVerifyData data = response.data();

            PaymentStatus status = mapTransactionStatus(data.status());
            BigDecimal amount = BigDecimal.valueOf(data.amount()).divide(BigDecimal.valueOf(100));
            LocalDateTime paidAt = parsePaidAt(data.paid_at());

            log.info("Retrieved Paystack payment reference={}, status={}", providerReference, status);

            return new PaymentResponse(
                    null,
                    null,
                    PaymentProviderType.PAYSTACK.name(),
                    status,
                    amount,
                    data.currency() != null ? data.currency().toUpperCase() : "",
                    null,
                    paidAt,
                    null
            );
        } catch (Exception e) {
            log.error("Failed to retrieve Paystack payment: {}", sanitizeErrorMessage(e.getMessage()));
            throw new PaymentException(sanitizeErrorMessage(e.getMessage()), e);
        }
    }

    @Override
    public List<PaymentResponse> getPaymentHistory(String providerReference) throws PaymentException {
        PaymentResponse response = retrievePayment(providerReference);
        log.info("Retrieved payment history for reference={}", providerReference);
        return Collections.singletonList(response);
    }

    @Override
    public PaymentWebhookEvent constructWebhookEvent(String payload, String signature) throws PaymentException {
        try {
            // Compute HMAC SHA-512 of the raw payload
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] computedHash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(computedHash);

            // Constant-time comparison
            byte[] computedBytes = computedSignature.getBytes(StandardCharsets.UTF_8);
            byte[] providedBytes = signature.getBytes(StandardCharsets.UTF_8);

            if (!MessageDigest.isEqual(computedBytes, providedBytes)) {
                throw new PaymentException("Webhook signature verification failed");
            }

            // Parse webhook payload
            PaystackWebhookPayload webhookPayload = objectMapper.readValue(payload, PaystackWebhookPayload.class);
            String eventType = webhookPayload.event();
            String reference = webhookPayload.data().reference();
            PaymentStatus status = mapWebhookEventType(eventType);

            log.info("Processed Paystack webhook event type={}, status={}", eventType, status);

            return new PaymentWebhookEvent(
                    eventType,
                    reference,
                    PaymentProviderType.PAYSTACK,
                    status,
                    payload,
                    LocalDateTime.now()
            );
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to construct Paystack webhook event: {}", sanitizeErrorMessage(e.getMessage()));
            throw new PaymentException("Failed to process webhook event", e);
        }
    }

    @Override
    public PaymentProviderType getProviderType() {
        return PaymentProviderType.PAYSTACK;
    }

    // --- Status Mapping Methods ---

    /**
     * Maps Paystack transaction status to platform PaymentStatus.
     */
    PaymentStatus mapTransactionStatus(String paystackStatus) {
        if (paystackStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (paystackStatus) {
            case "success" -> PaymentStatus.SUCCESS;
            case "failed" -> PaymentStatus.FAILED;
            case "abandoned" -> PaymentStatus.CANCELLED;
            case "pending" -> PaymentStatus.PROCESSING;
            default -> PaymentStatus.PENDING;
        };
    }

    /**
     * Maps Paystack webhook event type to platform PaymentStatus.
     */
    PaymentStatus mapWebhookEventType(String eventType) {
        if (eventType == null) {
            return PaymentStatus.PENDING;
        }
        return switch (eventType) {
            case "charge.success" -> PaymentStatus.SUCCESS;
            case "charge.failed" -> PaymentStatus.FAILED;
            case "refund.processed" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PROCESSING;
        };
    }

    /**
     * Sanitizes error messages to remove API keys, internal Paystack IDs, and sensitive data.
     * Returns a user-safe message suitable for external consumption.
     */
    String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "Payment operation failed";
        }

        String sanitized = message;

        // Remove Paystack secret key patterns (sk_live_xxx, sk_test_xxx)
        sanitized = sanitized.replaceAll("sk_(live|test)_[A-Za-z0-9]+", "[REDACTED]");

        // Remove internal Paystack transaction identifiers
        sanitized = sanitized.replaceAll("\\bPSTK_[A-Za-z0-9]+", "[REDACTED]");

        // If the message has been heavily redacted, provide a generic message
        if (sanitized.contains("[REDACTED]")) {
            return "Payment operation failed. Please try again or contact support.";
        }

        return sanitized;
    }

    /**
     * Parses the paid_at timestamp from Paystack's ISO 8601 format.
     */
    private LocalDateTime parsePaidAt(String paidAt) {
        if (paidAt == null || paidAt.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(paidAt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse paid_at timestamp: {}", paidAt);
            return null;
        }
    }
}
