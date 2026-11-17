package com.example.tickets.web.dto;

import com.example.tickets.domain.TicketPriority;
import java.time.Instant;

public record UpdateTicketRequest(String description, TicketPriority priority, Instant dueAt) {}