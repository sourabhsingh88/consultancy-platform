package com.consultancy.platform.modules.payments;

import com.consultancy.platform.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class RazorpayClient {
    private final RestClient restClient = RestClient.create("https://api.razorpay.com/v1");
    private final String keyId;
    private final String keySecret;

    public RazorpayClient(@Value("${app.razorpay.key-id}") String keyId,
                          @Value("${app.razorpay.key-secret}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public String keyId() {
        return keyId;
    }

    public String createOrder(long amount, String currency, String receipt) {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            return "local_order_" + receipt;
        }
        Map<?, ?> response = restClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8)))
                .body(Map.of("amount", amount, "currency", currency, "receipt", receipt, "payment_capture", 1))
                .retrieve()
                .body(Map.class);
        if (response == null || response.get("id") == null) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Unable to create Razorpay order");
        }
        return String.valueOf(response.get("id"));
    }
}
