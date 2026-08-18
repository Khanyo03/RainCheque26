package com.raincheque.payment.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.raincheque.entity.Booking;
import com.raincheque.entity.Event;
import com.raincheque.entity.TicketTier;
import com.raincheque.entity.User;
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
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for sensitive data never appearing in logs.
 * Validates: Requirements 10.2
 *
 * Property 11: For any payment operation (checkout, verify, refund, cancel, webhook)
 * using arbitrary inputs containing sensitive patterns, the log output never contains
 * API keys, card numbers, webhook secrets, or customer payment details.
 *
 * The PaymentService logs operational metadata (provider name, payment ID, status, duration)
 * but must never log sensitive configuration (API keys, webhook secrets), customer PII
 * (email addresses), or financial instrument data (card numbers).
 */
// Feature: payment-provider-abstraction, Property 11: Sensitive data never appears in logs
class PaymentServiceLoggingPropertyTest {

    private PaymentProviderFactory providerFactory;
    private PaymentRepository paymentRepository;
    private BookingRepository bookingRepository;
    private PaymentProperties paymentProperties;
    private TicketTierRepository ticketTierRepository;
    private PaymentProvider mockProvider;
    private PaymentService paymentService;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger paymentServiceLogger;

    // Sensitive values that represent secrets the service must never log
    private static final String SECRET_API_KEY = "sk_test_4eC39HqLyjWDarjtT1zdp7dc";
    private static final String SECRET_LIVE_KEY = "sk_live_51HGk9aB4cZ8qAbCdEfGh123";
    private static final String PUBLIC_KEY = "pk_test_TYooMQauvdEDq54NiTphI7jx";
    private static final String WEBHOOK_SECRET = "whsec_test123abc456def789ghi012";
    private static final String CARD_NUMBER = "4242424242424242";
    private static final String CARD_NUMBER_2 = "5555555555554444";

    // Patterns for sensitive data detection
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "(sk_test_|sk_live_|pk_test_|pk_live_)[a-zA-Z0-9]{10,}");
    private static final Pattern WEBHOOK_SECRET_PATTERN = Pattern.compile(
            "whsec_[a-zA-Z0-9]{10,}");
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile(
            "\\d{16}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    @BeforeTry
    void setUp() {
        providerFactory = mock(PaymentProviderFactory.class);
        paymentRepository = mock(PaymentRepository.class);
        bookingRepository = mock(BookingRepository.class);
        paymentProperties = mock(PaymentProperties.class);
        ticketTierRepository = mock(TicketTierRepository.class);
        mockProvider = mock(PaymentProvider.class);

        paymentService = new PaymentService(providerFactory, paymentRepository, bookingRepository, paymentProperties, ticketTierRepository);

        // Set up log capture
        paymentServiceLogger = (Logger) LoggerFactory.getLogger(PaymentService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        paymentServiceLogger.addAppender(logAppender);

        // Default mock behavior
        when(providerFactory.getProvider(any(PaymentProviderType.class))).thenReturn(mockProvider);
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
        paymentServiceLogger.detachAppender(logAppender);
        logAppender.stop();
        SecurityContextHolder.clearContext();
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 11: Sensitive data never appears in logs
    void checkoutLogsNeverContainSensitiveData(
            @ForAll("sensitivePaymentRequests") PaymentRequest request) throws PaymentException {

        // Arrange: mock ticket tier with matching event
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

        PaymentResponse providerResponse = new PaymentResponse(
                null,
                "https://checkout.stripe.com/session/cs_test_abc123",
                "STRIPE",
                PaymentStatus.PENDING,
                request.amount(),
                request.currency(),
                null, null, null
        );
        when(mockProvider.createCheckoutSession(request)).thenReturn(providerResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        paymentService.initiateCheckout(request);

        // Assert: no sensitive data in any log message
        assertNoSensitiveDataInLogs(request.customerEmail());
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 11: Sensitive data never appears in logs
    void verifyPaymentLogsNeverContainSensitiveData(
            @ForAll("providerTypes") PaymentProviderType providerType) throws PaymentException {

        // Arrange: payment entity with normal (non-sensitive) operational fields
        Payment payment = createNormalPayment(providerType);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentResponse providerResponse = new PaymentResponse(
                1L, null, providerType.name(), PaymentStatus.SUCCESS,
                payment.getAmount(), payment.getCurrency(),
                LocalDateTime.now(), LocalDateTime.now(), null
        );
        when(mockProvider.verifyPayment(payment.getProviderReference())).thenReturn(providerResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        paymentService.verifyPayment(1L);

        // Assert: logs don't contain API keys, webhook secrets, card numbers, or emails
        assertNoSensitiveDataInLogs(null);
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 11: Sensitive data never appears in logs
    void refundLogsNeverContainSensitiveData(
            @ForAll("validRefundAmounts") BigDecimal amount,
            @ForAll("providerTypes") PaymentProviderType providerType) throws PaymentException {

        // Arrange: payment with SUCCESS status
        Payment payment = createNormalPayment(providerType);
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        RefundRequest refundRequest = new RefundRequest(amount, "customer request");

        PaymentResponse providerResponse = new PaymentResponse(
                1L, null, providerType.name(), PaymentStatus.REFUNDED,
                amount, "ZAR", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
        when(mockProvider.refundPayment(payment.getProviderReference(), refundRequest)).thenReturn(providerResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        paymentService.processRefund(1L, refundRequest);

        // Assert
        assertNoSensitiveDataInLogs(null);
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 11: Sensitive data never appears in logs
    void cancelLogsNeverContainSensitiveData(
            @ForAll("providerTypes") PaymentProviderType providerType) throws PaymentException {

        // Arrange: payment with PENDING status
        Payment payment = createNormalPayment(providerType);
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentResponse providerResponse = new PaymentResponse(
                1L, null, providerType.name(), PaymentStatus.CANCELLED,
                payment.getAmount(), payment.getCurrency(),
                LocalDateTime.now(), null, null
        );
        when(mockProvider.cancelPayment(payment.getProviderReference())).thenReturn(providerResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        paymentService.cancelPayment(1L);

        // Assert
        assertNoSensitiveDataInLogs(null);
    }

    @Property(tries = 100)
    // Feature: payment-provider-abstraction, Property 11: Sensitive data never appears in logs
    void webhookLogsNeverContainSensitiveData(
            @ForAll("webhookPayloads") String payload,
            @ForAll("webhookSignatures") String signature,
            @ForAll("providerTypes") PaymentProviderType providerType) throws PaymentException {

        // Arrange
        String providerRef = "pi_3abc123def456";
        Payment payment = createNormalPayment(providerType);
        payment.setProviderReference(providerRef);
        when(paymentRepository.findByProviderReference(providerRef)).thenReturn(Optional.of(payment));

        PaymentWebhookEvent event = new PaymentWebhookEvent(
                "payment.completed", providerRef, providerType,
                PaymentStatus.SUCCESS, payload, LocalDateTime.now()
        );
        when(mockProvider.constructWebhookEvent(payload, signature)).thenReturn(event);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        paymentService.handleWebhook(providerType, payload, signature);

        // Assert: log output never contains any sensitive patterns
        assertNoSensitiveDataInLogs(null);
    }

    // ---- Assertion helper ----

    /**
     * Verifies that no log message contains sensitive data patterns.
     * Checks for: API keys, webhook secrets, card numbers, and optionally a specific email.
     */
    private void assertNoSensitiveDataInLogs(String customerEmail) {
        for (ILoggingEvent event : logAppender.list) {
            String message = event.getFormattedMessage();

            // Check for API key patterns (sk_test_*, sk_live_*, pk_test_*, pk_live_*)
            assertThat(API_KEY_PATTERN.matcher(message).find())
                    .as("Log message should not contain API keys. Found in: %s", message)
                    .isFalse();

            // Check for webhook secret patterns (whsec_*)
            assertThat(WEBHOOK_SECRET_PATTERN.matcher(message).find())
                    .as("Log message should not contain webhook secrets. Found in: %s", message)
                    .isFalse();

            // Check for card number patterns (16-digit sequences)
            assertThat(CARD_NUMBER_PATTERN.matcher(message).find())
                    .as("Log message should not contain card numbers. Found in: %s", message)
                    .isFalse();

            // Check for full email addresses (customer PII)
            assertThat(EMAIL_PATTERN.matcher(message).find())
                    .as("Log message should not contain full email addresses. Found in: %s", message)
                    .isFalse();

            // If a specific customer email was provided, verify it's not in logs
            if (customerEmail != null) {
                assertThat(message).as("Log message should not contain customer email")
                        .doesNotContain(customerEmail);
            }

            // Verify known secret values never appear
            assertThat(message).as("Log message should not contain known API keys")
                    .doesNotContain(SECRET_API_KEY)
                    .doesNotContain(SECRET_LIVE_KEY)
                    .doesNotContain(PUBLIC_KEY)
                    .doesNotContain(WEBHOOK_SECRET)
                    .doesNotContain(CARD_NUMBER)
                    .doesNotContain(CARD_NUMBER_2);
        }

        // Clear logs for next try
        logAppender.list.clear();
    }

    // ---- Helpers ----

    /**
     * Creates a Payment entity with normal (non-sensitive) operational field values.
     * Provider references and transaction IDs use the standard format that providers
     * return (e.g., pi_xxx for Stripe) which are opaque identifiers, not secrets.
     */
    private Payment createNormalPayment(PaymentProviderType providerType) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setProvider(providerType);
        payment.setProviderReference("pi_3abc123def456");
        payment.setProviderTransactionId("txn_789xyz012abc");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(new BigDecimal("150.00"));
        payment.setCurrency("ZAR");
        return payment;
    }

    // ---- Generators ----

    /**
     * Generates PaymentRequests with sensitive customer data (email addresses)
     * that should never appear in logs.
     */
    @Provide
    Arbitrary<PaymentRequest> sensitivePaymentRequests() {
        return Combinators.combine(
                Arbitraries.longs().between(1L, 10000L),
                Arbitraries.longs().between(1L, 10000L),
                Arbitraries.bigDecimals().between(new BigDecimal("0.01"), new BigDecimal("9999.99")).ofScale(2),
                Arbitraries.of("USD", "ZAR", "EUR", "GBP"),
                Arbitraries.of(
                        "https://example.com/success",
                        "https://mysite.com/payment/done",
                        "https://app.raincheque.com/checkout/complete"
                ),
                Arbitraries.of(
                        "https://example.com/cancel",
                        "https://mysite.com/payment/cancel",
                        "https://app.raincheque.com/checkout/cancelled"
                ),
                customerEmails(),
                Arbitraries.of(PaymentProviderType.values()).injectNull(0.3)
        ).as(PaymentRequest::new);
    }

    @Provide
    Arbitrary<PaymentProviderType> providerTypes() {
        return Arbitraries.of(PaymentProviderType.values());
    }

    @Provide
    Arbitrary<BigDecimal> validRefundAmounts() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("9999.99"))
                .ofScale(2);
    }

    /**
     * Generates webhook payloads that may contain sensitive data within them.
     * The PaymentService should never log the raw payload contents.
     */
    @Provide
    Arbitrary<String> webhookPayloads() {
        return Arbitraries.of(
                "{\"type\":\"payment_intent.succeeded\",\"data\":{\"id\":\"pi_123\"}}",
                "{\"type\":\"checkout.session.completed\",\"data\":{\"customer_email\":\"user@test.com\"}}",
                "{\"type\":\"charge.succeeded\",\"data\":{\"payment_method\":\"pm_card_visa\"}}",
                "{\"type\":\"payment_intent.payment_failed\",\"data\":{\"id\":\"pi_456\"}}",
                "{\"type\":\"invoice.paid\",\"data\":{\"customer\":\"cus_abc123\"}}"
        );
    }

    /**
     * Generates webhook signatures of various formats.
     * These should never appear in logs.
     */
    @Provide
    Arbitrary<String> webhookSignatures() {
        return Arbitraries.of(
                "t=1234567890,v1=abc123def456ghi789,v0=xyz987",
                "t=9876543210,v1=def789abc012ghi345,v0=uvw654",
                "t=1111111111,v1=aaa111bbb222ccc333,v0=ddd444",
                "t=2222222222,v1=eee555fff666ggg777,v0=hhh888"
        );
    }

    /**
     * Generates realistic customer email addresses that represent PII
     * and should never appear in INFO-level logs.
     */
    private Arbitrary<String> customerEmails() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                Arbitraries.of("example.com", "test.co.za", "mail.org", "company.io", "gmail.com")
        ).as((local, domain) -> local.toLowerCase() + "@" + domain);
    }
}
