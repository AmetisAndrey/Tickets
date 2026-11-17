package com.example.tickets.web.dto;

import com.example.tickets.domain.TicketPriority;
import com.example.tickets.domain.TicketStatus;

public record TicketFilter(TicketStatus status, TicketPriority priority, Long categoryId, Boolean overdue) {}