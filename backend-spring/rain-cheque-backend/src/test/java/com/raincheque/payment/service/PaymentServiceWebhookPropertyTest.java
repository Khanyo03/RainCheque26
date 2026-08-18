package com.raincheque.payment.service;

import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import com.raincheque.payment.dto.PaymentWebhookEvent;
import com.raincheque.payment.entity.Payment;
import com.raincheque.payment.provider.PaymentProvider;
import com.raincheque.payment.provider.PaymentProviderFactory;
import com.raincheque.payment.repository.PaymentRepository;
import com.raincheque.repository.BookingRepository;
import com.raincheque.repository.TicketTierRepository;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for PaymentService.handleWebhook entity status update.
 *
 * Feature: payment-provider-abstraction, Property 10: Webhook updates entity status
 *
 * Validates: Requirements 4.5, 11.3
 */
class PaymentServiceWebhookPropertyTest {

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 10: Webhook updates entity status
    void afterProcessingValidWebhookEntityStatusMatchesWebhookEventStatus(
            @ForAll("paymentStatuses") PaymentStatus webhookStatus,
            @ForAll("providerTypes") PaymentProviderType providerType) throws Exception {

        // Arrange: set up mocks
        PaymentProviderFactory providerFactory = mock(PaymentProviderFactory.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        PaymentProperties paymentProperties = mock(PaymentProperties.class);

        PaymentProvider provider = mock(PaymentProvider.class);

        String providerReference = "webhook-ref-" + providerType + "-" + webhookStatus;
        String payload = "{\"type\":\"payment.updated\"}";
        String signature = "sig_test_123";

        // Create a Payment entity with a providerReference
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setProvider(providerType);
        payment.setProviderReference(providerReference);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(BigDecimal.valueOf(50.00));
        payment.setCurrency("ZAR");

        // Mock factory to return the provider for the given type
        when(providerFactory.getProvider(providerType)).thenReturn(provider);

        // Mock provider's constructWebhookEvent to return a PaymentWebhookEvent
        // with the arbitrary status and matching providerReference as paymentId
        PaymentWebhookEvent webhookEvent = new PaymentWebhookEvent(
                "payment.updated",
                providerReference,
                providerType,
                webhookStatus,
                payload,
                LocalDateTime.now()
        );
        when(provider.constructWebhookEvent(payload, signature)).thenReturn(webhookEvent);

        // Mock repository to find the payment by providerReference
        when(paymentRepository.findByProviderReference(providerReference)).thenReturn(Optional.of(payment));

        // Mock save to return the payment as-is (captures the state after modification)
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Create the service under test
        PaymentService paymentService = new PaymentService(
                providerFactory, paymentRepository, bookingRepository, paymentProperties, mock(TicketTierRepository.class));

        // Act: call handleWebhook
        paymentService.handleWebhook(providerType, payload, signature);

        // Assert: the entity's status matches the webhook event's status
        assertThat(payment.getStatus()).isEqualTo(webhookStatus);

        // Verify the entity was saved
        verify(paymentRepository).save(payment);
    }

    // --- Generators ---

    @Provide
    Arbitrary<PaymentStatus> paymentStatuses() {
        return Arbitraries.of(PaymentStatus.values());
    }

    @Provide
    Arbitrary<PaymentProviderType> providerTypes() {
        return Arbitraries.of(PaymentProviderType.values());
    }
}
