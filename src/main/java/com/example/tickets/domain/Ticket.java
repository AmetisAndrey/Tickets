package com.example.tickets.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    @ManyToOne(optional = false) @JoinColumn(name = "category_id")
    private Category category;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TicketPriority priority;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TicketStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;
    @Column(name = "closed_at")
    private Instant closedAt;

    protected Ticket() {}

    public Ticket(String title, String description, Category category,
                  TicketPriority priority, Instant createdAt, Instant dueAt) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = TicketStatus.NEW;
        this.createdAt = createdAt;
        this.dueAt = dueAt;
    }

    public boolean isOverdue(Instant now) {
        return (status == TicketStatus.NEW || status == TicketStatus.IN_PROGRESS)
                && dueAt.isBefore(now);
    }

    public boolean isEditable() {
        return status != TicketStatus.RESOLVED && status != TicketStatus.CANCELLED;
    }

    // getters/setters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}