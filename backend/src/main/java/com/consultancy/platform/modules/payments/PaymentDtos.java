package com.consultancy.platform.modules.payments;

import jakarta.validation.constraints.NotBlank;

public final class PaymentDtos {
    private PaymentDtos() {
    }

    public record PaymentOrderResponse(String provider, String orderId, long amount, String currency, String keyId) {
    }

    public record VerifyRequest(@NotBlank String bookingPublicId, @NotBlank String razorpayOrderId,
                                @NotBlank String razorpayPaymentId, @NotBlank String razorpaySignature) {
    }
}
