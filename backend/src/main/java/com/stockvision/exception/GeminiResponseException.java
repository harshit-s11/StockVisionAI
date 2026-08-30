package com.stockvision.exception;

public class GeminiResponseException extends RuntimeException {
    public GeminiResponseException(String message) {
        super(message);
    }
}