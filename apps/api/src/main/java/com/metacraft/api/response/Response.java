package com.metacraft.api.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class Response {
    private int status;
    private final String message;
    private Object data;
    private Object error;

    private Response(String message, int status) {
        this.message = message;
        this.status = status;
        this.data = null;
        this.error = null;
    }

    public static Response success(String message) {
        return new Response(message, HttpStatus.OK.value());
    }

    public static Response error(String message) {
        return new Response(message, HttpStatus.BAD_REQUEST.value());
    }

    public Response data(Object data) {
        this.data = data;
        return this;
    }

    public Response error(Object error) {
        this.error = error;
        return this;
    }

    public Response status(int status) {
        this.status = status;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<ApiResponse<T>> build() {
        ApiResponse<T> response;
        
        if (this.error != null) {
            response = ApiResponse.fail(this.message, this.error);
        } else if (this.data != null) {
            response = (ApiResponse<T>) ApiResponse.ok(this.message, this.data);
        } else {
            response = ApiResponse.ok(this.message);
        }
        
        return ResponseEntity.status(this.status).body(response);
    }
}
