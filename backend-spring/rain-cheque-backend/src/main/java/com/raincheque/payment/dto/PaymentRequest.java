package com.raincheque.payment.dto;

import com.raincheque.payment.PaymentProviderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

/**
 * Request DTO for initiating a payment checkout session.
 */
public record PaymentRequest(
        @NotNull(message = "Event ID is required")
        Long eventId,

        @NotNull(message = "Ticket ID is required")
        Long ticketId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
        String currency,

        @NotBlank(message = "Success URL is required")
        @URL(message = "Success URL must be a valid URL")
        String successUrl,

        @NotBlank(message = "Cancel URL is required")
        @URL(message = "Cancel URL must be a valid URL")
        String cancelUrl,

        @NotBlank(message = "Customer email is required")
        @Email(message = "Customer email must be a valid email address")
        String customerEmail,

        PaymentProviderType provider
) {
}
