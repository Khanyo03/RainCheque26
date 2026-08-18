# Implementation Plan: Paystack Provider Integration

## Overview

Implement Paystack as a payment provider in the RainCheque ticketing system, following the existing `PaymentProvider` abstraction pattern established by `StripePaymentProvider`. The implementation uses Spring `RestClient` for API communication, HMAC SHA-512 for webhook verification, and makes Paystack the default provider.

## Tasks

- [x] 1. Enum and configuration setup
  - [x] 1.1 Add PAYSTACK to PaymentProviderType enum
    - Add `PAYSTACK` constant to the `PaymentProviderType` enum in `com.raincheque.payment.PaymentProviderType`
    - _Requirements: 1.1_

  - [x] 1.2 Update application.yml configuration
    - Add PAYSTACK provider block with `enabled: true`, `api-key: ${PAYSTACK_SECRET_KEY:}`, `webhook-secret: ${PAYSTACK_WEBHOOK_SECRET:}`
    - Set `default-provider: PAYSTACK`
    - Set STRIPE `enabled: false`
    - _Requirements: 2.1, 2.5, 11.1, 11.3_

  - [x] 1.3 Update .env.example with Paystack variables
    - Add `PAYSTACK_SECRET_KEY` and `PAYSTACK_WEBHOOK_SECRET` entries
    - _Requirements: 2.5_

- [x] 2. Paystack API client and response models
  - [x] 2.1 Create Paystack response record DTOs
    - Create `PaystackInitializeResponse`, `PaystackVerifyResponse`, `PaystackRefundResponse` records in `com.raincheque.payment.provider.paystack` package
    - Include nested data records as specified in design (authorization_url, access_code, reference, status, amount, currency, paid_at, etc.)
    - _Requirements: 3.1, 3.2, 4.1, 5.1_

  - [x] 2.2 Create PaystackWebhookPayload record
    - Create `PaystackWebhookPayload` record with `event` and nested `PaystackWebhookData` (reference, status, amount, currency, paid_at)
    - _Requirements: 7.3, 7.4, 7.5, 7.6_

  - [x] 2.3 Create PaystackApiClient helper class
    - Build `RestClient` with base URL `https://api.paystack.co`, Bearer token authorization, and JSON content type
    - Implement `initializeTransaction(Map<String, Object> body)` calling `POST /transaction/initialize`
    - Implement `verifyTransaction(String reference)` calling `GET /transaction/verify/{reference}`
    - Implement `createRefund(Map<String, Object> body)` calling `POST /refund`
    - Configure connect timeout (5s) and read timeout (30s)
    - _Requirements: 3.1, 4.1, 5.1_

- [x] 3. Implement PaystackPaymentProvider core operations
  - [x] 3.1 Implement createCheckoutSession
    - Generate unique reference per transaction (UUID-based)
    - Convert amount to kobo (multiply by 100)
    - Call Paystack Initialize Transaction API with amount, email, currency (ZAR), callback_url, and metadata (eventId, ticketId)
    - Return PaymentResponse with authorization_url, status PENDING, provider name PAYSTACK
    - Throw PaymentException with sanitized message on API error
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 3.2 Implement verifyPayment
    - Call Paystack Verify Transaction API with provider reference
    - Map Paystack status strings: "success"→SUCCESS, "failed"→FAILED, "abandoned"→CANCELLED, "pending"→PROCESSING
    - Convert amount from kobo to rands (divide by 100)
    - Parse paid_at timestamp
    - Throw PaymentException with sanitized message on API error
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [x] 3.3 Implement refundPayment
    - Call Paystack Create Refund API with transaction reference and amount in kobo
    - Return PaymentResponse with status REFUNDED
    - Throw PaymentException with sanitized message on API error
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 3.4 Implement cancelPayment
    - Verify transaction status via Paystack API first
    - If status is "success", throw PaymentException indicating completed payments cannot be cancelled
    - Otherwise return PaymentResponse with status CANCELLED
    - _Requirements: 6.1, 6.2_

  - [x] 3.5 Implement retrievePayment and getPaymentHistory
    - Call Paystack Verify Transaction API and map response to PaymentResponse
    - For getPaymentHistory, return result as single-item list
    - Convert amount from kobo to rands, parse timestamps
    - _Requirements: 8.1, 8.2, 9.1, 9.2_

  - [x] 3.6 Implement constructWebhookEvent with HMAC SHA-512 verification
    - Compute HMAC SHA-512 of raw payload using configured webhook secret
    - Compare computed hash to the provided signature (constant-time comparison)
    - Throw PaymentException on mismatch
    - Parse webhook payload JSON to extract event type and transaction reference
    - Map event types: "charge.success"→SUCCESS, "charge.failed"→FAILED, "refund.processed"→REFUNDED
    - Return PaymentWebhookEvent with provider type PAYSTACK
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

  - [x] 3.7 Implement sanitizeErrorMessage
    - Strip Paystack secret key patterns (sk_live_*, sk_test_*)
    - Strip internal Paystack transaction identifiers
    - Return generic fallback message when redaction markers are present
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 3.8 Implement getProviderType returning PAYSTACK
    - Return `PaymentProviderType.PAYSTACK`
    - Add `@Component` and `@ConditionalOnProperty(name = "payment.providers.PAYSTACK.enabled", havingValue = "true")` annotations
    - _Requirements: 1.2, 2.4_

- [x] 4. Checkpoint - Core provider implementation
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Webhook controller update and configuration validation
  - [x] 5.1 Update PaymentController webhook endpoint for header flexibility
    - Replace the fixed `@RequestHeader("Stripe-Signature")` parameter with `HttpServletRequest`
    - Add `resolveSignatureHeader(PaymentProviderType, HttpServletRequest)` method using switch expression
    - Return HTTP 400 when no recognized signature header is present
    - _Requirements: 12.1, 12.2, 12.3_

  - [x] 5.2 Add startup credential validation for PAYSTACK provider
    - Ensure PaymentProperties validation throws IllegalStateException when PAYSTACK is enabled but api-key or webhook-secret is blank
    - _Requirements: 2.2, 2.3_

- [ ] 6. Property-based tests
  - [ ]* 6.1 Write property test for amount conversion round-trip
    - **Property 1: Amount conversion round-trip**
    - Generate random BigDecimal values with up to 2 decimal places; verify converting to kobo and back produces the original amount
    - **Validates: Requirements 3.1, 5.1, 8.1**

  - [ ]* 6.2 Write property test for status mapping totality and determinism
    - **Property 2: Status mapping is total and deterministic**
    - For all Paystack status strings and webhook event types, verify mapping always returns non-null and is idempotent
    - **Validates: Requirements 4.2, 4.3, 4.4, 4.5, 7.3, 7.4, 7.5**

  - [ ]* 6.3 Write property test for webhook HMAC verification rejecting tampered payloads
    - **Property 3: Webhook HMAC verification rejects tampered payloads**
    - Generate random payloads and secrets, compute valid signature, then mutate a byte and verify rejection
    - **Validates: Requirements 7.1, 7.2**

  - [ ]* 6.4 Write property test for error sanitization never leaking secret keys
    - **Property 4: Error sanitization never leaks secret keys**
    - Generate strings containing Paystack key patterns; verify sanitized output never contains those patterns
    - **Validates: Requirements 10.1, 10.2, 10.3**

  - [ ]* 6.5 Write property test for checkout session response invariants
    - **Property 5: Checkout session always produces valid response with PENDING status**
    - For valid payment requests and successful API responses, verify response has PENDING status, non-null checkoutUrl, and provider "PAYSTACK"
    - **Validates: Requirements 3.2, 3.4**

  - [ ]* 6.6 Write property test for cancel rejecting completed transactions
    - **Property 6: Cancel rejects completed transactions**
    - For any transaction with verified status "success", verify cancelPayment throws PaymentException
    - **Validates: Requirements 6.2**

- [ ] 7. Unit tests
  - [ ]* 7.1 Write unit tests for PaystackPaymentProvider
    - Test createCheckoutSession with mocked API client (verify kobo conversion, metadata, reference generation)
    - Test verifyPayment for each status mapping
    - Test refundPayment amount conversion and response
    - Test cancelPayment rejects completed, accepts pending
    - Test constructWebhookEvent HMAC computation and event mapping
    - Test sanitizeErrorMessage strips secrets
    - _Requirements: 3.1–3.4, 4.1–4.6, 5.1–5.3, 6.1–6.2, 7.1–7.6, 10.1–10.3_

  - [ ]* 7.2 Write unit tests for PaymentController webhook header resolution
    - Test PAYSTACK provider reads x-paystack-signature header
    - Test STRIPE provider reads Stripe-Signature header
    - Test missing header returns 400
    - _Requirements: 12.1, 12.2, 12.3_

  - [ ]* 7.3 Write unit tests for configuration validation
    - Test startup fails with IllegalStateException when PAYSTACK enabled but api-key blank
    - Test startup fails with IllegalStateException when PAYSTACK enabled but webhook-secret blank
    - Test provider excluded from factory when disabled
    - _Requirements: 2.2, 2.3, 2.4_

- [x] 8. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The StripePaymentProvider serves as the reference pattern for implementation style
- Spring RestClient is used (no Paystack SDK) — already available in Spring Boot 3.5.x
- jqwik 1.9.1 is already in pom.xml for property-based testing

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["2.3"] },
    { "id": 3, "tasks": ["3.1", "3.2", "3.3", "3.4", "3.5", "3.6", "3.7", "3.8"] },
    { "id": 4, "tasks": ["5.1", "5.2"] },
    { "id": 5, "tasks": ["6.1", "6.2", "6.3", "6.4", "6.5", "6.6"] },
    { "id": 6, "tasks": ["7.1", "7.2", "7.3"] }
  ]
}
```
