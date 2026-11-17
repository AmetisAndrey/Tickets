package com.example.tickets.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ticket_status_history")
public class TicketStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    @Enumerated(EnumType.STRING) @Column(name = "from_status")
    private TicketStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false)
    private TicketStatus toStatus;
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected TicketStatusHistory() {}

    public TicketStatusHistory(Long ticketId, TicketStatus from, TicketStatus to, Instant at) {
        this.ticketId = ticketId;
        this.fromStatus = from;
        this.toStatus = to;
        this.changedAt = at;
    }

    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public TicketStatus getFromStatus() { return fromStatus; }
    public TicketStatus getToStatus() { return toStatus; }
    public Instant getChangedAt() { return changedAt; }
}