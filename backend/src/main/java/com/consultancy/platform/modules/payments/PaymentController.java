package com.consultancy.platform.modules.payments;

import com.consultancy.platform.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/razorpay")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/verify")
    public ApiResponse<Void> verify(@Valid @RequestBody PaymentDtos.VerifyRequest request) {
        paymentService.verifyCheckout(request);
        return ApiResponse.ok(null, "Payment verified", MDC.get("traceId"));
    }

    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(@RequestBody String body, @RequestHeader("X-Razorpay-Signature") String signature) {
        paymentService.webhook(body, signature);
        return ApiResponse.ok(null, "Webhook accepted", MDC.get("traceId"));
    }
}
