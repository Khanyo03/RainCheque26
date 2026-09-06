package com.raincheque.payment.service;

import com.raincheque.entity.Booking;
import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import com.raincheque.payment.dto.PaymentResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based test for refund entity update.
 * Validates: Requirements 4.4, 11.4
 *
 * Property 7: Refund updates entity correctly — After a successful refund,
 * the entity's status is always REFUNDED and refundedAt is set (non-null).
 */
// Feature: payment-provider-abstraction, Property 7: Refund updates entity correctly
class PaymentServiceRefundPropertyTest {

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
    // Feature: payment-provider-abstraction, Property 7: Refund updates entity correctly
    void afterSuccessfulRefundEntityHasStatusRefundedAndRefundedAtIsSet(
            @ForAll("positiveRefundAmounts") BigDecimal refundAmount) throws Exception {

        // Arrange: create a Payment entity with status SUCCESS (only SUCCESS can be refunded)
        Long paymentId = 1L;
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setProvider(PaymentProviderType.STRIPE);
        payment.setProviderReference("pi_test_reference");
        payment.setAmount(refundAmount.add(BigDecimal.TEN)); // original amount > refund amount
        payment.setCurrency("ZAR");
        payment.setPaidAt(LocalDateTime.now().minusHours(1));

        Booking booking = new Booking();
        booking.setId(1L);
        payment.setBooking(booking);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // Mock the provider's refundPayment() to succeed
        PaymentResponse refundResponse = new PaymentResponse(
                paymentId, null, "STRIPE", PaymentStatus.REFUNDED,
                refundAmount, "ZAR", payment.getCreatedAt(), payment.getPaidAt(), LocalDateTime.now()
        );
        when(mockProvider.refundPayment(eq(payment.getProviderReference()), any(RefundRequest.class)))
                .thenReturn(refundResponse);

        // Capture what gets saved to verify the entity state
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: process the refund
        RefundRequest refundRequest = new RefundRequest(refundAmount, "Customer requested refund");
        paymentService.processRefund(paymentId, refundRequest);

        // Assert: the entity saved has status REFUNDED and refundedAt is not null
        verify(paymentRepository).save(argThat(savedPayment -> {
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(savedPayment.getRefundedAt()).isNotNull();
            return true;
        }));
    }

    // --- Generators ---

    @Provide
    Arbitrary<BigDecimal> positiveRefundAmounts() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("99999.99"))
                .ofScale(2);
    }
}
