# Implementation Plan: Payment Provider Abstraction

## Overview

This plan implements a gateway-agnostic payment architecture for RainCheque using the Strategy and Factory patterns. The initial provider is Stripe, with the architecture designed for future providers (PayFast, Yoco, Peach, Ozow). Implementation uses Spring Boot 3.5, Java 17, Spring Data JPA, and the existing project conventions (GlobalExceptionHandler, JWT auth via JwtService, SecurityConfig).

## Tasks

- [x] 1. Set up payment module foundation (enums, exceptions, configuration, DTOs)
  - [x] 1.1 Create payment enums and exception hierarchy
    - Create package `com.raincheque.payment`
    - Create `PaymentProviderType` enum with values: STRIPE, PAYFAST, YOCO, PEACH, OZOW
    - Create `PaymentStatus` enum with values: PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED, REFUNDED, EXPIRED
    - Create exception classes: `PaymentException` (checked base), `ProviderUnavailableException`, `InvalidPaymentException`, `RefundException` (all runtime)
    - _Requirements: 9.1, 11.1_

  - [x] 1.2 Create payment configuration class
    - Create `PaymentProperties` with `@ConfigurationProperties(prefix = "payment")`
    - Define per-provider `ProviderConfig` inner class with enabled flag, apiKey, webhookSecret, and properties map
    - Implement `isProviderEnabled(PaymentProviderType)` method
    - Add `@PostConstruct validateCredentials()` to validate enabled providers have required credentials
    - Add `defaultProvider` field with default `PaymentProviderType.STRIPE`
    - Add payment config section to `application.yml` and `application-test.yml`
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [x] 1.3 Create payment DTOs
    - Create `PaymentRequest` record with Jakarta validation annotations: `@NotNull eventId`, `@NotNull ticketId`, `@NotNull @DecimalMin("0.01") amount`, `@NotBlank @Size(min=3,max=3) currency`, `@NotBlank @URL successUrl`, `@NotBlank @URL cancelUrl`, `@NotBlank @Email customerEmail`, optional `PaymentProviderType provider`
    - Create `PaymentResponse` record with: paymentId, checkoutUrl, providerName, status, amount, currency, createdAt, paidAt, refundedAt
    - Create `PaymentWebhookEvent` record with: eventType, paymentId, provider, status, rawPayload, timestamp
    - Create `RefundRequest` record with: `@NotNull @DecimalMin("0.01") amount`, optional reason
    - _Requirements: 6.1, 6.2, 6.3_

- [x] 2. Implement Payment Entity and Repository
  - [x] 2.1 Create Payment entity and repository
    - Create `Payment` entity in `com.raincheque.payment.entity` with all fields: id, booking (ManyToOne), provider, providerReference (unique), providerStatus, providerTransactionId, status (default PENDING), amount, currency, createdAt, updatedAt, paidAt, refundedAt
    - Use `@EntityListeners(AuditingEntityListener.class)` for automatic timestamp management
    - Create `PaymentRepository` extending `JpaRepository<Payment, Long>` with query methods: `findByProviderReference`, `findByBookingId`
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 3. Implement PaymentProvider interface and factory
  - [x] 3.1 Create PaymentProvider interface
    - Create `PaymentProvider` interface in `com.raincheque.payment.provider` with methods: createCheckoutSession, verifyPayment, refundPayment, cancelPayment, retrievePayment, getPaymentHistory, constructWebhookEvent, getProviderType
    - All methods accept/return platform DTOs and throw `PaymentException`
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [x] 3.2 Create PaymentProviderFactory
    - Create `PaymentProviderFactory` as `@Component`
    - Constructor inject `List<PaymentProvider>` and `PaymentProperties`
    - Filter providers by enabled status and build `Map<PaymentProviderType, PaymentProvider>`
    - Implement `getProvider(PaymentProviderType)` that throws `ProviderUnavailableException` if provider not found or disabled
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 3.3 Write property tests for PaymentProviderFactory
    - **Property 1: Factory resolves registered providers correctly**
    - **Property 2: Factory rejects unavailable providers**
    - **Validates: Requirements 2.1, 2.2, 2.3**

- [ ] 4. Implement StripePaymentProvider
  - [x] 4.1 Add Stripe SDK dependency and implement StripePaymentProvider
    - Add `stripe-java` dependency to `pom.xml`
    - Create `StripePaymentProvider` in `com.raincheque.payment.provider.stripe` with `@ConditionalOnProperty(name = "payment.providers.STRIPE.enabled", havingValue = "true")`
    - Implement `createCheckoutSession`: create Stripe Checkout Session, return PaymentResponse with checkout URL and PENDING status
    - Implement `verifyPayment`: retrieve Stripe session/intent, map status to PaymentStatus
    - Implement `refundPayment`: create Stripe Refund, return updated response
    - Implement `cancelPayment`: cancel Stripe PaymentIntent, return CANCELLED status
    - Implement `retrievePayment` and `getPaymentHistory`
    - Implement `constructWebhookEvent`: verify signature using webhook secret, normalize to PaymentWebhookEvent
    - Map all Stripe exceptions to `PaymentException` with sanitized messages (no API keys or internal IDs)
    - Confine all Stripe SDK imports to the stripe sub-package
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

  - [x] 4.2 Write property test for Stripe status mapping
    - **Property 3: Stripe status mapping covers all provider statuses**
    - **Validates: Requirements 3.2**

  - [x] 4.3 Write property test for provider error sanitization
    - **Property 4: Provider error sanitization**
    - **Validates: Requirements 3.6, 9.2, 9.3**

- [ ] 5. Implement PaymentService business logic
  - [x] 5.1 Create PaymentService
    - Create `PaymentService` in `com.raincheque.payment.service` with `@Service` and `@Slf4j`
    - Constructor inject `PaymentProviderFactory` and `PaymentRepository`
    - Implement `initiateCheckout(PaymentRequest)`: resolve provider from factory, delegate checkout, persist Payment entity with PENDING status, return PaymentResponse
    - Implement `verifyPayment(Long paymentId)`: retrieve entity, delegate to provider, update entity status to match provider-reported status
    - Implement `processRefund(Long paymentId, RefundRequest)`: validate current status is SUCCESS, delegate refund, update entity to REFUNDED with refundedAt timestamp
    - Implement `cancelPayment(Long paymentId)`: validate status allows cancellation, delegate to provider, update entity
    - Implement `handleWebhook(PaymentProviderType, String payload, String signature)`: resolve provider, verify signature via constructWebhookEvent, update entity based on normalized event
    - Implement `getPaymentStatus(Long paymentId)`: retrieve entity and map to PaymentResponse
    - Add status transition validation: throw `InvalidPaymentException` for invalid transitions (e.g., refunding PENDING)
    - Log provider name, transaction ID, status, and operation duration at INFO level
    - Never log API keys, card numbers, webhook secrets, or customer payment details
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 10.1, 10.2, 10.4, 11.2, 11.3, 11.4, 11.5_

  - [x] 5.2 Write property test for checkout producing PENDING status
    - **Property 5: Checkout always produces PENDING status**
    - **Validates: Requirements 4.2, 11.2**

  - [x] 5.3 Write property test for payment status synchronization
    - **Property 6: Payment status synchronization**
    - **Validates: Requirements 4.3**

  - [x] 5.4 Write property test for refund entity update
    - **Property 7: Refund updates entity correctly**
    - **Validates: Requirements 4.4, 11.4**

  - [x] 5.5 Write property test for invalid status transitions
    - **Property 9: Invalid status transitions throw InvalidPaymentException**
    - **Validates: Requirements 11.5**

  - [x] 5.6 Write property test for webhook entity status update
    - **Property 10: Webhook updates entity status**
    - **Validates: Requirements 4.5, 11.3**

- [x] 6. Implement PaymentController
  - [x] 6.1 Create PaymentController REST endpoints
    - Create `PaymentController` in `com.raincheque.payment.controller` with `@RestController` and `@RequestMapping("/api/v1/payments")`
    - Implement `POST /checkout` → `initiateCheckout(PaymentRequest)` → 201 Created
    - Implement `GET /{id}` → `getPaymentStatus(id)` → 200 OK
    - Implement `POST /{id}/refund` → `processRefund(id, RefundRequest)` → 200 OK
    - Implement `POST /{id}/cancel` → `cancelPayment(id)` → 200 OK
    - Implement `POST /webhook/{provider}` → `handleWebhook(provider, body, headers)` → 200 OK or 400
    - Use `@Valid` for request body validation
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [x] 6.2 Write property test for PaymentRequest validation
    - **Property 8: PaymentRequest validation rejects all invalid fields**
    - **Validates: Requirements 6.1, 6.4**

- [ ] 7. Update SecurityConfig and add exception handlers
  - [x] 7.1 Update SecurityConfig for payment endpoints
    - Add `.requestMatchers("/api/v1/payments/webhook/**").permitAll()` for public webhook access
    - Add `.requestMatchers("/api/v1/payments/*/refund").hasAnyRole("ADMIN")` for refund authorization
    - Add `.requestMatchers("/api/v1/payments/**").authenticated()` for all other payment endpoints
    - Ensure rule ordering: webhook permitAll BEFORE authenticated catch-all
    - _Requirements: 5.4, 5.5, 5.6_

  - [x] 7.2 Add payment exception handlers to GlobalExceptionHandler
    - Add handler for `ProviderUnavailableException` → 503
    - Add handler for `InvalidPaymentException` → 400
    - Add handler for `RefundException` → 422
    - Add handler for `PaymentException` (base) → 502 with sanitized message
    - Ensure no API keys, stack traces, or internal IDs in error responses
    - _Requirements: 9.2, 9.3, 9.4_

- [x] 8. Checkpoint - Ensure all code compiles and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Write unit tests
  - [x] 9.1 Write unit tests for PaymentService
    - Test initiateCheckout: verifies entity created with PENDING, provider delegated correctly
    - Test verifyPayment: verifies entity status updated to match provider
    - Test processRefund: verifies SUCCESS → REFUNDED transition, refundedAt set
    - Test handleWebhook: verifies signature delegation, entity status updated
    - Test invalid state transitions throw InvalidPaymentException
    - Mock PaymentProvider and PaymentRepository
    - _Requirements: 4.2, 4.3, 4.4, 4.5, 11.2, 11.3, 11.4, 11.5, 12.1_

  - [x] 9.2 Write unit tests for PaymentController
    - Test HTTP 201 for successful checkout
    - Test HTTP 200 for payment status retrieval
    - Test HTTP 200 for successful refund
    - Test HTTP 400 for validation errors on invalid PaymentRequest
    - Use MockMvc with mocked PaymentService
    - _Requirements: 5.1, 5.2, 5.3, 6.4_

  - [x] 9.3 Write unit tests for StripePaymentProvider
    - Test createCheckoutSession maps to PaymentResponse with checkout URL
    - Test verifyPayment maps Stripe statuses to PaymentStatus
    - Test refundPayment returns REFUNDED status
    - Test constructWebhookEvent verifies signature and normalizes event
    - Test Stripe exceptions mapped to PaymentException with sanitized messages
    - Mock Stripe API client
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 3.6_

- [x] 10. Write integration tests
  - [x] 10.1 Write security integration tests
    - Test 401 for unauthenticated requests to payment endpoints
    - Test 403 for non-ADMIN users attempting refunds
    - Test permitAll for webhook endpoints (no JWT required)
    - Test authenticated users can access checkout and status endpoints
    - Use Spring Boot Test with MockMvc and @WithMockUser
    - _Requirements: 5.4, 5.5, 5.6_

  - [x] 10.2 Write payment flow integration tests
    - Test end-to-end checkout → verify → webhook flow with mocked Stripe
    - Test refund flow with state validation
    - Test providerReference unique constraint enforcement
    - Test configuration validation at startup
    - Use @SpringBootTest with test profile and H2
    - _Requirements: 4.2, 4.3, 4.4, 4.5, 7.4, 8.4_

  - [x] 10.3 Write property test for sensitive data not in logs
    - **Property 11: Sensitive data never appears in logs**
    - **Validates: Requirements 10.2**

- [x] 11. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document (11 properties total)
- Unit tests validate specific examples and edge cases
- jqwik is already available as a project dependency — no setup needed
- Stripe Java SDK must be added to pom.xml in task 4.1
- All Stripe SDK classes must be confined to `com.raincheque.payment.provider.stripe`
- The existing GlobalExceptionHandler pattern is reused for payment exceptions

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3"] },
    { "id": 2, "tasks": ["2.1", "3.1"] },
    { "id": 3, "tasks": ["3.2"] },
    { "id": 4, "tasks": ["3.3", "4.1"] },
    { "id": 5, "tasks": ["4.2", "4.3", "5.1"] },
    { "id": 6, "tasks": ["5.2", "5.3", "5.4", "5.5", "5.6", "6.1"] },
    { "id": 7, "tasks": ["6.2", "7.1", "7.2"] },
    { "id": 8, "tasks": ["9.1", "9.2", "9.3"] },
    { "id": 9, "tasks": ["10.1", "10.2", "10.3"] }
  ]
}
```
