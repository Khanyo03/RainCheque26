package com.raincheque.payment.service;

import com.raincheque.entity.Booking;
import com.raincheque.entity.Event;
import com.raincheque.entity.TicketTier;
import com.raincheque.entity.User;
import com.raincheque.payment.PaymentProperties;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import com.raincheque.payment.dto.PaymentRequest;
import com.raincheque.payment.dto.PaymentResponse;
import com.raincheque.payment.entity.Payment;
import com.raincheque.payment.provider.PaymentProvider;
import com.raincheque.payment.provider.PaymentProviderFactory;
import com.raincheque.payment.repository.PaymentRepository;
import com.raincheque.repository.BookingRepository;
import com.raincheque.repository.TicketTierRepository;
import com.raincheque.payment.PaymentException;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for checkout always producing PENDING status.
 * Validates: Requirements 4.2, 11.2
 *
 * Property 5: For any valid PaymentRequest, initiating checkout always results in a
 * PaymentResponse with status PENDING and a persisted Payment entity with PENDING status.
 */
// Feature: payment-provider-abstraction, Property 5: Checkout always produces PENDING status
class PaymentServiceCheckoutPropertyTest {

    private PaymentProviderFactory providerFactory;
    private PaymentRepository paymentRepository;
    private BookingRepository bookingRepository;
    private PaymentProperties paymentProperties;
    private TicketTierRepository ticketTierRepository;
    private PaymentProvider mockProvider;
    private PaymentService paymentService;

    @BeforeTry
    void setUp() {
        providerFactory = mock(PaymentProviderFactory.class);
        paymentRepository = mock(PaymentRepository.class);
        bookingRepository = mock(BookingRepository.class);
        paymentProperties = mock(PaymentProperties.class);
        ticketTierRepository = mock(TicketTierRepository.class);
        mockProvider = mock(PaymentProvider.class);

        paymentService = new PaymentService(providerFactory, paymentRepository, bookingRepository, paymentProperties, ticketTierRepository);

        // The factory always returns the mock provider
        when(providerFactory.getProvider(any(PaymentProviderType.class))).thenReturn(mockProvider);

        // Default provider type when request.provider() is null
        when(paymentProperties.getDefaultProvider()).thenReturn(PaymentProviderType.STRIPE);

        // Set up SecurityContext with a mock user
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        User mockUser = new User();
        mockUser.setId(1L);
        when(authentication.getPrincipal()).thenReturn(mockUser);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterTry
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 5: Checkout always produces PENDING status
    void checkoutAlwaysProducesPendingStatus(
            @ForAll("validPaymentRequests") PaymentRequest request) throws PaymentException {

        // Arrange: mock ticket tier exists with matching event
        Event event = new Event();
        event.setId(request.eventId());
        TicketTier ticketTier = new TicketTier();
        ticketTier.setId(request.ticketId());
        ticketTier.setEvent(event);
        when(ticketTierRepository.findById(request.ticketId())).thenReturn(Optional.of(ticketTier));

        // Arrange: mock booking save
        Booking savedBooking = new Booking();
        savedBooking.setId(999L);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // Arrange: mock provider returns a checkout response with a URL
        PaymentResponse providerResponse = new PaymentResponse(
                null,
                "https://checkout.stripe.com/session/" + System.nanoTime(),
                "STRIPE",
                PaymentStatus.PENDING,
                request.amount(),
                request.currency(),
                null,
                null,
                null
        );
        when(mockProvider.createCheckoutSession(request)).thenReturn(providerResponse);

        // Arrange: mock repository save returns the payment with an ID
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        PaymentResponse response = paymentService.initiateCheckout(request);

        // Assert 1: The returned PaymentResponse has status PENDING
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);

        // Assert 2: The Payment entity passed to save() has status PENDING
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 5: Checkout always produces PENDING status
    void checkoutResponseStatusIsPendingRegardlessOfProviderType(
            @ForAll("validPaymentRequests") PaymentRequest request,
            @ForAll("providerTypes") PaymentProviderType providerType) throws PaymentException {

        // Override request with specific provider type
        PaymentRequest requestWithProvider = new PaymentRequest(
                request.eventId(),
                request.ticketId(),
                request.amount(),
                request.currency(),
                request.successUrl(),
                request.cancelUrl(),
                request.customerEmail(),
                providerType
        );

        // Arrange: mock ticket tier exists with matching event
        Event event = new Event();
        event.setId(requestWithProvider.eventId());
        TicketTier ticketTier = new TicketTier();
        ticketTier.setId(requestWithProvider.ticketId());
        ticketTier.setEvent(event);
        when(ticketTierRepository.findById(requestWithProvider.ticketId())).thenReturn(Optional.of(ticketTier));

        // Arrange: mock booking save
        Booking savedBooking = new Booking();
        savedBooking.setId(999L);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // Arrange: mock provider returns a checkout response
        PaymentResponse providerResponse = new PaymentResponse(
                null,
                "https://checkout.provider.com/session/abc123",
                providerType.name(),
                PaymentStatus.PENDING,
                requestWithProvider.amount(),
                requestWithProvider.currency(),
                null,
                null,
                null
        );
        when(mockProvider.createCheckoutSession(requestWithProvider)).thenReturn(providerResponse);

        // Arrange: mock repository save
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        PaymentResponse response = paymentService.initiateCheckout(requestWithProvider);

        // Assert: status is always PENDING regardless of provider type
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);

        // Assert: persisted entity also has PENDING status
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    // --- Generators ---

    @Provide
    Arbitrary<PaymentRequest> validPaymentRequests() {
        return Combinators.combine(
                eventIds(),
                ticketIds(),
                validAmounts(),
                validCurrencies(),
                validUrls(),
                validUrls(),
                validEmails(),
                optionalProviderTypes()
        ).as(PaymentRequest::new);
    }

    @Provide
    Arbitrary<PaymentProviderType> providerTypes() {
        return Arbitraries.of(PaymentProviderType.values());
    }

    private Arbitrary<Long> eventIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }

    private Arbitrary<Long> ticketIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }

    private Arbitrary<BigDecimal> validAmounts() {
        // Amounts > 0.01, up to a reasonable max
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("99999.99"))
                .ofScale(2);
    }

    private Arbitrary<String> validCurrencies() {
        // 3-character currency codes
        return Arbitraries.of("USD", "EUR", "GBP", "ZAR", "AUD", "CAD", "JPY", "CHF", "NZD", "SGD");
    }

    private Arbitrary<String> validUrls() {
        return Arbitraries.of(
                "https://example.com/success",
                "https://example.com/cancel",
                "https://mysite.co.za/payment/success",
                "https://mysite.co.za/payment/cancel",
                "https://app.raincheque.com/checkout/done",
                "https://app.raincheque.com/checkout/cancelled"
        );
    }

    private Arbitrary<String> validEmails() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                Arbitraries.of("example.com", "test.co.za", "mail.org", "company.io")
        ).as((local, domain) -> local.toLowerCase() + "@" + domain);
    }

    private Arbitrary<PaymentProviderType> optionalProviderTypes() {
        // Can be null (defaults to configured default) or a specific provider
        return Arbitraries.of(PaymentProviderType.values())
                .injectNull(0.3);
    }
}
