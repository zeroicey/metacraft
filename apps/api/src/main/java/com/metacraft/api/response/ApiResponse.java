package com.metacraft.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String message, T data, Object error) {

    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(message, null, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(message, data, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(message, null, null);
    }

    public static <T> ApiResponse<T> fail(String message, Object error) {
        return new ApiResponse<>(message, null, error);
    }
}
