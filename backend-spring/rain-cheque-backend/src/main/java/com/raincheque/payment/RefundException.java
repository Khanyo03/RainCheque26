package com.raincheque.payment;

/**
 * Thrown when a refund operation fails.
 */
public class RefundException extends RuntimeException {

    public RefundException(String message) {
        super(message);
    }

    public RefundException(String message, Throwable cause) {
        super(message, cause);
    }
}
