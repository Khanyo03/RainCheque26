package com.raincheque.payment;

/**
 * Thrown when a payment operation violates business rules,
 * such as an invalid status transition or validation failure.
 */
public class InvalidPaymentException extends RuntimeException {

    public InvalidPaymentException(String message) {
        super(message);
    }

    public InvalidPaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
