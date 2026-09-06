package com.raincheque.payment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for verifying a payment by its provider reference on checkout return.
 */
public record VerifyPaymentRequest(
        @NotBlank(message = "Reference is required")
        String reference
) {
}
