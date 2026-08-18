package com.raincheque.payment.provider.paystack;

/**
 * Maps the JSON response from Paystack POST /refund.
 */
public record PaystackRefundResponse(
    boolean status,
    String message,
    PaystackRefundData data
) {
    public record PaystackRefundData(
        String status,
        long amount,
        String currency,
        String transaction_reference
    ) {}
}
