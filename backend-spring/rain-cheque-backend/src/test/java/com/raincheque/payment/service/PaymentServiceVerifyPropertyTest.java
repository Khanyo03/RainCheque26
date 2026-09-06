package com.raincheque.payment.service;

import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import com.raincheque.payment.dto.PaymentResponse;
import com.raincheque.payment.entity.Payment;
import com.raincheque.payment.provider.PaymentProvider;
import com.raincheque.payment.provider.PaymentProviderFactory;
import com.raincheque.payment.repository.PaymentRepository;
import com.raincheque.repository.BookingRepository;
import com.raincheque.repository.TicketTierRepository;
import com.raincheque.service.BookingTicketGenerator;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for PaymentService.verifyPayment status synchronization.
 * 
 * Feature: payment-provider-abstraction, Property 6: Payment status synchronization
 * 
 * Validates: Requirements 4.3
 */
class PaymentServiceVerifyPropertyTest {

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 6: Payment status synchronization
    void afterVerifyPaymentEntityStatusMatchesProviderReportedStatus(
            @ForAll("paymentStatuses") PaymentStatus providerReportedStatus) throws Exception {

        // Arrange: set up mocks
        PaymentProviderFactory providerFactory = mock(PaymentProviderFactory.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        PaymentProperties paymentProperties = mock(PaymentProperties.class);

        PaymentProvider provider = mock(PaymentProvider.class);

        // Create a Payment entity with a provider reference
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setProvider(PaymentProviderType.STRIPE);
        payment.setProviderReference("provider-ref-123");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(BigDecimal.valueOf(100.00));
        payment.setCurrency("ZAR");

        // Mock repository to find the payment
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        // Mock factory to return the provider
        when(providerFactory.getProvider(PaymentProviderType.STRIPE)).thenReturn(provider);

        // Mock provider to return a response with the arbitrary status
        PaymentResponse providerResponse = new PaymentResponse(
                1L,
                null,
                "STRIPE",
                providerReportedStatus,
                BigDecimal.valueOf(100.00),
                "ZAR",
                LocalDateTime.now(),
                providerReportedStatus == PaymentStatus.SUCCESS ? LocalDateTime.now() : null,
                null
        );
        when(provider.verifyPayment("provider-ref-123")).thenReturn(providerResponse);

        // Mock save to return the payment as-is (captures the state after modification)
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Create the service under test
        PaymentService paymentService = new PaymentService(
                providerFactory, paymentRepository, bookingRepository, paymentProperties, mock(TicketTierRepository.class), mock(BookingTicketGenerator.class));

        // Act: call verifyPayment
        paymentService.verifyPayment(1L);

        // Assert: the entity's status matches the provider-reported status
        assertThat(payment.getStatus()).isEqualTo(providerReportedStatus);

        // Verify the entity was saved
        verify(paymentRepository).save(payment);
    }

    // --- Generators ---

    @Provide
    Arbitrary<PaymentStatus> paymentStatuses() {
        return Arbitraries.of(PaymentStatus.values());
    }
}
