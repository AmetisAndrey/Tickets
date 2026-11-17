package com.example.tickets.web.error;

public class ConflictException extends ApiException {
    public ConflictException(String message) { super(message); }
}