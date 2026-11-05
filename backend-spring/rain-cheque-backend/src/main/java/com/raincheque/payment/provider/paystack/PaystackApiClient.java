package com.raincheque.payment.provider.paystack;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.util.Map;

import org.springframework.web.client.RestClient;

/**
 * Internal helper that encapsulates HTTP communication with the Paystack REST API.
 * Configured with Bearer token authorization, JSON content type, and timeouts.
 */
class PaystackApiClient {

    private final RestClient restClient;

    PaystackApiClient(String secretKey) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.paystack.co")
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(factory)
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
