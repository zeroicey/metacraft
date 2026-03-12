package com.metacraft.api.modules.ai.service.opencode;

public class OpenCodeClientException extends RuntimeException {

    public OpenCodeClientException(String message) {
        super(message);
    }

    public OpenCodeClientException(String message, Throwable cause) {
        super(message, cause);
    }
}