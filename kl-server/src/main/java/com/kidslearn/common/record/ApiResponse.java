package com.kidslearn.common.record;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    T data,
    String traceId,
    Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, UUID.randomUUID().toString(), Instant.now());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(null, UUID.randomUUID().toString(), Instant.now());
    }
}
