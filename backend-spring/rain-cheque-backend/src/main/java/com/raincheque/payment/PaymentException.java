package com.raincheque.payment;

/**
 * Base checked exception for payment operations that interact with external services.
 */
public class PaymentException extends Exception {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
