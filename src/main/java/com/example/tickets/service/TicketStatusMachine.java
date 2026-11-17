package com.example.tickets.service;

import com.example.tickets.domain.TicketStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Машина состояний заявки.
 * Разрешённые переходы:
 *   NEW         -> IN_PROGRESS, CANCELLED
 *   IN_PROGRESS -> RESOLVED, CANCELLED
 *   RESOLVED    -> (нет)
 *   CANCELLED   -> (нет)
 */
@Component
public class TicketStatusMachine {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = Map.of(
            TicketStatus.NEW,         EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED),
            TicketStatus.RESOLVED,    EnumSet.noneOf(TicketStatus.class),
            TicketStatus.CANCELLED,   EnumSet.noneOf(TicketStatus.class)
    );

    public boolean canTransition(TicketStatus from, TicketStatus to) {
        if (from == null || to == null) return false;
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }
}