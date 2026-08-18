package com.raincheque.payment.controller;

import com.raincheque.entity.User;
import com.raincheque.payment.PaymentException;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.dto.PaymentRequest;
import com.raincheque.payment.dto.PaymentResponse;
import com.raincheque.payment.dto.RefundRequest;
import com.raincheque.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing payment endpoints to frontend clients.
 * Delegates all business logic to PaymentService.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiates a checkout session with the configured payment provider.
     *
     * @param request validated payment request with event, ticket, amount, and URLs
     * @return 201 Created with PaymentResponse containing checkout URL
     */
    @PostMapping("/checkout")
    public ResponseEntity<PaymentResponse> initiateCheckout(
            @Valid @RequestBody PaymentRequest request) throws PaymentException {
        log.debug("Checkout request received for eventId={}, ticketId={}", request.eventId(), request.ticketId());
        PaymentResponse response = paymentService.initiateCheckout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves the current status of a payment.
     * Only the payment owner (or an admin) can access a payment.
     *
     * @param id   the payment ID
     * @param user the authenticated user requesting access
     * @return 200 OK with current PaymentResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        PaymentResponse response = paymentService.getPaymentStatus(id, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Processes a refund for a completed payment. Requires ADMIN role.
     *
     * @param id      the payment ID to refund
     * @param request validated refund request with amount and optional reason
     * @return 200 OK with updated PaymentResponse showing REFUNDED status
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> processRefund(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request) throws PaymentException {
        log.debug("Refund request received for paymentId={}, amount={}", id, request.amount());
        PaymentResponse response = paymentService.processRefund(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels a pending or processing payment.
     * Only the payment owner (or an admin) can cancel a payment.
     *
     * @param id   the payment ID to cancel
     * @param user the authenticated user requesting cancellation
     * @return 200 OK with updated PaymentResponse showing CANCELLED status
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) throws PaymentException {
        log.debug("Cancel request received for paymentId={}", id);
        PaymentResponse response = paymentService.cancelPayment(id, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Handles incoming webhook events from payment providers.
     * This endpoint is publicly accessible (no JWT required) since providers cannot authenticate.
     * Returns 200 OK on successful processing or 400 Bad Request if signature verification fails.
     *
     * @param provider    the payment provider type from the URL path
     * @param payload     the raw webhook request body
     * @param httpRequest the HTTP request to extract provider-specific signature headers
     * @return 200 OK on success, 400 Bad Request on signature verification failure
     */
    @PostMapping("/webhook/{provider}")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable PaymentProviderType provider,
            @RequestBody String payload,
            HttpServletRequest httpRequest) {
        String signature = resolveSignatureHeader(provider, httpRequest);
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            paymentService.handleWebhook(provider, payload, signature);
            return ResponseEntity.ok().build();
        } catch (PaymentException e) {
            log.warn("Webhook processing failed for provider={}: {}", provider, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Resolves the provider-specific signature header from the HTTP request.
     *
     * @param provider the payment provider type
     * @param request  the HTTP servlet request
     * @return the signature header value, or null if not recognized
     */
    private String resolveSignatureHeader(PaymentProviderType provider, HttpServletRequest request) {
        return switch (provider) {
            case STRIPE -> request.getHeader("Stripe-Signature");
            case PAYSTACK -> request.getHeader("x-paystack-signature");
            default -> request.getHeader("Stripe-Signature");
        };
    }
}
