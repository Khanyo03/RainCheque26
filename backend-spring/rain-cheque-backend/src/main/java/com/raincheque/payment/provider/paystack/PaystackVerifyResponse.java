package com.raincheque.payment.provider.paystack;

/**
 * Maps the JSON response from Paystack GET /transaction/verify/:reference.
 */
public record PaystackVerifyResponse(
    boolean status,
    String message,
    PaystackVerifyData data
) {
    public record PaystackVerifyData(
        String status,          // "success", "failed", "abandoned", "pending"
        String reference,
        long amount,            // in kobo
        String currency,
        String paid_at,         // ISO 8601 timestamp or null
        PaystackCustomer customer,
        PaystackMetadata metadata
    ) {}

    public record PaystackCustomer(String email) {}

    public record PaystackMetadata(String eventId, String ticketId) {}
}
