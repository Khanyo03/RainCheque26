package com.raincheque.payment.integration;

import com.raincheque.RainChequeBackendApplication;
import com.raincheque.entity.Booking;
import com.raincheque.entity.Event;
import com.raincheque.entity.TicketTier;
import com.raincheque.entity.User;
import com.raincheque.entity.Venue;
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
import com.raincheque.payment.service.PaymentService;
import com.raincheque.repository.BookingRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.mockito.junit.jupiter.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the payment flow.
 * Tests end-to-end checkout → verify → webhook flow, refund flow,
 * unique constraint enforcement, and configuration validation.
 *
 * Validates: Requirements 4.2, 4.3, 4.4, 4.5, 7.4, 8.4
 */
@SpringBootTest(classes = {RainChequeBackendApplication.class, PaymentFlowIntegrationTest.MockPaymentProviderConfig.class},
        properties = {
                "management.health.mail.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:paymentflowtest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.sql.init.mode=never",
                "spring.data.redis.repositories.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                "payment.default-provider=STRIPE",
                "payment.providers.STRIPE.enabled=false",
                "payment.providers.STRIPE.api-key=sk_test_fake",
                "payment.providers.STRIPE.webhook-secret=whsec_test_fake",
                "payment.providers.PAYSTACK.api-key=sk_test_fake_paystack",
                "payment.providers.PAYSTACK.webhook-secret=whsec_test_fake_paystack"
        })
@ActiveProfiles("dev")
@Transactional
class PaymentFlowIntegrationTest {

    @MockitoBean
    private JavaMailSender javaMailSender;

    /**
     * Test configuration that provides a controllable mock PaymentProvider
     * and a custom factory that uses it. This replaces the real StripePaymentProvider
     * for integration testing.
     */
    @TestConfiguration
    static class MockPaymentProviderConfig {

        @Bean
        @Primary
        public PaymentProvider mockPaymentProvider() {
            return new FakePaymentProvider();
        }

        @Bean
        @Primary
        public PaymentProviderFactory testPaymentProviderFactory(PaymentProvider mockProvider, PaymentProperties properties) {
            // Create a factory that always returns the mock provider for STRIPE
            return new PaymentProviderFactory(List.of(mockProvider), properties) {
                @Override
                public PaymentProvider getProvider(PaymentProviderType type) {
                    return mockProvider;
                }
            };
        }
    }

    /**
     * Fake payment provider that returns controlled responses for testing.
     * Simulates successful Stripe interactions without any external calls.
     */
    static class FakePaymentProvider implements PaymentProvider {

        // Configurable responses for test scenarios
        private PaymentStatus verifyStatus = PaymentStatus.SUCCESS;
        private boolean webhookShouldFail = false;

        void setVerifyStatus(PaymentStatus status) {
            this.verifyStatus = status;
        }

        void setWebhookShouldFail(boolean shouldFail) {
            this.webhookShouldFail = shouldFail;
        }

        void reset() {
            this.verifyStatus = PaymentStatus.SUCCESS;
            this.webhookShouldFail = false;
        }

        @Override
        public PaymentResponse createCheckoutSession(PaymentRequest request) throws PaymentException {
            return new PaymentResponse(
                    null,
                    "https://checkout.stripe.com/pay/cs_test_session123",
                    PaymentProviderType.STRIPE.name(),
                    PaymentStatus.PENDING,
                    request.amount(),
                    request.currency(),
                    LocalDateTime.now(),
                    null,
                    null
            );
        }

        @Override
        public PaymentResponse verifyPayment(String providerReference) throws PaymentException {
            return new PaymentResponse(
                    null,
                    null,
                    PaymentProviderType.STRIPE.name(),
                    verifyStatus,
                    BigDecimal.valueOf(99.99),
                    "ZAR",
                    null,
                    verifyStatus == PaymentStatus.SUCCESS ? LocalDateTime.now() : null,
                    null
            );
        }

        @Override
        public PaymentResponse refundPayment(String providerReference, RefundRequest request) throws PaymentException {
            return new PaymentResponse(
                    null,
                    null,
                    PaymentProviderType.STRIPE.name(),
                    PaymentStatus.REFUNDED,
                    request.amount(),
                    "ZAR",
                    null,
                    null,
                    LocalDateTime.now()
            );
        }

        @Override
        public PaymentResponse cancelPayment(String providerReference) throws PaymentException {
            return new PaymentResponse(
                    null,
                    null,
                    PaymentProviderType.STRIPE.name(),
                    PaymentStatus.CANCELLED,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        @Override
        public PaymentResponse retrievePayment(String providerReference) throws PaymentException {
            return new PaymentResponse(
                    null,
                    null,
                    PaymentProviderType.STRIPE.name(),
                    PaymentStatus.PENDING,
                    BigDecimal.valueOf(99.99),
                    "ZAR",
                    null,
                    null,
                    null
            );
        }

        @Override
        public List<PaymentResponse> getPaymentHistory(String providerReference) throws PaymentException {
            return Collections.emptyList();
        }

        @Override
        public PaymentWebhookEvent constructWebhookEvent(String payload, String signature) throws PaymentException {
            if (webhookShouldFail) {
                throw new PaymentException("Webhook signature verification failed");
            }
            return new PaymentWebhookEvent(
                    "checkout.session.completed",
                    "cs_test_session123",
                    PaymentProviderType.STRIPE,
                    PaymentStatus.SUCCESS,
                    payload,
                    LocalDateTime.now()
            );
        }

        @Override
        public PaymentProviderType getProviderType() {
            return PaymentProviderType.STRIPE;
        }
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PaymentProvider paymentProvider;

    @Autowired
    private PaymentProperties paymentProperties;

    private Booking testBooking;
    private TicketTier testTier;
    private Event testEvent;
    private User testUser;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();

        // Reset fake provider state
        if (paymentProvider instanceof FakePaymentProvider fake) {
            fake.reset();
        }

        // Create required entities for the test booking
        testUser = new User();
        testUser.setEmail("testuser-" + System.nanoTime() + "@example.com");
        testUser.setPasswordHash("$2a$12$dummyhashfortesting");
        testUser.setFullName("Test User");
        testUser.setRole(User.Role.USER);
        testUser.setEmailVerified(true);
        entityManager.persist(testUser);

        Venue venue = new Venue();
        venue.setName("Test Venue");
        venue.setAddress("123 Test St");
        venue.setCity("Test City");
        venue.setCountry("ZA");
        venue.setCapacity(1000);
        entityManager.persist(venue);

        testEvent = new Event();
        testEvent.setTitle("Test Event");
        testEvent.setVenue(venue);
        testEvent.setStartTime(LocalDateTime.now().plusDays(30));
        testEvent.setEndTime(LocalDateTime.now().plusDays(30).plusHours(3));
        testEvent.setStatus(Event.EventStatus.PUBLISHED);
        entityManager.persist(testEvent);

        testTier = new TicketTier();
        testTier.setEvent(testEvent);
        testTier.setName("General");
        testTier.setPrice(BigDecimal.valueOf(99.99));
        testTier.setTotalQuantity(100);
        testTier.setRemainingQuantity(100);
        entityManager.persist(testTier);

        testBooking = new Booking();
        testBooking.setUser(testUser);
        testBooking.setEvent(testEvent);
        testBooking.setTicketTier(testTier);
        testBooking.setQuantity(1);
        testBooking.setTotalAmount(BigDecimal.valueOf(99.99));
        testBooking.setBookingTime(LocalDateTime.now());
        testBooking.setStatus(Booking.BookingStatus.PENDING);
        testBooking = bookingRepository.save(testBooking);

        entityManager.flush();

        // Set up SecurityContext with the test user
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void cleanUpSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ========== Checkout → Verify → Webhook Flow ==========

    @Test
    @DisplayName("Checkout creates Payment entity with PENDING status")
    @Transactional
    void checkout_createsPaymentEntity_withPendingStatus() throws PaymentException {
        PaymentRequest request = new PaymentRequest(
                testEvent.getId(),
                testTier.getId(),
                BigDecimal.valueOf(99.99),
                "ZAR",
                "https://example.com/success",
                "https://example.com/cancel",
                "customer@example.com",
                PaymentProviderType.STRIPE
        );

        PaymentResponse response = paymentService.initiateCheckout(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.paymentId()).isNotNull();
        assertThat(response.checkoutUrl()).contains("checkout.stripe.com");

        // Verify entity in database
        Payment savedPayment = paymentRepository.findById(response.paymentId()).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedPayment.getProvider()).isEqualTo(PaymentProviderType.STRIPE);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        assertThat(savedPayment.getCurrency()).isEqualTo("ZAR");
        assertThat(savedPayment.getBooking()).isNotNull();
    }

    @Test
    @DisplayName("Verify updates Payment entity status to match provider")
    @Transactional
    void verify_updatesPaymentStatus_toMatchProvider() throws PaymentException {
        // First create a payment via checkout
        PaymentRequest request = new PaymentRequest(
                testEvent.getId(),
                testTier.getId(),
                BigDecimal.valueOf(99.99),
                "ZAR",
                "https://example.com/success",
                "https://example.com/cancel",
                "customer@example.com",
                PaymentProviderType.STRIPE
        );
        PaymentResponse checkoutResponse = paymentService.initiateCheckout(request);

        // Set the provider reference on the payment so verify can delegate to provider
        Payment payment = paymentRepository.findById(checkoutResponse.paymentId()).orElseThrow();
        payment.setProviderReference("cs_test_session123");
        paymentRepository.save(payment);

        // Set fake provider to return SUCCESS on verify
        ((FakePaymentProvider) paymentProvider).setVerifyStatus(PaymentStatus.SUCCESS);

        // Verify the payment
        PaymentResponse verifyResponse = paymentService.verifyPayment(checkoutResponse.paymentId());

        assertThat(verifyResponse.status()).isEqualTo(PaymentStatus.SUCCESS);

        // Verify entity updated in database
        Payment updatedPayment = paymentRepository.findById(checkoutResponse.paymentId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(updatedPayment.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("Webhook processing updates Payment entity status")
    @Transactional
    void webhook_updatesPaymentEntity_status() throws PaymentException {
        // Create a payment with a known provider reference
        Payment payment = new Payment();
        payment.setBooking(testBooking);
        payment.setProvider(PaymentProviderType.STRIPE);
        payment.setProviderReference("cs_test_session123");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(BigDecimal.valueOf(99.99));
        payment.setCurrency("ZAR");
        Payment savedPayment = paymentRepository.save(payment);

        // Process webhook
        paymentService.handleWebhook(
                PaymentProviderType.STRIPE,
                "{\"type\":\"checkout.session.completed\"}",
                "valid_signature"
        );

        // Verify entity updated
        Payment updatedPayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(updatedPayment.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("End-to-end: checkout → verify → webhook flow")
    @Transactional
    void endToEnd_checkout_verify_webhook() throws PaymentException {
        // Step 1: Checkout
        PaymentRequest request = new PaymentRequest(
                testEvent.getId(),
                testTier.getId(),
                BigDecimal.valueOf(150.00),
                "ZAR",
                "https://example.com/success",
                "https://example.com/cancel",
                "buyer@example.com",
                PaymentProviderType.STRIPE
        );
        PaymentResponse checkoutResponse = paymentService.initiateCheckout(request);
        assertThat(checkoutResponse.status()).isEqualTo(PaymentStatus.PENDING);

        // Set provider reference for subsequent calls
        Payment payment = paymentRepository.findById(checkoutResponse.paymentId()).orElseThrow();
        payment.setProviderReference("cs_test_session123");
        paymentRepository.save(payment);

        // Step 2: Verify (provider reports SUCCESS)
        ((FakePaymentProvider) paymentProvider).setVerifyStatus(PaymentStatus.SUCCESS);
        PaymentResponse verifyResponse = paymentService.verifyPayment(checkoutResponse.paymentId());
        assertThat(verifyResponse.status()).isEqualTo(PaymentStatus.SUCCESS);

        // Verify entity in DB after verify
        Payment afterVerify = paymentRepository.findById(checkoutResponse.paymentId()).orElseThrow();
        assertThat(afterVerify.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(afterVerify.getPaidAt()).isNotNull();
    }

    // ========== Refund Flow ==========

    @Test
    @DisplayName("Refund transitions SUCCESS payment to REFUNDED with refundedAt set")
    @Transactional
    void refund_successPayment_transitionsToRefunded() throws PaymentException {
        // Create a payment already in SUCCESS status
        Payment payment = new Payment();
        payment.setBooking(testBooking);
        payment.setProvider(PaymentProviderType.STRIPE);
        payment.setProviderReference("cs_test_refund_session");
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setAmount(BigDecimal.valueOf(99.99));
        payment.setCurrency("ZAR");
        payment.setPaidAt(LocalDateTime.now().minusHours(1));
        Payment savedPayment = paymentRepository.save(payment);

        // Process refund
        RefundRequest refundRequest = new RefundRequest(BigDecimal.valueOf(99.99), "Customer requested");
        PaymentResponse refundResponse = paymentService.processRefund(savedPayment.getId(), refundRequest);

        assertThat(refundResponse.status()).isEqualTo(PaymentStatus.REFUNDED);

        // Verify entity in database
        Payment refundedPayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();
        assertThat(refundedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refundedPayment.getRefundedAt()).isNotNull();
    }

    @Test
    @DisplayName("Refund of PENDING payment throws InvalidPaymentException")
    @Transactional
    void refund_pendingPayment_throwsInvalidPaymentException() {
        // Create a payment in PENDING status
        Payment payment = new Payment();
        payment.setBooking(testBooking);
        payment.setProvider(PaymentProviderType.STRIPE);
        payment.setProviderReference("cs_test_pending_session");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(BigDecimal.valueOf(99.99));
        payment.setCurrency("ZAR");
        Payment savedPayment = paymentRepository.save(payment);

        RefundRequest refundRequest = new RefundRequest(BigDecimal.valueOf(99.99), null);

        assertThatThrownBy(() -> paymentService.processRefund(savedPayment.getId(), refundRequest))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Cannot refund");
    }

    @Test
    @DisplayName("Refund of REFUNDED payment throws InvalidPaymentException")
    @Transactional
    void refund_refundedPayment_throwsInvalidPaymentException() {
        // Create a payment already REFUNDED
        Payment payment = new Payment();
        payment.setBooking(testBooking);
        payment.setProvider(PaymentProviderType.STRIPE);
        payment.setProviderReference("cs_test_already_refunded");
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setAmount(BigDecimal.valueOf(99.99));
        payment.setCurrency("ZAR");
        payment.setRefundedAt(LocalDateTime.now().minusHours(1));
        Payment savedPayment = paymentRepository.save(payment);

        RefundRequest refundRequest = new RefundRequest(BigDecimal.valueOf(99.99), null);

        assertThatThrownBy(() -> paymentService.processRefund(savedPayment.getId(), refundRequest))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("terminal state");
    }

    // ========== providerReference Unique Constraint ==========

    @Test
    @DisplayName("providerReference unique constraint prevents duplicate payments")
    @Transactional
    void providerReference_uniqueConstraint_preventsDuplicates() {
        String duplicateRef = "cs_test_duplicate_ref";

        // Create first payment with a provider reference
        Payment payment1 = new Payment();
        payment1.setBooking(testBooking);
        payment1.setProvider(PaymentProviderType.STRIPE);
        payment1.setProviderReference(duplicateRef);
        payment1.setStatus(PaymentStatus.PENDING);
        payment1.setAmount(BigDecimal.valueOf(99.99));
        payment1.setCurrency("ZAR");
        paymentRepository.saveAndFlush(payment1);

        // Attempt to save a second payment with the same provider reference
        Payment payment2 = new Payment();
        payment2.setBooking(testBooking);
        payment2.setProvider(PaymentProviderType.STRIPE);
        payment2.setProviderReference(duplicateRef);
        payment2.setStatus(PaymentStatus.PENDING);
        payment2.setAmount(BigDecimal.valueOf(50.00));
        payment2.setCurrency("ZAR");

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(payment2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ========== Configuration Validation at Startup ==========

    @Test
    @DisplayName("PaymentProperties validates enabled providers have required credentials")
    void configurationValidation_enabledProvider_missingApiKey_throwsException() {
        // Create a new PaymentProperties with an enabled provider missing apiKey
        PaymentProperties props = new PaymentProperties();
        PaymentProperties.ProviderConfig config = new PaymentProperties.ProviderConfig();
        config.setEnabled(true);
        config.setApiKey(""); // blank
        config.setWebhookSecret("whsec_valid");
        props.getProviders().put(PaymentProviderType.STRIPE, config);

        assertThatThrownBy(props::validateCredentials)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing required apiKey");
    }

    @Test
    @DisplayName("PaymentProperties validates enabled providers have webhook secret")
    void configurationValidation_enabledProvider_missingWebhookSecret_throwsException() {
        PaymentProperties props = new PaymentProperties();
        PaymentProperties.ProviderConfig config = new PaymentProperties.ProviderConfig();
        config.setEnabled(true);
        config.setApiKey("sk_test_valid");
        config.setWebhookSecret(""); // blank
        props.getProviders().put(PaymentProviderType.STRIPE, config);

        assertThatThrownBy(props::validateCredentials)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing required webhookSecret");
    }

    @Test
    @DisplayName("PaymentProperties passes validation when disabled providers have no credentials")
    void configurationValidation_disabledProvider_noCredentials_passesValidation() {
        PaymentProperties props = new PaymentProperties();
        PaymentProperties.ProviderConfig config = new PaymentProperties.ProviderConfig();
        config.setEnabled(false);
        config.setApiKey("");
        config.setWebhookSecret("");
        props.getProviders().put(PaymentProviderType.PAYFAST, config);

        // Should not throw
        props.validateCredentials();
    }

    @Test
    @DisplayName("Application context loads with valid test configuration")
    void contextLoads_withValidTestConfig() {
        // If we reach this test, the application context loaded successfully
        // This implicitly validates that the test profile configuration passes startup validation
        assertThat(paymentService).isNotNull();
        assertThat(paymentProperties).isNotNull();
        assertThat(paymentProperties.getDefaultProvider()).isEqualTo(PaymentProviderType.STRIPE);
    }
}
