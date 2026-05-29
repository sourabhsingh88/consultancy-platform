package com.consultancy.platform.modules.bookings;

import com.consultancy.platform.modules.payments.PaymentDtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class BookingDtos {
    private BookingDtos() {
    }

    public record CreateConsultationRequest(@NotBlank String consultantPublicId, @NotNull Instant startsAt,
                                            @NotNull Instant endsAt, String notes) {
    }

    public record SeminarRegistrationRequest(String notes) {
    }

    public record BookingResponse(String bookingPublicId, String meetingPublicId, String status,
                                  PaymentDtos.PaymentOrderResponse paymentOrder) {
    }
}
