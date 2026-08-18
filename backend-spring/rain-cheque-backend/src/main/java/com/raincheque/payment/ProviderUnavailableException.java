package com.raincheque.payment;

/**
 * Thrown when a requested payment provider is not registered or is disabled in configuration.
 */
public class ProviderUnavailableException extends RuntimeException {

    public ProviderUnavailableException(String message) {
        super(message);
    }

    public ProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
