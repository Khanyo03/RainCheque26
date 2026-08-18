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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService business logic.
 * Validates: Requirements 4.2, 4.3, 4.4, 4.5, 11.2, 11.3, 11.4, 11.5, 12.1
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentProviderFactory providerFactory;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentProperties paymentProperties;

    @Mock
    private PaymentProvider mockProvider;

    @Mock
    private TicketTierRepository ticketTierRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(providerFactory, paymentRepository, bookingRepository, paymentProperties, ticketTierRepository);

        // Set up SecurityContext with a mock user (lenient because not all tests use checkout)
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        User mockUser = new User();
        mockUser.setId(1L);
        lenient().when(authentication.getPrincipal()).thenReturn(mockUser);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- Helper methods ---

    private PaymentRequest createValidRequest(PaymentProviderType provider) {
        return new PaymentRequest(
                1L,
                100L,
                new BigDecimal("50.00"),
                "ZAR",
                "https://example.com/success",
                "https://example.com/cancel",
                "customer@example.com",
                provider
        );
    }

    private Payment createPaymentWithStatus(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setProvider(PaymentProviderType.STRIPE);
        payment.setProviderReference("cs_test_ref123");
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("50.00"));
        payment.setCurrency("ZAR");
        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }

    // --- initiateCheckout Tests ---

    @Nested
    @DisplayName("initiateCheckout")
    class InitiateCheckoutTests {

        @Test
        @DisplayName("creates entity with PENDING status and delegates to provider with default provider")
        void createsEntityWithPendingStatus_defaultProvider() throws PaymentException {
            // Arrange
            PaymentRequest request = createValidRequest(null); // null triggers default provider
            when(paymentProperties.getDefaultProvider()).thenReturn(PaymentProviderType.STRIPE);
            when(providerFactory.getProvider(PaymentProviderType.STRIPE)).thenReturn(mockProvider);

            Event event = new Event();
            event.setId(1L);
            TicketTier ticketTier = new TicketTier();
            ticketTier.setId(100L);
            ticketTier.setEvent(event);
            when(ticketTierRepository.findById(100L)).thenReturn(Optional.of(ticketTier));

            Booking savedBooking = new Booking();
            savedBooking.setId(200L);
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            PaymentResponse providerResponse = new PaymentResponse(
                    null,
                    "https://checkout.stripe.com/session/cs_test_abc",
                    "STRIPE",
                    PaymentStatus.PENDING,
                    new BigDecimal("50.00"),
                    "ZAR",
                    null, null, null
            );
            when(mockProvider.createCheckoutSession(request)).thenReturn(providerResponse);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // Act
            PaymentResponse response = paymentService.initiateCheckout(request);

            // Assert
            assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
            assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.com/session/cs_test_abc");
            assertThat(response.providerName()).isEqualTo("STRIPE");

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment savedPayment = captor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(savedPayment.getProvider()).isEqualTo(PaymentProviderType.STRIPE);
            assertThat(savedPayment.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(savedPayment.getCurrency()).isEqualTo("ZAR");
            assertThat(savedPayment.getBooking()).isEqualTo(savedBooking);
        }

        @Test
        @DisplayName("creates entity with PENDING status and delegates to explicit provider")
        void createsEntityWithPendingStatus_explicitProvider() throws PaymentException {
            // Arrange
            PaymentRequest request = createValidRequest(PaymentProviderType.YOCO);
            when(providerFactory.getProvider(PaymentProviderType.YOCO)).thenReturn(mockProvider);

            Event event = new Event();
            event.setId(1L);
            TicketTier ticketTier = new TicketTier();
            ticketTier.setId(100L);
            ticketTier.setEvent(event);
            when(ticketTierRepository.findById(100L)).thenReturn(Optional.of(ticketTier));

            Booking savedBooking = new Booking();
            savedBooking.setId(200L);
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            PaymentResponse providerResponse = new PaymentResponse(
                    null,
                    "https://checkout.yoco.com/session/yc_test_xyz",
                    "YOCO",
                    PaymentStatus.PENDING,
                    new BigDecimal("50.00"),
                    "ZAR",
                    null, null, null
            );
            when(mockProvider.createCheckoutSession(request)).thenReturn(providerResponse);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment saved = invocation.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            // Act
            PaymentResponse response = paymentService.initiateCheckout(request);

            // Assert
            assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
            assertThat(response.providerName()).isEqualTo("YOCO");

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getProvider()).isEqualTo(PaymentProviderType.YOCO);

            // Verify the provider was resolved with YOCO, not default
            verify(providerFactory).getProvider(PaymentProviderType.YOCO);
            verify(paymentProperties, never()).getDefaultProvider();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when ticket tier not found")
        void throwsWhenTicketTierNotFound() {
            // Arrange
            PaymentRequest request = createValidRequest(PaymentProviderType.STRIPE);
            when(providerFactory.getProvider(PaymentProviderType.STRIPE)).thenReturn(mockProvider);
            when(ticketTierRepository.findById(100L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.initiateCheckout(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ticket tier not found");
        }
    }

    // --- verifyPayment Tests ---

    @Nested
    @DisplayName("verifyPayment")
    class VerifyPaymentTests {

        @Test
        @DisplayName("updates entity status to match provider-reported status")
        void updatesEntityStatusToMatchProvider() throws PaymentException {
            // Arrange
            Payment payment = createPaymentWithStatus(PaymentStatus.PENDING);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(providerFactory.getProvider(PaymentProviderType.STRIPE)).thenReturn(mockProvider);

            PaymentResponse providerResponse = new PaymentResponse(
                    null, null, "STRIPE",
                    PaymentStatus.SUCCESS,
                    new BigDecimal("50.00"), "ZAR",
                    null, null, null
            );
            when(mockProvider.verifyPayment("cs_test_ref123")).thenReturn(providerResponse);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            PaymentResponse response = paymentService.verifyPayment(1L);

            // Assert
            assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment savedPayment = captor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(savedPayment.getProviderStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("sets paidAt when status changes to SUCCESS")
        void setsPaidAtWhenSuccess() throws PaymentException {
            // Arrange
            Payment payment = createPaymentWithStatus(PaymentStatus.PENDING);
            payment.setPaidAt(null); // explicitly null
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(providerFactory.getProvider(PaymentProviderType.STRIPE)).thenReturn(mockProvider);

            PaymentResponse providerResponse = new PaymentResponse(
                    null, null, "STRIPE",
                    PaymentStatus.SUCCESS,
                    new BigDecimal("50.00"), "ZAR",
                    null, null, null
            );
            when(mockProvider.verifyPayment("cs_test_ref123")).thenReturn(providerResponse);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            paymentService.verifyPayment(1L);

            // Assert
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("does not overwrite paidAt if already set")
        void doesNotOverwritePaidAt() throws PaymentException {
            // Arrange
            Payment payment = createPaymentWithStatus(PaymentStatus.PENDING);
            LocalDateTime existingPaidAt = LocalDateTime.of(2024, 1, 1, 12, 0);
            payment.setPaidAt(existingPaidAt);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(providerFactory.getProvider(PaymentProviderType.STRIPE)).thenReturn(mockProvider);

            PaymentResponse providerResponse = new PaymentResponse(
                    null, null, "STRIPE",
                    PaymentStatus.SUCCESS,
                    new BigDecimal("50.00"), "ZAR",
                    null, null, null
            );
            when(mockProvider.verifyPayment("cs_test_ref123")).thenReturn(providerResponse);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            paymentService.verifyPayment(1L);

            // Assert
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getPaidAt()).isEqualTo(existingPaidAt);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when payment not found")
        void throwsWhenPaymentNotFound() {
            when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.verifyPayment(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Payment not found");
        }
    }

    // --- processRefund Tests ---

    @Nested
    @DisplayName("processRefund")
    class ProcessRefundTests {

        @Test
        @DisplayName("transitions SUCCESS payment to REFUNDED and sets refundedAt")
        void transitionsSuccessToRefunded() throws PaymentException {
            // Arrange
            Payment payment = createPaymentWithStatus(PaymentStatus.SUCCESS);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(providerFactory.getProvider(PaymentProviderType.STRIPE)).thenReturn(mockProvider);

            RefundRequest refundRequest = new RefundRequest(new BigDecimal("50.00"), "Customer request");
            PaymentResponse providerResponse = new PaymentResponse(
                    null, null, "STRIPE",
                    PaymentStatus.REFUNDED,
                    new BigDecimal("50.00"), "ZAR",
                    null, null, LocalDateTime.now()
            );
            when(mockProvider.refundPayment("cs_test_ref123", refundRequest)).thenReturn(providerResponse);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            PaymentResponse response = paymentService.processRefund(1L, refundRequest);

            // Assert
            assertThat(response.status()).isEqualTo(PaymentStatus.REFUNDED);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment savedPayment = captor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(savedPayment.getRefundedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws InvalidPaymentException for PENDING payment")
        void throwsForPendingPayment() {
            Payment payment = createPaymentWithStatus(PaymentStatus.PENDING);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            RefundRequest refundRequest = new RefundRequest(new BigDecimal("50.00"), null);

            assertThatThrownBy(() -> paymentService.processRefund(1L, refundRequest))
                    .isInstanceOf(InvalidPaymentException.class)
                    .hasMessageContaining("Cannot refund");
        }

        @Test
        @DisplayName("throws InvalidPaymentException for PROCESSING payment")
        void throwsForProcessingPayment() {
            Payment payment = createPaymentWithStatus(PaymentStatus.PROCESSING);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            RefundRequest refundRequest = new RefundRequest(new BigDecimal("50.00"), null);

            assertThatThrownBy(() -> paymentService.processRefund(1L, refundRequest))
                    .isInstanceOf(InvalidPaymentException.class)
                    .hasMessageContaining("Cannot refund");
        }

        @Test
        @DisplayName("throws InvalidPaymentException for CANCELLED payment (terminal state)")
        void throwsForCancelledPayment() {
            Payment payment = createPaymentWithStatus(PaymentStatus.CANCELLED);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            RefundRequest refundRequest = new RefundRequest(new BigDecimal("50.00"), null);

            assertThatThrownBy(() -> paymentService.processRefund(1L, refundRequest))
                    .isInstanceOf(InvalidPaymentException.class)
                    .hasMessageContaining("terminal state");
        }

        @Test
        @DisplayName("throws InvalidPaymentException for already REFUNDED payment (terminal state)")
        void throwsForAlreadyRefundedPayment() {
            Payment payment = createPaymentWithStatus(PaymentStatus.REFUNDED);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            RefundRequest refundRequest = new RefundRequest(new BigDecimal("50.00"), null);

            assertThatThrownBy(() -> paymentService.processRefund(1L, refundRequest))
                    .isInstanceOf(InvalidPaymentException.class)
                    .hasMessageContaining("terminal state");
        }
    }

    // --- cancelPayment Tests ---

    @Nested
    @DisplayName("cancelPayment")
    class CancelPaymentTests {

        @Test
        @DisplayName("throws InvalidPaymentException for SUCCESS payment")
        void throwsForSuccessPayment() {
            Payment payment = createPaymentWithStatus(PaymentStatus.SUCCESS);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.cancelPayment(1L))
                    .isInstanceOf(InvalidPaymentException.class)
                    .hasMessageContaining("Cannot cancel");
        }

        @Test
        @DisplayName("throws InvalidPaymentException for REFUNDED payment (terminal state)")
        void throwsForRefundedPayment() {
            Payment payment = createPaymentWithStatus(PaymentStatus.REFUNDED);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.cancelPayment(1L))
                    .isInstanceOf(InvalidPaymentException.class)
                    .hasMessageContaining("terminal state");
        }

        @Test
        @DisplayName("allows cancellation for PENDING payment")
        void allowsCancellationForPending() throws PaymentException {
            Payment payment = createPaymentWithStatus(PaymentStatus.PENDING);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(providerFactory.getProvider(PaymentProviderType.STRIPE)).thenReturn(mockProvider);

            PaymentResponse providerResponse = new PaymentResponse(
                    null, null, "STRIPE",
                    PaymentStatus.CANCELLED,
                    new BigDecimal("50.00"), "ZAR",
                    null, null, null
            );
            when(mockProvider.cancelPayment("cs_test_ref123")).thenReturn(providerResponse);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            PaymentResponse response = paymentService.cancelPayment(1L);

            // Assert
            assertThat(response.status()).isEqualTo(PaymentStatus.CANCELLED);
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }
    }

    // --- handleWebhook Tests ---

    @Nested
    @DisplayName("handleWebhook")
    class HandleWebhookTests {

        @Test
        @DisplayName("delegates signature verification to provider and updates entity status")
        void delegatesSignatureVerificationAndUpdatesEntity() throws PaymentException {
            // Arrange
            String payload = "{\"type\":\"checkout.session.completed\"}";
            String signature = "whsec_test_signature";
            PaymentProviderType providerType = PaymentProviderType.STRIPE;

            when(providerFactory.getProvider(providerType)).thenReturn(mockProvider);

            PaymentWebhookEvent event = new PaymentWebhookEvent(
                    "checkout.session.completed",
                    "cs_test_ref123",
                    PaymentProviderType.STRIPE,
                    PaymentStatus.SUCCESS,
                    payload,
                    LocalDateTime.now()
            );
            when(mockProvider.constructWebhookEvent(payload, signature)).thenReturn(event);

            Payment payment = createPaymentWithStatus(PaymentStatus.PENDING);
            when(paymentRepository.findByProviderReference("cs_test_ref123")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            paymentService.handleWebhook(providerType, payload, signature);

            // Assert
            verify(mockProvider).constructWebhookEvent(payload, signature);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment savedPayment = captor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(savedPayment.getProviderStatus()).isEqualTo("SUCCESS");
            assertThat(savedPayment.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("sets paidAt on SUCCESS webhook event")
        void setsPaidAtOnSuccessWebhook() throws PaymentException {
            // Arrange
            String payload = "{\"type\":\"payment_intent.succeeded\"}";
            String signature = "sig_abc";

            when(providerFactory.getProvider(PaymentProviderType.STRIPE)).thenReturn(mockProvider);

            PaymentWebhookEvent event = new PaymentWebhookEvent(
                    "payment_intent.succeeded",
                    "cs_test_ref123",
                    PaymentProviderType.STRIPE,
                    PaymentStatus.SUCCESS,
                    payload,
                    LocalDateTime.now()
            );
            when(mockProvider.constructWebhookEvent(payload, signature)).thenReturn(event);

            Payment payment = createPaymentWithStatus(PaymentStatus.PENDING);
            payment.setPaidAt(null);
            when(paymentRepository.findByProviderReference("cs_test_ref123")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            paymentService.handleWebhook(PaymentProviderType.STRIPE, payload, signature);

            // Assert
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("sets refundedAt on REFUNDED webhook event")
        void setsRefundedAtOnRefundedWebhook() throws PaymentException {
            // Arrange
            String payload = "{\"type\":\"charge.refunded\"}";
            String signature = "sig_xyz";

            when(providerFactory.getProvider(PaymentProviderType.STRIPE)).thenReturn(mockProvider);

            PaymentWebhookEvent event = new PaymentWebhookEvent(
                    "charge.refunded",
                    "cs_test_ref123",
                    PaymentProviderType.STRIPE,
                    PaymentStatus.REFUNDED,
                    payload,
                    LocalDateTime.now()
            );
            when(mockProvider.constructWebhookEvent(payload, signature)).thenReturn(event);

            Payment payment = createPaymentWithStatus(PaymentStatus.SUCCESS);
            payment.setRefundedAt(null);
            when(paymentRepository.findByProviderReference("cs_test_ref123")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            paymentService.handleWebhook(PaymentProviderType.STRIPE, payload, signature);

            // Assert
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getRefundedAt()).isNotNull();
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when payment not found by provider reference")
        void throwsWhenPaymentNotFoundByReference() throws PaymentException {
            String payload = "{\"type\":\"checkout.session.completed\"}";
            String signature = "sig_test";

            when(providerFactory.getProvider(PaymentProviderType.STRIPE)).thenReturn(mockProvider);

            PaymentWebhookEvent event = new PaymentWebhookEvent(
                    "checkout.session.completed",
                    "unknown_ref",
                    PaymentProviderType.STRIPE,
                    PaymentStatus.SUCCESS,
                    payload,
                    LocalDateTime.now()
            );
            when(mockProvider.constructWebhookEvent(payload, signature)).thenReturn(event);
            when(paymentRepository.findByProviderReference("unknown_ref")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.handleWebhook(PaymentProviderType.STRIPE, payload, signature))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Payment not found for provider reference");
        }
    }

    // --- getPaymentStatus Tests ---

    @Nested
    @DisplayName("getPaymentStatus")
    class GetPaymentStatusTests {

        @Test
        @DisplayName("returns payment response for existing payment")
        void returnsResponseForExistingPayment() {
            Payment payment = createPaymentWithStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.of(2024, 6, 15, 10, 30));
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            PaymentResponse response = paymentService.getPaymentStatus(1L);

            assertThat(response.paymentId()).isEqualTo(1L);
            assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(response.providerName()).isEqualTo("STRIPE");
            assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(response.currency()).isEqualTo("ZAR");
            assertThat(response.paidAt()).isEqualTo(LocalDateTime.of(2024, 6, 15, 10, 30));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for non-existing payment")
        void throwsForNonExistingPayment() {
            when(paymentRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentStatus(404L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Payment not found");
        }
    }

    // --- Invalid State Transition Tests ---

    @Nested
    @DisplayName("Invalid state transitions")
    class InvalidStateTransitionTests {

        @Test
        @DisplayName("refunding FAILED payment throws InvalidPaymentException")
        void refundingFailedPaymentThrows() {
            Payment payment = createPaymentWithStatus(PaymentStatus.FAILED);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            RefundRequest refundRequest = new RefundRequest(new BigDecimal("50.00"), null);

            assertThatThrownBy(() -> paymentService.processRefund(1L, refundRequest))
                    .isInstanceOf(InvalidPaymentException.class);
        }

        @Test
        @DisplayName("cancelling FAILED payment throws InvalidPaymentException")
        void cancellingFailedPaymentThrows() {
            Payment payment = createPaymentWithStatus(PaymentStatus.FAILED);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.cancelPayment(1L))
                    .isInstanceOf(InvalidPaymentException.class);
        }

        @Test
        @DisplayName("cancelling EXPIRED payment throws InvalidPaymentException (terminal state)")
        void cancellingExpiredPaymentThrows() {
            Payment payment = createPaymentWithStatus(PaymentStatus.EXPIRED);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.cancelPayment(1L))
                    .isInstanceOf(InvalidPaymentException.class)
                    .hasMessageContaining("terminal state");
        }

        @Test
        @DisplayName("refunding EXPIRED payment throws InvalidPaymentException (terminal state)")
        void refundingExpiredPaymentThrows() {
            Payment payment = createPaymentWithStatus(PaymentStatus.EXPIRED);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            RefundRequest refundRequest = new RefundRequest(new BigDecimal("50.00"), null);

            assertThatThrownBy(() -> paymentService.processRefund(1L, refundRequest))
                    .isInstanceOf(InvalidPaymentException.class)
                    .hasMessageContaining("terminal state");
        }
    }
}
