package com.example.tickets.web.error;

public class NotFoundException extends ApiException {
    public NotFoundException(String message) { super(message); }
}