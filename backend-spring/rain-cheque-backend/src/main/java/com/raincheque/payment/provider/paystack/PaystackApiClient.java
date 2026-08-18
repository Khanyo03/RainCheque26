package com.raincheque.payment.provider.paystack;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

/**
 * Internal helper that encapsulates HTTP communication with the Paystack REST API.
 * Configured with Bearer token authorization, JSON content type, and timeouts.
 */
class PaystackApiClient {

    private final RestClient restClient;

    PaystackApiClient(String secretKey) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.paystack.co")
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    PaystackInitializeResponse initializeTransaction(Map<String, Object> body) {
        return restClient.post()
                .uri("/transaction/initialize")
                .body(body)
                .retrieve()
                .body(PaystackInitializeResponse.class);
    }

    PaystackVerifyResponse verifyTransaction(String reference) {
        return restClient.get()
                .uri("/transaction/verify/{reference}", reference)
                .retrieve()
                .body(PaystackVerifyResponse.class);
    }

    PaystackRefundResponse createRefund(Map<String, Object> body) {
        return restClient.post()
                .uri("/refund")
                .body(body)
                .retrieve()
                .body(PaystackRefundResponse.class);
    }
}
