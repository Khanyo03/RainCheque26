# Requirements Document

## Introduction

The Payment Provider Abstraction module introduces a provider-agnostic payment architecture to RainCheque, enabling the platform to support multiple payment gateways (Stripe, PayFast, Yoco, Peach Payments, Ozow) without modifying business logic. The architecture uses the Strategy and Factory patterns to isolate provider-specific SDKs behind a unified `PaymentProvider` interface. The initial implementation targets Stripe, with future providers pluggable through configuration and a single new class per provider.

## Glossary

- **Payment_Service**: The core business logic service that orchestrates payment operations through the PaymentProvider interface without knowledge of any specific payment gateway SDK
- **Payment_Provider**: The interface contract defining all payment operations (checkout, verification, refund, cancellation, retrieval, webhook handling) that each gateway implementation must fulfil
- **Payment_Provider_Factory**: The factory component responsible for resolving and returning the correct PaymentProvider implementation based on the configured provider type
- **Stripe_Provider**: The concrete PaymentProvider implementation that encapsulates all Stripe SDK interactions
- **Payment_Controller**: The REST controller exposing payment endpoints to frontend clients
- **Payment_Entity**: The JPA entity persisting payment records with provider-specific reference fields
- **Payment_Validator**: The validation layer enforcing data integrity on payment requests
- **Webhook_Handler**: The component responsible for receiving, verifying, and normalizing provider-specific webhook events into platform-standard PaymentWebhookEvent objects
- **Authorized_User**: A user authenticated via JWT who is permitted to initiate payment operations
- **Admin_User**: A user with role ADMIN permitted to issue refunds and view payment history

## Requirements

### Requirement 1: PaymentProvider Interface Contract

**User Story:** As a developer, I want a unified interface for all payment operations, so that business logic remains decoupled from any specific payment gateway.

#### Acceptance Criteria

1. THE Payment_Provider SHALL define operations: createCheckoutSession, verifyPayment, refundPayment, cancelPayment, retrievePayment, getPaymentHistory, constructWebhookEvent, and getProviderName
2. THE Payment_Provider SHALL use platform-defined DTOs (PaymentRequest, PaymentResponse, PaymentWebhookEvent) as input and output types rather than provider-specific objects
3. THE Payment_Provider SHALL declare checked exceptions (PaymentException) for all operations that interact with external services
4. WHEN a new payment provider is added to the system, THE Payment_Provider interface SHALL require no modifications to existing implementations or consuming services

### Requirement 2: Payment Provider Factory

**User Story:** As a developer, I want a factory that resolves the correct provider at runtime, so that adding a new provider requires no changes to the PaymentService.

#### Acceptance Criteria

1. WHEN the Payment_Provider_Factory receives a PaymentProviderType, THE Payment_Provider_Factory SHALL return the corresponding PaymentProvider implementation
2. IF the requested PaymentProviderType has no registered implementation, THEN THE Payment_Provider_Factory SHALL throw a ProviderUnavailableException with a descriptive message
3. IF the requested provider is disabled in configuration, THEN THE Payment_Provider_Factory SHALL throw a ProviderUnavailableException indicating the provider is not enabled
4. WHEN a new PaymentProvider implementation is registered as a Spring bean, THE Payment_Provider_Factory SHALL resolve the new provider without code changes to the factory class

### Requirement 3: Stripe Provider Implementation

**User Story:** As a venue admin, I want to process payments through Stripe, so that customers can purchase tickets using credit cards and other Stripe-supported methods.

#### Acceptance Criteria

1. WHEN the Stripe_Provider receives a valid PaymentRequest, THE Stripe_Provider SHALL create a Stripe Checkout Session and return a PaymentResponse containing the checkout URL, payment ID, and status PENDING
2. WHEN the Stripe_Provider receives a payment ID for verification, THE Stripe_Provider SHALL query the Stripe API and return the current payment status mapped to the platform PaymentStatus enum
3. WHEN the Stripe_Provider receives a refund request for a completed payment, THE Stripe_Provider SHALL initiate a Stripe refund and return the updated PaymentResponse with status REFUNDED
4. WHEN the Stripe_Provider receives a cancellation request for a pending payment, THE Stripe_Provider SHALL cancel the Stripe Payment Intent and return status CANCELLED
5. WHEN the Stripe_Provider receives a raw webhook payload and signature header, THE Stripe_Provider SHALL verify the webhook signature using the configured webhook secret and return a normalized PaymentWebhookEvent
6. IF the Stripe API returns an error, THEN THE Stripe_Provider SHALL map the Stripe exception to a platform PaymentException with a sanitized message that excludes API keys or internal Stripe details
7. THE Stripe_Provider SHALL isolate all Stripe SDK imports within the stripe package and SHALL NOT expose Stripe-specific types in method signatures

### Requirement 4: Payment Service Business Logic

**User Story:** As a developer, I want the payment service to contain all business logic independent of the provider, so that switching providers does not require business logic changes.

#### Acceptance Criteria

1. THE Payment_Service SHALL NOT import or reference any provider SDK classes (Stripe, PayFast, Yoco, Peach, Ozow)
2. WHEN the Payment_Service initiates a checkout, THE Payment_Service SHALL obtain the PaymentProvider from the Payment_Provider_Factory, delegate the checkout to the provider, persist a Payment_Entity with status PENDING, and return the PaymentResponse
3. WHEN the Payment_Service processes a verified payment, THE Payment_Service SHALL update the Payment_Entity status to match the provider-reported status
4. WHEN the Payment_Service processes a refund, THE Payment_Service SHALL delegate to the provider, update the Payment_Entity status to REFUNDED, and record the refund timestamp
5. WHEN the Payment_Service receives a webhook event, THE Payment_Service SHALL delegate signature verification to the provider, then update the corresponding Payment_Entity based on the normalized event type
6. THE Payment_Service SHALL log the provider name, transaction ID, payment status, and operation duration for every payment operation

### Requirement 5: Payment Controller Endpoints

**User Story:** As a frontend developer, I want well-defined REST endpoints for all payment operations, so that the UI can initiate and track payments.

#### Acceptance Criteria

1. WHEN an Authorized_User submits a valid checkout request, THE Payment_Controller SHALL return HTTP 201 with a PaymentResponse containing the checkout URL
2. WHEN an Authorized_User requests payment status by payment ID, THE Payment_Controller SHALL return HTTP 200 with the current PaymentResponse
3. WHEN an Admin_User submits a refund request for a completed payment, THE Payment_Controller SHALL return HTTP 200 with the updated PaymentResponse showing status REFUNDED
4. WHEN the Payment_Controller receives a webhook POST from a provider, THE Payment_Controller SHALL return HTTP 200 after successful processing or HTTP 400 if signature verification fails
5. WHEN an unauthenticated user attempts a payment operation (excluding webhooks), THE Payment_Controller SHALL return HTTP 401
6. WHEN a user without sufficient role attempts a refund, THE Payment_Controller SHALL return HTTP 403

### Requirement 6: Payment Data Transfer Objects

**User Story:** As a developer, I want standardized DTOs for payment data, so that all layers communicate with consistent, validated structures.

#### Acceptance Criteria

1. THE Payment_Validator SHALL enforce that PaymentRequest contains non-null eventId, ticketId, amount greater than zero, non-blank currency, non-blank successUrl, non-blank cancelUrl, and valid customerEmail
2. THE PaymentResponse SHALL contain paymentId, checkoutUrl, provider name, status mapped to PaymentStatus enum, amount, and currency
3. THE PaymentWebhookEvent SHALL contain eventType, paymentId, provider, status, rawPayload (for audit), and timestamp
4. WHEN the Payment_Validator receives a PaymentRequest with invalid fields, THE Payment_Validator SHALL reject with HTTP 400 and field-level error messages for all invalid fields

### Requirement 7: Payment Entity Persistence

**User Story:** As a developer, I want payment records persisted with provider-specific references, so that transactions are auditable and reconcilable.

#### Acceptance Criteria

1. THE Payment_Entity SHALL store provider (PaymentProviderType), providerReference (external session/intent ID), providerStatus (raw provider status string), providerTransactionId, amount, currency, status (PaymentStatus), and timestamps (createdAt, updatedAt, paidAt, refundedAt)
2. THE Payment_Entity SHALL maintain a relationship to the Booking entity via bookingId
3. WHEN a payment status changes, THE Payment_Entity SHALL update the updatedAt timestamp automatically
4. THE Payment_Entity SHALL store the providerReference as a unique constraint to prevent duplicate processing

### Requirement 8: Payment Configuration

**User Story:** As a DevOps engineer, I want payment provider settings externalized in configuration, so that credentials and feature flags are managed through environment variables without code changes.

#### Acceptance Criteria

1. THE PaymentProperties SHALL define per-provider configuration blocks containing enabled flag and provider-specific credentials (stripe: apiKey, webhookSecret; payfast: merchantId, merchantKey; yoco: secretKey)
2. THE PaymentProperties SHALL load all sensitive values from environment variables and SHALL NOT contain hardcoded secrets in source code or configuration files
3. WHEN a provider is disabled in configuration, THE Payment_Provider_Factory SHALL exclude that provider from the available provider set
4. THE PaymentProperties SHALL validate at startup that all enabled providers have their required credentials configured

### Requirement 9: Payment Error Handling

**User Story:** As a developer, I want a consistent error hierarchy for payment failures, so that controllers return meaningful responses without exposing provider internals.

#### Acceptance Criteria

1. THE Payment_Service SHALL define a hierarchy of exceptions: PaymentException (base), ProviderUnavailableException, InvalidPaymentException, and RefundException
2. IF a provider operation fails, THEN THE Payment_Service SHALL wrap the provider-specific exception in the appropriate platform exception with a user-safe message
3. THE Payment_Controller SHALL NOT expose provider-specific exception types, stack traces, or API keys in error responses
4. WHEN a payment operation fails, THE Payment_Service SHALL log the full exception details (including provider error codes) at ERROR level while returning only the sanitized message to the caller

### Requirement 10: Payment Logging and Security

**User Story:** As a security engineer, I want payment operations logged for audit without exposing sensitive data, so that the system is both observable and compliant.

#### Acceptance Criteria

1. THE Payment_Service SHALL log provider name, transaction ID, payment status, and operation duration for every payment operation at INFO level
2. THE Payment_Service SHALL NOT log secret keys, card numbers, webhook secrets, or customer payment details at any log level
3. WHEN a webhook signature verification fails, THE Webhook_Handler SHALL log the failure with the source IP and timestamp at WARN level without logging the raw payload
4. THE Payment_Service SHALL log refund operations with the original payment ID, refund amount, and initiating user at INFO level

### Requirement 11: Payment Status Lifecycle

**User Story:** As a developer, I want a well-defined payment status enum with clear transitions, so that the system maintains consistent payment state.

#### Acceptance Criteria

1. THE PaymentStatus enum SHALL define the values: PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED, REFUNDED, and EXPIRED
2. WHEN the Payment_Service creates a new payment, THE Payment_Service SHALL set the initial status to PENDING
3. WHEN the Payment_Service receives a webhook indicating successful payment, THE Payment_Service SHALL transition the status from PENDING or PROCESSING to SUCCESS
4. WHEN the Payment_Service processes a refund, THE Payment_Service SHALL transition the status from SUCCESS to REFUNDED
5. IF a payment status transition is invalid (e.g., refunding a PENDING payment), THEN THE Payment_Service SHALL throw an InvalidPaymentException

### Requirement 12: Provider Isolation and Testability

**User Story:** As a developer, I want the payment architecture to be fully testable with mock providers, so that unit tests verify business logic without external API calls.

#### Acceptance Criteria

1. THE Payment_Service SHALL accept PaymentProvider as a dependency through constructor injection, enabling mock providers in unit tests
2. WHEN unit testing the Payment_Service, THE test SHALL verify that no Stripe SDK or other provider SDK classes are imported or referenced by the Payment_Service class
3. THE Payment_Provider_Factory SHALL be testable in isolation by verifying correct provider resolution for each PaymentProviderType
4. WHEN integration testing the Stripe_Provider, THE test SHALL verify checkout session creation, payment verification, refund processing, and webhook signature validation against Stripe test APIs

