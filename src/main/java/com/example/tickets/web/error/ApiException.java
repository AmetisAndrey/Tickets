package com.example.tickets.web.error;

public abstract class ApiException extends RuntimeException {
    protected ApiException(String message) { super(message); }
}