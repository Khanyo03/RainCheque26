package com.raincheque.payment.service;

import com.raincheque.entity.Booking;
import com.raincheque.entity.Event;
import com.raincheque.entity.TicketTier;
import com.raincheque.entity.User;
import com.raincheque.exception.ResourceNotFoundException;
import com.raincheque.payment.InvalidPaymentException;
import com.raincheque.payment.PaymentException;
import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import com.raincheque.payment.dto.PaymentRequest;
import com.raincheque.payment.dto.PaymentResponse;
import com.raincheque.payment.dto.PaymentWebhookEvent;
import com.raincheque.payment.dto.RefundRequest;
import com.raincheque.payment.entity.Payment;
import com.raincheque.payment.provider.PaymentProvider;
import com.raincheque.payment.provider.PaymentProviderFactory;
import com.raincheque.payment.repository.PaymentRepository;
import com.raincheque.repository.BookingRepository;
import com.raincheque.repository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Core payment business logic service. Orchestrates payment operations through the
 * PaymentProvider interface without knowledge of any specific payment gateway SDK.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentProviderFactory providerFactory;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentProperties paymentProperties;
    private final TicketTierRepository ticketTierRepository;

    private static final Set<PaymentStatus> TERMINAL_STATUSES = Set.of(
            PaymentStatus.REFUNDED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED
    );

    /**
     * Initiates a checkout session with the resolved payment provider.
     * Persists a Payment entity with PENDING status and returns the checkout URL.
     */
    @Transactional
    public PaymentResponse initiateCheckout(PaymentRequest request) throws PaymentException {
        long startTime = System.currentTimeMillis();

        PaymentProviderType providerType = resolveProviderType(request.provider());
        PaymentProvider provider = providerFactory.getProvider(providerType);

        // Create a new booking for this checkout
        TicketTier ticketTier = ticketTierRepository.findById(request.ticketId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket tier not found: " + request.ticketId()));

        Event event = ticketTier.getEvent();
        if (event == null || !event.getId().equals(request.eventId())) {
            throw new InvalidPaymentException("Ticket tier does not belong to the specified event");
        }

        // Get authenticated user from security context
        User currentUser = getCurrentUser();

        Booking booking = new Booking();
        booking.setUser(currentUser);
        booking.setEvent(event);
        booking.setTicketTier(ticketTier);
        booking.setQuantity(1);
        booking.setTotalAmount(request.amount());
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setBookingTime(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        PaymentResponse providerResponse = provider.createCheckoutSession(request);

        Payment payment = new Payment();
        payment.setBooking(savedBooking);
        payment.setProvider(providerType);
        payment.setProviderReference(providerResponse.checkoutUrl() != null
                ? extractProviderReference(providerResponse) : null);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());

        Payment savedPayment = paymentRepository.save(payment);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Payment checkout initiated: provider={}, paymentId={}, bookingId={}, status=PENDING, duration={}ms",
                providerType, savedPayment.getId(), savedBooking.getId(), duration);

        return new PaymentResponse(
                savedPayment.getId(),
                providerResponse.checkoutUrl(),
                providerType.name(),
                PaymentStatus.PENDING,
                savedPayment.getAmount(),
                savedPayment.getCurrency(),
                savedPayment.getCreatedAt(),
                savedPayment.getPaidAt(),
                savedPayment.getRefundedAt()
        );
    }

    /**
     * Verifies the current payment status by querying the provider.
     * Updates the entity status to match the provider-reported status.
     */
    @Transactional
    public PaymentResponse verifyPayment(Long paymentId) throws PaymentException {
        long startTime = System.currentTimeMillis();

        Payment payment = findPaymentOrThrow(paymentId);
        PaymentProvider provider = providerFactory.getProvider(payment.getProvider());

        PaymentResponse providerResponse = provider.verifyPayment(payment.getProviderReference());

        payment.setStatus(providerResponse.status());
        payment.setProviderStatus(providerResponse.status().name());
        if (providerResponse.status() == PaymentStatus.SUCCESS && payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }

        Payment updatedPayment = paymentRepository.save(payment);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Payment verified: provider={}, paymentId={}, transactionId={}, status={}, duration={}ms",
                payment.getProvider(), paymentId, payment.getProviderTransactionId(),
                updatedPayment.getStatus(), duration);

        return mapToResponse(updatedPayment);
    }

    /**
     * Processes a refund for a completed payment. Only SUCCESS payments can be refunded.
     */
    @Transactional
    public PaymentResponse processRefund(Long paymentId, RefundRequest request) throws PaymentException {
        long startTime = System.currentTimeMillis();

        Payment payment = findPaymentOrThrow(paymentId);
        validateStatusTransition(payment.getStatus(), PaymentStatus.REFUNDED, "refund");

        PaymentProvider provider = providerFactory.getProvider(payment.getProvider());
        provider.refundPayment(payment.getProviderReference(), request);

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        Payment updatedPayment = paymentRepository.save(payment);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Payment refunded: provider={}, paymentId={}, transactionId={}, refundAmount={}, status=REFUNDED, duration={}ms",
                payment.getProvider(), paymentId, payment.getProviderTransactionId(),
                request.amount(), duration);

        return mapToResponse(updatedPayment);
    }

    /**
     * Cancels a payment. Only PENDING or PROCESSING payments can be cancelled.
     */
    @Transactional
    public PaymentResponse cancelPayment(Long paymentId) throws PaymentException {
        long startTime = System.currentTimeMillis();

        Payment payment = findPaymentOrThrow(paymentId);
        validateStatusTransition(payment.getStatus(), PaymentStatus.CANCELLED, "cancel");

        PaymentProvider provider = providerFactory.getProvider(payment.getProvider());
        provider.cancelPayment(payment.getProviderReference());

        payment.setStatus(PaymentStatus.CANCELLED);
        Payment updatedPayment = paymentRepository.save(payment);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Payment cancelled: provider={}, paymentId={}, transactionId={}, status=CANCELLED, duration={}ms",
                payment.getProvider(), paymentId, payment.getProviderTransactionId(), duration);

        return mapToResponse(updatedPayment);
    }

    /**
     * Cancels a payment with ownership verification.
     * Users can only cancel their own payments; admins can cancel any.
     */
    @Transactional
    public PaymentResponse cancelPayment(Long paymentId, User user) throws PaymentException {
        long startTime = System.currentTimeMillis();

        Payment payment = findPaymentOrThrow(paymentId);
        verifyOwnership(payment, user);
        validateStatusTransition(payment.getStatus(), PaymentStatus.CANCELLED, "cancel");

        PaymentProvider provider = providerFactory.getProvider(payment.getProvider());
        provider.cancelPayment(payment.getProviderReference());

        payment.setStatus(PaymentStatus.CANCELLED);
        Payment updatedPayment = paymentRepository.save(payment);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Payment cancelled: provider={}, paymentId={}, transactionId={}, status=CANCELLED, duration={}ms",
                payment.getProvider(), paymentId, payment.getProviderTransactionId(), duration);

        return mapToResponse(updatedPayment);
    }

    /**
     * Handles an incoming webhook event from a payment provider.
     * Verifies signature, normalizes the event, and updates the payment entity.
     */
    @Transactional
    public void handleWebhook(PaymentProviderType providerType, String payload, String signature) throws PaymentException {
        long startTime = System.currentTimeMillis();

        PaymentProvider provider = providerFactory.getProvider(providerType);
        PaymentWebhookEvent event = provider.constructWebhookEvent(payload, signature);

        Payment payment = paymentRepository.findByProviderReference(event.paymentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for provider reference: " + event.paymentId()));

        payment.setStatus(event.status());
        payment.setProviderStatus(event.status().name());
        if (event.status() == PaymentStatus.SUCCESS && payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }
        if (event.status() == PaymentStatus.REFUNDED && payment.getRefundedAt() == null) {
            payment.setRefundedAt(LocalDateTime.now());
        }

        paymentRepository.save(payment);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Webhook processed: provider={}, providerReference={}, eventType={}, status={}, duration={}ms",
                providerType, event.paymentId(), event.eventType(), event.status(), duration);
    }

    /**
     * Retrieves the current payment status and maps to a response DTO.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(Long paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);

        log.info("Payment status retrieved: provider={}, paymentId={}, transactionId={}, status={}",
                payment.getProvider(), paymentId, payment.getProviderTransactionId(), payment.getStatus());

        return mapToResponse(payment);
    }

    /**
     * Retrieves the current payment status with ownership verification.
     * Users can only view their own payments; admins can view any.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(Long paymentId, User user) {
        Payment payment = findPaymentOrThrow(paymentId);
        verifyOwnership(payment, user);

        log.info("Payment status retrieved: provider={}, paymentId={}, transactionId={}, status={}",
                payment.getProvider(), paymentId, payment.getProviderTransactionId(), payment.getStatus());

        return mapToResponse(payment);
    }

    // ---- Private helpers ----

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new AccessDeniedException("User not authenticated");
    }

    /**
     * Verifies that the requesting user owns the payment (via booking).
     * Admins bypass this check.
     */
    private void verifyOwnership(Payment payment, User user) {
        if (user.getRole() == User.Role.ADMIN) {
            return;
        }
        Booking booking = payment.getBooking();
        if (booking.getUser() == null || !booking.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to access this payment");
        }
    }

    private PaymentProviderType resolveProviderType(PaymentProviderType requestedProvider) {
        return requestedProvider != null ? requestedProvider : paymentProperties.getDefaultProvider();
    }

    private Payment findPaymentOrThrow(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
    }

    /**
     * Validates that the current status allows the target transition.
     * <p>
     * Rules:
     * - Only SUCCESS can be refunded
     * - Only PENDING or PROCESSING can be cancelled
     * - Terminal statuses (REFUNDED, CANCELLED, EXPIRED) cannot transition further
     */
    private void validateStatusTransition(PaymentStatus currentStatus, PaymentStatus targetStatus, String operation) {
        if (TERMINAL_STATUSES.contains(currentStatus)) {
            throw new InvalidPaymentException(
                    "Cannot " + operation + " payment with status " + currentStatus + ": payment is in a terminal state");
        }

        if (targetStatus == PaymentStatus.REFUNDED && currentStatus != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentException(
                    "Cannot refund payment with status " + currentStatus + ": only SUCCESS payments can be refunded");
        }

        if (targetStatus == PaymentStatus.CANCELLED
                && currentStatus != PaymentStatus.PENDING
                && currentStatus != PaymentStatus.PROCESSING) {
            throw new InvalidPaymentException(
                    "Cannot cancel payment with status " + currentStatus + ": only PENDING or PROCESSING payments can be cancelled");
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                null,
                payment.getProvider().name(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getCreatedAt(),
                payment.getPaidAt(),
                payment.getRefundedAt()
        );
    }

    private String extractProviderReference(PaymentResponse providerResponse) {
        // The provider response's paymentId field typically contains the provider reference
        // when returned from createCheckoutSession
        return providerResponse.paymentId() != null
                ? providerResponse.paymentId().toString()
                : null;
    }
}
