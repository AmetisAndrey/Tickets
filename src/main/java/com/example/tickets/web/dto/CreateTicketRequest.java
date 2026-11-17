package com.example.tickets.web.dto;

import com.example.tickets.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateTicketRequest(
        @NotBlank String title,
        String description,
        @NotNull Long categoryId,
        @NotNull TicketPriority priority,
        @NotNull Instant dueAt
) {}