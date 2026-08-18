package com.raincheque.payment.dto;

import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;

import java.time.LocalDateTime;

/**
 * DTO representing a normalized webhook event from a payment provider.
 */
public record PaymentWebhookEvent(
        String eventType,
        String paymentId,
        PaymentProviderType provider,
        PaymentStatus status,
        String rawPayload,
        LocalDateTime timestamp
) {
}
