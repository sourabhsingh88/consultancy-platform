package com.consultancy.platform.modules.consultants;

import jakarta.validation.constraints.*;

import java.time.*;
import java.util.List;

public final class ConsultantDtos {
    private ConsultantDtos() {
    }

    public record UpsertProfileRequest(@NotBlank String headline, @NotBlank String bio, @NotBlank String timezone,
                                       @Min(0) long defaultPriceAmount, @NotBlank String currency) {
    }

    public record ConsultantResponse(String publicId, String userPublicId, String displayName, String headline, String bio,
                                     String timezone, long defaultPriceAmount, String currency) {
    }

    public record AvailabilityRuleRequest(@NotBlank String timezone, @NotNull LocalDate startDate, LocalDate endDate,
                                          @NotNull LocalTime startTime, @NotNull LocalTime endTime,
                                          @Min(5) @Max(480) int slotDurationMinutes,
                                          @Min(0) @Max(120) int bufferBeforeMinutes,
                                          @Min(0) @Max(120) int bufferAfterMinutes,
                                          @NotBlank String recurrenceFrequency,
                                          @Min(1) @Max(12) int recurrenceInterval,
                                          List<String> daysOfWeek) {
    }

    public record SlotResponse(String publicId, Instant startsAt, Instant endsAt, String timezone, long priceAmount, String currency) {
    }

    public record SeminarRequest(@NotBlank String title, String description, @NotBlank String timezone,
                                 @NotNull Instant startsAt, @NotNull Instant endsAt,
                                 @Min(1) @Max(200) int maxParticipants,
                                 @Min(0) long priceAmount, @NotBlank String currency) {
    }

    public record MeetingResponse(String publicId, String title, String meetingType, String status, Instant startsAt, Instant endsAt,
                                  long priceAmount, String currency, Integer maxParticipants, Integer confirmedCount) {
    }
}
