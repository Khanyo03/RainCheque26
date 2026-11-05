package com.raincheque.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raincheque.RainChequeBackendApplication;
import com.raincheque.payment.PaymentProviderType;
import com.raincheque.payment.PaymentStatus;
import com.raincheque.payment.dto.PaymentRequest;
import com.raincheque.payment.dto.PaymentResponse;
import com.raincheque.payment.dto.RefundRequest;
import com.raincheque.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security integration tests for payment endpoints.
 * Validates that the SecurityConfig correctly enforces:
 * - 401 for unauthenticated requests to payment endpoints
 * - 403 for non-ADMIN users attempting refunds
 * - permitAll for webhook endpoints (no JWT required)
 * - Authenticated users can access checkout and status endpoints
 *
 * Validates: Requirements 5.4, 5.5, 5.6
 */
@SpringBootTest(classes = RainChequeBackendApplication.class, properties = {
    "management.health.mail.enabled=false",
    "payment.providers.STRIPE.enabled=true",
    "payment.providers.STRIPE.api-key=sk_test_fake_key",
    "payment.providers.STRIPE.webhook-secret=whsec_test_fake_secret",
    "payment.providers.PAYSTACK.api-key=sk_test_fake_paystack",
    "payment.providers.PAYSTACK.webhook-secret=whsec_test_fake_paystack"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PaymentSecurityIntegrationTest {

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // ===================== Requirement 5.5 =====================
    // Unauthenticated users receive 401/403 for payment operations (excluding webhooks)

    @Nested
    @DisplayName("Unauthenticated access (no JWT)")
    class UnauthenticatedAccess {

        @Test
        @DisplayName("POST /api/v1/payments/checkout returns 401/403 when unauthenticated")
        void checkout_unauthenticated_returnsForbidden() throws Exception {
            String requestBody = """
                    {
                        "eventId": 1,
                        "ticketId": 2,
                        "amount": 50.00,
                        "currency": "USD",
                        "successUrl": "https://example.com/success",
                        "cancelUrl": "https://example.com/cancel",
                        "customerEmail": "test@example.com",
                        "provider": "STRIPE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/v1/payments/1 returns 401/403 when unauthenticated")
        void getPaymentStatus_unauthenticated_returnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/payments/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/v1/payments/1/refund returns 401/403 when unauthenticated")
        void refund_unauthenticated_returnsForbidden() throws Exception {
            String requestBody = """
                    {
                        "amount": 25.00,
                        "reason": "Customer request"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/1/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/v1/payments/1/cancel returns 401/403 when unauthenticated")
        void cancel_unauthenticated_returnsForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/payments/1/cancel"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== Requirement 5.6 =====================
    // Non-ADMIN users receive 403 when attempting refunds

    @Nested
    @DisplayName("Insufficient role for refund (non-ADMIN)")
    class InsufficientRoleForRefund {

        @Test
        @DisplayName("POST /api/v1/payments/1/refund returns 403 for USER role")
        @WithMockUser(roles = "USER")
        void refund_userRole_returnsForbidden() throws Exception {
            String requestBody = """
                    {
                        "amount": 25.00,
                        "reason": "Customer request"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/1/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/v1/payments/1/refund returns 403 for VENUE_ADMIN role")
        @WithMockUser(roles = "VENUE_ADMIN")
        void refund_venueAdminRole_returnsForbidden() throws Exception {
            String requestBody = """
                    {
                        "amount": 25.00,
                        "reason": "Customer request"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/1/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== Requirement 5.4 =====================
    // Webhook endpoints are publicly accessible (permitAll)

    @Nested
    @DisplayName("Webhook endpoints (permitAll)")
    class WebhookEndpoints {

        @Test
        @DisplayName("POST /api/v1/payments/webhook/STRIPE is accessible without authentication")
        void webhook_unauthenticated_returns200() throws Exception {
            String webhookPayload = "{\"type\":\"checkout.session.completed\",\"data\":{\"id\":\"cs_test_123\"}}";

            mockMvc.perform(post("/api/v1/payments/webhook/STRIPE")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookPayload)
                            .header("Stripe-Signature", "t=123,v1=abc"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/v1/payments/webhook/PAYFAST is accessible without authentication")
        void webhook_payfast_unauthenticated_returns200() throws Exception {
            String webhookPayload = "{\"payment_id\":\"123\",\"status\":\"COMPLETE\"}";

            mockMvc.perform(post("/api/v1/payments/webhook/PAYFAST")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookPayload)
                            .header("Stripe-Signature", "sig123"))
                    .andExpect(status().isOk());
        }
    }

    // ===================== Authenticated access =====================
    // Authenticated users can access checkout, status, and cancel endpoints

    @Nested
    @DisplayName("Authenticated access (valid user)")
    class AuthenticatedAccess {

        @Test
        @DisplayName("POST /api/v1/payments/checkout returns 201 for authenticated user")
        @WithMockUser(roles = "USER")
        void checkout_authenticated_returns201() throws Exception {
            PaymentResponse mockResponse = new PaymentResponse(
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
                    .thenReturn(mockResponse);

            String requestBody = """
                    {
                        "eventId": 1,
                        "ticketId": 2,
                        "amount": 99.99,
                        "currency": "USD",
                        "successUrl": "https://example.com/success",
                        "cancelUrl": "https://example.com/cancel",
                        "customerEmail": "test@example.com",
                        "provider": "STRIPE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("GET /api/v1/payments/1 returns 200 for authenticated user")
        @WithMockUser(roles = "USER")
        void getPaymentStatus_authenticated_returns200() throws Exception {
            PaymentResponse mockResponse = new PaymentResponse(
                    1L,
                    null,
                    "STRIPE",
                    PaymentStatus.SUCCESS,
                    new BigDecimal("50.00"),
                    "USD",
                    LocalDateTime.of(2024, 1, 15, 10, 0),
                    LocalDateTime.of(2024, 1, 15, 10, 5),
                    null
            );

            when(paymentService.getPaymentStatus(1L)).thenReturn(mockResponse);

            mockMvc.perform(get("/api/v1/payments/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/v1/payments/1/cancel returns 200 for authenticated user")
        @WithMockUser(roles = "USER")
        void cancel_authenticated_returns200() throws Exception {
            PaymentResponse mockResponse = new PaymentResponse(
                    1L,
                    null,
                    "STRIPE",
                    PaymentStatus.CANCELLED,
                    new BigDecimal("50.00"),
                    "USD",
                    LocalDateTime.of(2024, 1, 15, 10, 0),
                    null,
                    null
            );

            when(paymentService.cancelPayment(1L)).thenReturn(mockResponse);

            mockMvc.perform(post("/api/v1/payments/1/cancel"))
                    .andExpect(status().isOk());
        }
    }

    // ===================== ADMIN role for refund =====================
    // ADMIN users can successfully access the refund endpoint

    @Nested
    @DisplayName("ADMIN role for refund")
    class AdminRefundAccess {

        @Test
        @DisplayName("POST /api/v1/payments/1/refund returns 200 for ADMIN role")
        @WithMockUser(roles = "ADMIN")
        void refund_adminRole_returns200() throws Exception {
            PaymentResponse mockResponse = new PaymentResponse(
                    1L,
                    null,
                    "STRIPE",
                    PaymentStatus.REFUNDED,
                    new BigDecimal("25.00"),
                    "USD",
                    LocalDateTime.of(2024, 1, 10, 9, 0),
                    LocalDateTime.of(2024, 1, 10, 9, 5),
                    LocalDateTime.of(2024, 1, 10, 9, 10)
            );

            when(paymentService.processRefund(eq(1L), any(RefundRequest.class)))
                    .thenReturn(mockResponse);

            String requestBody = """
                    {
                        "amount": 25.00,
                        "reason": "Customer requested"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/1/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }
    }
}
