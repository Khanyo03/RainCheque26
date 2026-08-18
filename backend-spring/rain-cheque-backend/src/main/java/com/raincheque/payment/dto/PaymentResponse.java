package com.raincheque.payment.dto;

import com.raincheque.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing the result of a payment operation.
 */
public record PaymentResponse(
        Long paymentId,
        String checkoutUrl,
        String providerName,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime refundedAt
) {
}
