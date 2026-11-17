package com.example.tickets.web.dto;

import com.example.tickets.domain.TicketStatus;
import java.time.Instant;

public record HistoryResponse(Long id, TicketStatus fromStatus, TicketStatus toStatus, Instant changedAt) {}