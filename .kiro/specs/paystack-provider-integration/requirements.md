# Requirements Document

## Introduction

This document defines the requirements for integrating Paystack as a payment provider in the RainCheque ticketing system. Paystack is Stripe's sister company operating in Africa and supports businesses registered in South Africa. The integration follows the existing PaymentProvider abstraction layer, implementing all required operations (checkout, verify, refund, cancel, webhook) against the Paystack API. Paystack will be configured as the primary payment provider, replacing Stripe which cannot be used directly for South African businesses.

## Glossary

- **Paystack_Provider**: The concrete implementation of the PaymentProvider interface that communicates with the Paystack payment gateway API.
- **Payment_Provider_Factory**: The Spring component that resolves PaymentProvider implementations at runtime based on provider type and enabled status.
- **Checkout_Session**: A Paystack transaction initialization that returns an authorization URL where the customer completes payment on Paystack's hosted page.
- **Transaction_Reference**: A unique identifier returned by Paystack when a transaction is initialized, used to verify and track payment status.
- **Webhook_Event**: An HTTP POST notification sent by Paystack to the application server when a payment event occurs (e.g., charge success, charge failed, refund processed).
- **HMAC_Signature**: A hash-based message authentication code sent in the `x-paystack-signature` header of webhook requests, computed using the provider's secret key.
- **Payment_Properties**: The Spring configuration class binding payment provider credentials and settings from application.yml.
- **PaymentProviderType_Enum**: The Java enum listing all supported payment provider types.

## Requirements

### Requirement 1: Add PAYSTACK to PaymentProviderType Enum

**User Story:** As a developer, I want PAYSTACK added to the PaymentProviderType enum, so that the payment abstraction layer can reference Paystack as a valid provider.

#### Acceptance Criteria

1. THE PaymentProviderType_Enum SHALL include a PAYSTACK constant.
2. WHEN PAYSTACK is used as a provider type in configuration, THE Payment_Provider_Factory SHALL resolve the Paystack_Provider implementation.

### Requirement 2: Paystack Provider Configuration

**User Story:** As a system administrator, I want to configure Paystack credentials via environment variables and application.yml, so that the application connects to the correct Paystack account without hardcoded secrets.

#### Acceptance Criteria

1. THE Payment_Properties SHALL bind a PAYSTACK provider entry with fields for enabled status, api-key (Paystack secret key), and webhook-secret.
2. WHEN the PAYSTACK provider is enabled and the api-key is blank, THEN THE Payment_Properties SHALL throw an IllegalStateException at application startup.
3. WHEN the PAYSTACK provider is enabled and the webhook-secret is blank, THEN THE Payment_Properties SHALL throw an IllegalStateException at application startup.
4. WHEN the PAYSTACK provider is disabled, THE Payment_Provider_Factory SHALL exclude the Paystack_Provider from the available providers map.
5. THE application.yml SHALL reference environment variables PAYSTACK_SECRET_KEY and PAYSTACK_WEBHOOK_SECRET for the PAYSTACK provider credentials.

### Requirement 3: Initialize Checkout Session

**User Story:** As a customer, I want to be redirected to a Paystack hosted payment page when I purchase a ticket, so that I can pay securely using South African payment methods.

#### Acceptance Criteria

1. WHEN a PaymentRequest is received, THE Paystack_Provider SHALL call the Paystack Initialize Transaction API with the amount in kobo (amount multiplied by 100), the customer email, the currency as ZAR, the callback URL set to the success URL, and metadata containing eventId and ticketId.
2. WHEN the Paystack Initialize Transaction API responds successfully, THE Paystack_Provider SHALL return a PaymentResponse with the authorization_url as the checkoutUrl, the provider name set to PAYSTACK, and the status set to PENDING.
3. IF the Paystack Initialize Transaction API returns an error, THEN THE Paystack_Provider SHALL throw a PaymentException with a sanitized error message.
4. THE Paystack_Provider SHALL generate a unique reference for each transaction initialization request.

### Requirement 4: Verify Payment Status

**User Story:** As a system operator, I want to verify the current status of a Paystack payment, so that the system reflects accurate payment states.

#### Acceptance Criteria

1. WHEN a providerReference is provided, THE Paystack_Provider SHALL call the Paystack Verify Transaction API using that reference.
2. WHEN the Paystack Verify Transaction API returns status "success", THE Paystack_Provider SHALL map the status to PaymentStatus SUCCESS.
3. WHEN the Paystack Verify Transaction API returns status "failed", THE Paystack_Provider SHALL map the status to PaymentStatus FAILED.
4. WHEN the Paystack Verify Transaction API returns status "abandoned", THE Paystack_Provider SHALL map the status to PaymentStatus CANCELLED.
5. WHEN the Paystack Verify Transaction API returns status "pending", THE Paystack_Provider SHALL map the status to PaymentStatus PROCESSING.
6. IF the Paystack Verify Transaction API returns an error, THEN THE Paystack_Provider SHALL throw a PaymentException with a sanitized error message.

### Requirement 5: Process Refunds

**User Story:** As an administrator, I want to refund a completed Paystack payment, so that customers receive their money back when a ticket event is cancelled or a dispute is resolved.

#### Acceptance Criteria

1. WHEN a refund is requested, THE Paystack_Provider SHALL call the Paystack Create Refund API with the transaction reference and the refund amount in kobo.
2. WHEN the Paystack Create Refund API responds successfully, THE Paystack_Provider SHALL return a PaymentResponse with status REFUNDED.
3. IF the Paystack Create Refund API returns an error, THEN THE Paystack_Provider SHALL throw a PaymentException with a sanitized error message.

### Requirement 6: Cancel Payment

**User Story:** As a customer, I want to cancel a pending payment, so that my ticket hold is released and I am not charged.

#### Acceptance Criteria

1. WHEN a cancel is requested for a pending transaction, THE Paystack_Provider SHALL return a PaymentResponse with status CANCELLED.
2. IF the transaction has already been completed (status is "success"), THEN THE Paystack_Provider SHALL throw a PaymentException indicating that a completed payment cannot be cancelled.

### Requirement 7: Webhook Event Processing

**User Story:** As a system operator, I want Paystack webhook events to automatically update payment statuses, so that the system stays synchronized with actual payment outcomes without manual polling.

#### Acceptance Criteria

1. WHEN a webhook payload and signature are received, THE Paystack_Provider SHALL compute an HMAC SHA-512 hash of the payload using the configured webhook secret and compare the result to the provided signature.
2. IF the computed HMAC does not match the provided signature, THEN THE Paystack_Provider SHALL throw a PaymentException indicating webhook signature verification failure.
3. WHEN the webhook event type is "charge.success", THE Paystack_Provider SHALL map the status to PaymentStatus SUCCESS.
4. WHEN the webhook event type is "charge.failed", THE Paystack_Provider SHALL map the status to PaymentStatus FAILED.
5. WHEN the webhook event type is "refund.processed", THE Paystack_Provider SHALL map the status to PaymentStatus REFUNDED.
6. THE Paystack_Provider SHALL extract the transaction reference from the webhook payload data object and include the reference in the returned PaymentWebhookEvent.
7. THE PaymentController webhook endpoint SHALL accept the header name "x-paystack-signature" when the provider path variable is PAYSTACK.

### Requirement 8: Retrieve Payment Details

**User Story:** As a system operator, I want to retrieve full payment details from Paystack, so that the system can display transaction information to users.

#### Acceptance Criteria

1. WHEN a providerReference is provided, THE Paystack_Provider SHALL call the Paystack Verify Transaction API and return a PaymentResponse with the current status, amount (converted from kobo to rands), currency, and paid-at timestamp.
2. IF the Paystack Verify Transaction API returns an error, THEN THE Paystack_Provider SHALL throw a PaymentException with a sanitized error message.

### Requirement 9: Payment History

**User Story:** As a system operator, I want to retrieve the payment history for a transaction, so that auditing and support queries have full context.

#### Acceptance Criteria

1. WHEN a providerReference is provided, THE Paystack_Provider SHALL call the Paystack Verify Transaction API and return the current transaction state as a single-item list of PaymentResponse.
2. IF the Paystack Verify Transaction API returns an error, THEN THE Paystack_Provider SHALL throw a PaymentException with a sanitized error message.

### Requirement 10: Error Message Sanitization

**User Story:** As a developer, I want Paystack error messages sanitized before being exposed to clients, so that secret keys and internal identifiers are never leaked in API responses.

#### Acceptance Criteria

1. THE Paystack_Provider SHALL remove any occurrence of Paystack secret key patterns (sk_live_*, sk_test_*) from error messages before returning them.
2. THE Paystack_Provider SHALL remove any internal Paystack transaction identifiers from error messages before returning them.
3. WHEN an error message contains redacted content, THE Paystack_Provider SHALL return a generic message: "Payment operation failed. Please try again or contact support."

### Requirement 11: Set Paystack as Default Provider

**User Story:** As a system administrator, I want Paystack configured as the default payment provider, so that checkout requests use Paystack without requiring the frontend to specify a provider.

#### Acceptance Criteria

1. THE application.yml SHALL set the default-provider to PAYSTACK.
2. WHEN a PaymentRequest does not specify a provider, THE payment service SHALL route the request to the Paystack_Provider.
3. THE application.yml SHALL set the STRIPE provider enabled flag to false.

### Requirement 12: Webhook Endpoint Header Flexibility

**User Story:** As a developer, I want the webhook endpoint to accept provider-specific signature headers, so that Paystack webhooks are processed without requiring the Stripe-Signature header.

#### Acceptance Criteria

1. WHEN the provider path variable is PAYSTACK, THE PaymentController SHALL read the signature from the "x-paystack-signature" request header.
2. WHEN the provider path variable is STRIPE, THE PaymentController SHALL read the signature from the "Stripe-Signature" request header.
3. IF no recognized signature header is present for the given provider, THEN THE PaymentController SHALL return HTTP 400 Bad Request.
