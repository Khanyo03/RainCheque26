package com.raincheque.payment.provider.paystack;

public record PaystackWebhookPayload(
    String event,       // "charge.success", "charge.failed", "refund.processed"
    PaystackWebhookData data
) {
    public record PaystackWebhookData(
        String reference,
        String status,
        long amount,
        String currency,
        String paid_at
    ) {}
}
