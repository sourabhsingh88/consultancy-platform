package com.consultancy.platform.common.api;

import java.time.Instant;

public record ApiResponse<T>(boolean success, String message, T data, PageMeta meta, String traceId, Instant timestamp) {
    public static <T> ApiResponse<T> ok(T data, String message, String traceId) {
        return new ApiResponse<>(true, message, data, null, traceId, Instant.now());
    }

    public static <T> ApiResponse<T> page(T data, PageMeta meta, String traceId) {
        return new ApiResponse<>(true, "OK", data, meta, traceId, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, String traceId) {
        return new ApiResponse<>(false, message, null, null, traceId, Instant.now());
    }
}
