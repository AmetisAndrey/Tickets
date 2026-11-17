package com.example.tickets.service;

import com.example.tickets.domain.*;
import com.example.tickets.repository.CategoryRepository;
import com.example.tickets.repository.TicketRepository;
import com.example.tickets.repository.TicketStatusHistoryRepository;
import com.example.tickets.web.dto.*;
import com.example.tickets.web.error.ConflictException;
import com.example.tickets.web.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository tickets;
    private final CategoryRepository categories;
    private final TicketStatusHistoryRepository history;
    private final TicketStatusMachine machine;
    private final Clock clock;

    public TicketService(TicketRepository tickets,
                         CategoryRepository categories,
                         TicketStatusHistoryRepository history,
                         TicketStatusMachine machine,
                         Clock clock) {
        this.tickets = tickets;
        this.categories = categories;
        this.history = history;
        this.machine = machine;
        this.clock = clock;
    }

    @Transactional
    public Ticket create(CreateTicketRequest req) {
        Category cat = categories.findById(req.categoryId())
                .orElseThrow(() -> new NotFoundException("Category " + req.categoryId()));
        Instant now = clock.instant();
        Ticket t = new Ticket(req.title(), req.description(), cat, req.priority(), now, req.dueAt());
        t = tickets.save(t);
        history.save(new TicketStatusHistory(t.getId(), null, TicketStatus.NEW, now));
        return t;
    }

    public Ticket get(Long id) {
        return tickets.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket " + id));
    }

    @Transactional
    public Ticket update(Long id, UpdateTicketRequest req) {
        Ticket t = get(id);
        if (!t.isEditable()) {
            throw new ConflictException("Ticket is closed or cancelled");
        }
        if (req.description() != null) t.setDescription(req.description());
        if (req.priority() != null)    t.setPriority(req.priority());
        if (req.dueAt() != null)       t.setDueAt(req.dueAt());
        return t;
    }

    @Transactional
    public Ticket changeStatus(Long id, TicketStatus target) {
        Ticket t = get(id);
        if (!machine.canTransition(t.getStatus(), target)) {
            throw new ConflictException(
                    "Illegal transition " + t.getStatus() + " -> " + target);
        }
        Instant now = clock.instant();
        TicketStatus prev = t.getStatus();
        t.setStatus(target);
        if (target == TicketStatus.RESOLVED || target == TicketStatus.CANCELLED) {
            t.setClosedAt(now);
        }
        history.save(new TicketStatusHistory(t.getId(), prev, target, now));
        return t;
    }

    public List<TicketStatusHistory> history(Long id) {
        get(id);
        return history.findByTicketIdOrderByChangedAtAsc(id);
    }

    public Page<Ticket> list(TicketFilter filter, Pageable pageable) {
        Specification<Ticket> spec = Specification.where(null);
        if (filter.status() != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), filter.status()));
        }
        if (filter.priority() != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("priority"), filter.priority()));
        }
        if (filter.categoryId() != null) {
            spec = spec.and((r, q, cb) ->
                    cb.equal(r.get("category").get("id"), filter.categoryId()));
        }
        if (Boolean.TRUE.equals(filter.overdue())) {
            Instant now = clock.instant();
            spec = spec.and((r, q, cb) -> cb.and(
                    r.get("status").in(TicketStatus.NEW, TicketStatus.IN_PROGRESS),
                    cb.lessThan(r.get("dueAt"), now)));
        }
        return tickets.findAll(spec, pageable);
    }
}