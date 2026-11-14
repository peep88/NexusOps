package com.nexusops.shared.web;

public record ApiResponse<T>(T data, Meta meta, ApiError error) {
    public record Meta(String requestId) {}
    public record ApiError(String code, String message) {}

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(data, new Meta(requestId), null);
    }
}

