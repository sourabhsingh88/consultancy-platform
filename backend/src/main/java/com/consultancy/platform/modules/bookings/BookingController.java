package com.consultancy.platform.modules.bookings;

import com.consultancy.platform.common.api.ApiResponse;
import com.consultancy.platform.common.api.CurrentUser;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/bookings/consultations")
    public ApiResponse<BookingDtos.BookingResponse> bookConsultation(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                                     @Valid @RequestBody BookingDtos.CreateConsultationRequest request,
                                                                     Authentication authentication) {
        return ApiResponse.ok(bookingService.bookConsultation(CurrentUser.publicId(authentication), idempotencyKey, request), "Booking created", MDC.get("traceId"));
    }

    @PostMapping("/seminars/{seminarPublicId}/registrations")
    public ApiResponse<BookingDtos.BookingResponse> registerSeminar(@PathVariable String seminarPublicId,
                                                                    @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                                    Authentication authentication) {
        return ApiResponse.ok(bookingService.registerSeminar(CurrentUser.publicId(authentication), seminarPublicId, idempotencyKey), "Registration created", MDC.get("traceId"));
    }

    @PostMapping("/bookings/{bookingPublicId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String bookingPublicId,
                                    @RequestParam(defaultValue = "Cancelled by user") String reason,
                                    Authentication authentication) {
        bookingService.cancel(CurrentUser.publicId(authentication), bookingPublicId, reason);
        return ApiResponse.ok(null, "Booking cancelled", MDC.get("traceId"));
    }
}
