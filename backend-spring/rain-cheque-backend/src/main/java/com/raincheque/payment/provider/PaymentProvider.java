package com.raincheque.payment.provider;

import com.raincheque.payment.PaymentException;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.dto.PaymentRequest;
import com.raincheque.payment.dto.PaymentResponse;
import com.raincheque.payment.dto.PaymentWebhookEvent;
import com.raincheque.payment.dto.RefundRequest;

import java.util.List;

/**
 * Unified interface for all payment provider operations.
 * <p>
 * Each payment gateway (Stripe, PayFast, Yoco, etc.) implements this interface,
 * keeping business logic decoupled from provider-specific SDKs. All methods
 * accept and return platform-defined DTOs — provider-specific types never leak
 * beyond the concrete implementation.
 */
public interface PaymentProvider {

    /**
     * Creates a checkout session with the payment provider.
     *
     * @param request the payment request containing amount, currency, and URLs
     * @return a response containing the checkout URL and payment status (PENDING)
     * @throws PaymentException if the provider fails to create the session
     */
    PaymentResponse createCheckoutSession(PaymentRequest request) throws PaymentException;

    /**
     * Verifies the current status of a payment with the provider.
     *
     * @param providerReference the provider's external reference (session/intent ID)
     * @return a response with the current payment status mapped to PaymentStatus
     * @throws PaymentException if the provider fails to retrieve payment information
     */
    PaymentResponse verifyPayment(String providerReference) throws PaymentException;

    /**
     * Initiates a refund for a completed payment.
     *
     * @param providerReference the provider's external reference for the payment
     * @param request           the refund request containing amount and optional reason
     * @return a response with the updated payment status (REFUNDED)
     * @throws PaymentException if the provider fails to process the refund
     */
    PaymentResponse refundPayment(String providerReference, RefundRequest request) throws PaymentException;

    /**
     * Cancels a pending payment with the provider.
     *
     * @param providerReference the provider's external reference for the payment
     * @return a response with status CANCELLED
     * @throws PaymentException if the provider fails to cancel the payment
     */
    PaymentResponse cancelPayment(String providerReference) throws PaymentException;

    /**
     * Retrieves the full details of a payment from the provider.
     *
     * @param providerReference the provider's external reference for the payment
     * @return a response with the current payment details
     * @throws PaymentException if the provider fails to retrieve payment information
     */
    PaymentResponse retrievePayment(String providerReference) throws PaymentException;

    /**
     * Retrieves payment history for a given provider reference.
     *
     * @param providerReference the provider's external reference
     * @return a list of payment responses representing the history
     * @throws PaymentException if the provider fails to retrieve history
     */
    List<PaymentResponse> getPaymentHistory(String providerReference) throws PaymentException;

    /**
     * Constructs and verifies a webhook event from the raw payload and signature.
     * <p>
     * This method verifies the webhook signature using the configured secret,
     * then normalizes the provider-specific event into a platform PaymentWebhookEvent.
     *
     * @param payload   the raw webhook request body
     * @param signature the signature header sent by the provider
     * @return a normalized webhook event with platform-standard status mapping
     * @throws PaymentException if signature verification fails or the event cannot be parsed
     */
    PaymentWebhookEvent constructWebhookEvent(String payload, String signature) throws PaymentException;

    /**
     * Returns the type of payment provider this implementation represents.
     *
     * @return the provider type enum value
     */
    PaymentProviderType getProviderType();
}
