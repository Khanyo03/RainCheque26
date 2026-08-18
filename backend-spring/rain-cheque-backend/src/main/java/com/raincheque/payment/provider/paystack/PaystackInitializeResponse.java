package com.raincheque.payment.provider.paystack;

/**
 * Maps the JSON response from Paystack POST /transaction/initialize.
 */
public record PaystackInitializeResponse(
    boolean status,
    String message,
    PaystackInitializeData data
) {
    public record PaystackInitializeData(
        String authorization_url,
        String access_code,
        String reference
    ) {}
}
