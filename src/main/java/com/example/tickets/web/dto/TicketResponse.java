package com.example.tickets.web.dto;

import com.example.tickets.domain.*;

import java.time.Instant;

public record TicketResponse(
        Long id, String title, String description, Long categoryId, String categoryName,
        TicketPriority priority, TicketStatus status, Instant createdAt, Instant dueAt, Instant closedAt
) {
    public static TicketResponse from(Ticket t) {
        return new TicketResponse(t.getId(), t.getTitle(), t.getDescription(),
                t.getCategory().getId(), t.getCategory().getName(),
                t.getPriority(), t.getStatus(), t.getCreatedAt(), t.getDueAt(), t.getClosedAt());
    }
}