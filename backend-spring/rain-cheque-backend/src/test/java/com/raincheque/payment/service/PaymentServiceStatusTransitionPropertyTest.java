package com.raincheque.payment.service;

import com.raincheque.entity.Booking;
import com.raincheque.payment.InvalidPaymentException;
import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import com.raincheque.payment.dto.RefundRequest;
import com.raincheque.payment.entity.Payment;
import com.raincheque.payment.provider.PaymentProvider;
import com.raincheque.payment.provider.PaymentProviderFactory;
import com.raincheque.payment.repository.PaymentRepository;
import com.raincheque.repository.BookingRepository;
import com.raincheque.repository.TicketTierRepository;
import com.raincheque.service.BookingTicketGenerator;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for invalid status transitions.
 * Validates: Requirements 11.5
 *
 * Property 9: Invalid status transitions throw InvalidPaymentException —
 * For any payment in a terminal state (REFUNDED, CANCELLED, EXPIRED), attempting to
 * refund or cancel always throws InvalidPaymentException. Also, attempting to refund
 * a PENDING or PROCESSING payment, or cancel a SUCCESS payment, always throws
 * InvalidPaymentException.
 */
// Feature: payment-provider-abstraction, Property 9: Invalid status transitions throw InvalidPaymentException
class PaymentServiceStatusTransitionPropertyTest {

    private PaymentProviderFactory providerFactory;
    private PaymentRepository paymentRepository;
    private BookingRepository bookingRepository;
    private PaymentProperties paymentProperties;
    private PaymentProvider mockProvider;
    private PaymentService paymentService;

    @BeforeTry
    void setUp() {
        providerFactory = mock(PaymentProviderFactory.class);
        paymentRepository = mock(PaymentRepository.class);
        bookingRepository = mock(BookingRepository.class);
        paymentProperties = mock(PaymentProperties.class);
        mockProvider = mock(PaymentProvider.class);

        paymentService = new PaymentService(providerFactory, paymentRepository, bookingRepository, paymentProperties, mock(TicketTierRepository.class), mock(BookingTicketGenerator.class));

        when(providerFactory.getProvider(any(PaymentProviderType.class))).thenReturn(mockProvider);
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 9: Invalid status transitions throw InvalidPaymentException
    void terminalStatusRefundAlwaysThrowsInvalidPaymentException(
            @ForAll("terminalStatuses") PaymentStatus terminalStatus,
            @ForAll("positiveAmounts") BigDecimal refundAmount) {

        // Arrange: payment in a terminal state
        Payment payment = createPaymentWithStatus(terminalStatus);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        RefundRequest refundRequest = new RefundRequest(refundAmount, "Test refund");

        // Act & Assert: processRefund must throw InvalidPaymentException
        assertThatThrownBy(() -> paymentService.processRefund(payment.getId(), refundRequest))
                .isInstanceOf(InvalidPaymentException.class);
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 9: Invalid status transitions throw InvalidPaymentException
    void terminalStatusCancelAlwaysThrowsInvalidPaymentException(
            @ForAll("terminalStatuses") PaymentStatus terminalStatus) {

        // Arrange: payment in a terminal state
        Payment payment = createPaymentWithStatus(terminalStatus);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        // Act & Assert: cancelPayment must throw InvalidPaymentException
        assertThatThrownBy(() -> paymentService.cancelPayment(payment.getId()))
                .isInstanceOf(InvalidPaymentException.class);
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 9: Invalid status transitions throw InvalidPaymentException
    void pendingOrProcessingRefundAlwaysThrowsInvalidPaymentException(
            @ForAll("pendingOrProcessingStatuses") PaymentStatus status,
            @ForAll("positiveAmounts") BigDecimal refundAmount) {

        // Arrange: payment in PENDING or PROCESSING state (cannot be refunded)
        Payment payment = createPaymentWithStatus(status);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        RefundRequest refundRequest = new RefundRequest(refundAmount, "Test refund");

        // Act & Assert: processRefund must throw InvalidPaymentException
        assertThatThrownBy(() -> paymentService.processRefund(payment.getId(), refundRequest))
                .isInstanceOf(InvalidPaymentException.class);
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 9: Invalid status transitions throw InvalidPaymentException
    void successStatusCancelAlwaysThrowsInvalidPaymentException() {

        // Arrange: payment with SUCCESS status (cannot be cancelled, only refunded)
        Payment payment = createPaymentWithStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        // Act & Assert: cancelPayment must throw InvalidPaymentException
        assertThatThrownBy(() -> paymentService.cancelPayment(payment.getId()))
                .isInstanceOf(InvalidPaymentException.class);
    }

    // --- Generators ---

    @Provide
    Arbitrary<PaymentStatus> terminalStatuses() {
        return Arbitraries.of(PaymentStatus.REFUNDED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED);
    }

    @Provide
    Arbitrary<PaymentStatus> pendingOrProcessingStatuses() {
        return Arbitraries.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING);
    }

    @Provide
    Arbitrary<BigDecimal> positiveAmounts() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("99999.99"))
                .ofScale(2);
    }

    // --- Helper ---

    private Payment createPaymentWithStatus(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(status);
        payment.setProvider(PaymentProviderType.STRIPE);
        payment.setProviderReference("pi_test_reference");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("ZAR");

        Booking booking = new Booking();
        booking.setId(1L);
        payment.setBooking(booking);

        if (status == PaymentStatus.SUCCESS) {
            payment.setPaidAt(LocalDateTime.now().minusHours(1));
        }
        if (status == PaymentStatus.REFUNDED) {
            payment.setRefundedAt(LocalDateTime.now().minusMinutes(30));
        }

        return payment;
    }
}
