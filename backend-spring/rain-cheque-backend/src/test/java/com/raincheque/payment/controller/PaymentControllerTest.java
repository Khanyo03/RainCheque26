package com.raincheque.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raincheque.entity.User;
import com.raincheque.exception.GlobalExceptionHandler;
import com.raincheque.payment.PaymentException;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import com.raincheque.payment.dto.PaymentRequest;
import com.raincheque.payment.dto.PaymentResponse;
import com.raincheque.payment.dto.RefundRequest;
import com.raincheque.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PaymentController.
 * Uses standalone MockMvc setup with mocked PaymentService for fast, isolated testing.
 * Security is not involved here (tested separately in integration tests).
 *
 * Validates: Requirements 5.1, 5.2, 5.3, 6.4
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        Environment env = mock(Environment.class);
        org.mockito.Mockito.lenient().when(env.getActiveProfiles()).thenReturn(new String[]{"test"});

        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler(env))
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/payments/checkout")
    class CheckoutEndpoint {

        @Test
        @DisplayName("Returns HTTP 201 with PaymentResponse for valid checkout request")
        void checkout_validRequest_returns201() throws Exception {
            // Arrange
            PaymentRequest request = new PaymentRequest(
                    1L, 2L,
                    new BigDecimal("99.99"),
                    "USD",
                    "https://example.com/success",
                    "https://example.com/cancel",
                    "customer@example.com",
                    PaymentProviderType.STRIPE
            );

            PaymentResponse expectedResponse = new PaymentResponse(
                    1L,
                    "https://checkout.stripe.com/session/abc123",
                    "STRIPE",
                    PaymentStatus.PENDING,
                    new BigDecimal("99.99"),
                    "USD",
                    LocalDateTime.of(2024, 1, 15, 10, 0),
                    null,
                    null
            );

            when(paymentService.initiateCheckout(any(PaymentRequest.class)))
                    .thenReturn(expectedResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/payments/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentId").value(1))
                    .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/session/abc123"))
                    .andExpect(jsonPath("$.providerName").value("STRIPE"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.amount").value(99.99))
                    .andExpect(jsonPath("$.currency").value("USD"));
        }

        @Test
        @DisplayName("Returns HTTP 400 when eventId is null")
        void checkout_nullEventId_returns400() throws Exception {
            String requestBody = """
                    {
                        "ticketId": 2,
                        "amount": 99.99,
                        "currency": "USD",
                        "successUrl": "https://example.com/success",
                        "cancelUrl": "https://example.com/cancel",
                        "customerEmail": "customer@example.com",
                        "provider": "STRIPE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns HTTP 400 when amount is zero")
        void checkout_zeroAmount_returns400() throws Exception {
            String requestBody = """
                    {
                        "eventId": 1,
                        "ticketId": 2,
                        "amount": 0,
                        "currency": "USD",
                        "successUrl": "https://example.com/success",
                        "cancelUrl": "https://example.com/cancel",
                        "customerEmail": "customer@example.com",
                        "provider": "STRIPE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns HTTP 400 when currency is blank")
        void checkout_blankCurrency_returns400() throws Exception {
            String requestBody = """
                    {
                        "eventId": 1,
                        "ticketId": 2,
                        "amount": 50.00,
                        "currency": "",
                        "successUrl": "https://example.com/success",
                        "cancelUrl": "https://example.com/cancel",
                        "customerEmail": "customer@example.com",
                        "provider": "STRIPE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns HTTP 400 when customerEmail is invalid")
        void checkout_invalidEmail_returns400() throws Exception {
            String requestBody = """
                    {
                        "eventId": 1,
                        "ticketId": 2,
                        "amount": 50.00,
                        "currency": "USD",
                        "successUrl": "https://example.com/success",
                        "cancelUrl": "https://example.com/cancel",
                        "customerEmail": "not-an-email",
                        "provider": "STRIPE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns HTTP 400 when multiple required fields are missing")
        void checkout_multipleInvalidFields_returns400() throws Exception {
            String requestBody = """
                    {
                        "provider": "STRIPE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{id}")
    class GetPaymentStatusEndpoint {

        @Test
        @DisplayName("Returns HTTP 200 with PaymentResponse for valid payment ID")
        void getStatus_validId_returns200() throws Exception {
            PaymentResponse expectedResponse = new PaymentResponse(
                    5L,
                    null,
                    "STRIPE",
                    PaymentStatus.SUCCESS,
                    new BigDecimal("150.00"),
                    "ZAR",
                    LocalDateTime.of(2024, 1, 15, 10, 30),
                    LocalDateTime.of(2024, 1, 15, 10, 35),
                    null
            );

            when(paymentService.getPaymentStatus(eq(5L), any(User.class))).thenReturn(expectedResponse);

            mockMvc.perform(get("/api/v1/payments/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value(5))
                    .andExpect(jsonPath("$.providerName").value("STRIPE"))
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.amount").value(150.00))
                    .andExpect(jsonPath("$.currency").value("ZAR"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{id}/refund")
    class RefundEndpoint {

        @Test
        @DisplayName("Returns HTTP 200 with updated PaymentResponse for successful refund")
        void refund_validRequest_returns200() throws Exception {
            RefundRequest refundRequest = new RefundRequest(new BigDecimal("50.00"), "Customer requested");

            PaymentResponse expectedResponse = new PaymentResponse(
                    3L,
                    null,
                    "STRIPE",
                    PaymentStatus.REFUNDED,
                    new BigDecimal("50.00"),
                    "USD",
                    LocalDateTime.of(2024, 1, 10, 9, 0),
                    LocalDateTime.of(2024, 1, 10, 9, 5),
                    LocalDateTime.of(2024, 1, 10, 9, 10)
            );

            when(paymentService.processRefund(eq(3L), any(RefundRequest.class)))
                    .thenReturn(expectedResponse);

            mockMvc.perform(post("/api/v1/payments/3/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refundRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value(3))
                    .andExpect(jsonPath("$.status").value("REFUNDED"))
                    .andExpect(jsonPath("$.refundedAt").isNotEmpty());
        }

        @Test
        @DisplayName("Returns HTTP 400 when refund amount is null")
        void refund_nullAmount_returns400() throws Exception {
            String requestBody = """
                    {
                        "reason": "Some reason"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/3/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns HTTP 400 when refund amount is zero")
        void refund_zeroAmount_returns400() throws Exception {
            String requestBody = """
                    {
                        "amount": 0,
                        "reason": "Some reason"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/3/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{id}/cancel")
    class CancelEndpoint {

        @Test
        @DisplayName("Returns HTTP 200 with updated PaymentResponse for successful cancellation")
        void cancel_validRequest_returns200() throws Exception {
            PaymentResponse expectedResponse = new PaymentResponse(
                    7L,
                    null,
                    "STRIPE",
                    PaymentStatus.CANCELLED,
                    new BigDecimal("75.00"),
                    "EUR",
                    LocalDateTime.of(2024, 2, 1, 14, 0),
                    null,
                    null
            );

            when(paymentService.cancelPayment(eq(7L), any(User.class))).thenReturn(expectedResponse);

            mockMvc.perform(post("/api/v1/payments/7/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value(7))
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/webhook/{provider}")
    class WebhookEndpoint {

        @Test
        @DisplayName("Returns HTTP 200 for successfully processed webhook")
        void webhook_validPayload_returns200() throws Exception {
            String webhookPayload = "{\"type\":\"checkout.session.completed\",\"data\":{\"id\":\"cs_test_123\"}}";

            mockMvc.perform(post("/api/v1/payments/webhook/STRIPE")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookPayload)
                            .header("Stripe-Signature", "t=123,v1=abc"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Returns HTTP 400 when webhook signature verification fails")
        void webhook_invalidSignature_returns400() throws Exception {
            String webhookPayload = "{\"type\":\"checkout.session.completed\",\"data\":{\"id\":\"cs_test_123\"}}";

            org.mockito.Mockito.doThrow(new PaymentException("Webhook signature verification failed"))
                    .when(paymentService)
                    .handleWebhook(eq(PaymentProviderType.STRIPE), any(String.class), any(String.class));

            mockMvc.perform(post("/api/v1/payments/webhook/STRIPE")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookPayload)
                            .header("Stripe-Signature", "invalid-signature"))
                    .andExpect(status().isBadRequest());
        }
    }
}
