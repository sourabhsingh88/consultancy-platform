package com.consultancy.platform.modules.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class RazorpaySignatureVerifier {
    private final String keySecret;
    private final String webhookSecret;

    public RazorpaySignatureVerifier(@Value("${app.razorpay.key-secret}") String keySecret,
                                     @Value("${app.razorpay.webhook-secret}") String webhookSecret) {
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    public boolean verifyCheckout(String orderId, String paymentId, String signature) {
        return verify(orderId + "|" + paymentId, signature, keySecret);
    }

    public boolean verifyWebhook(String body, String signature) {
        return verify(body, signature, webhookSecret);
    }

    private boolean verify(String payload, String signature, String secret) {
        if (secret == null || secret.isBlank()) {
            return payload.startsWith("local_order_");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return false;
        }
    }
}
