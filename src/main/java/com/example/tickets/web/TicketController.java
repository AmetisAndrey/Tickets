package com.example.tickets.web;

import com.example.tickets.domain.Ticket;
import com.example.tickets.service.TicketService;
import com.example.tickets.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService service;

    public TicketController(TicketService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest req) {
        Ticket t = service.create(req);
        return ResponseEntity.created(URI.create("/api/tickets/" + t.getId()))
                .body(TicketResponse.from(t));
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return TicketResponse.from(service.get(id));
    }

    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @RequestBody UpdateTicketRequest req) {
        return TicketResponse.from(service.update(id, req));
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest req) {
        return TicketResponse.from(service.changeStatus(id, req.status()));
    }

    @GetMapping("/{id}/history")
    public List<HistoryResponse> history(@PathVariable Long id) {
        return service.history(id).stream()
                .map(h -> new HistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(), h.getChangedAt()))
                .toList();
    }

    @GetMapping
    public Page<TicketResponse> list(@RequestParam(required = false) com.example.tickets.domain.TicketStatus status,
                                     @RequestParam(required = false) com.example.tickets.domain.TicketPriority priority,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam(required = false) Boolean overdue,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        var filter = new TicketFilter(status, priority, categoryId, overdue);
        return service.list(filter, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(TicketResponse::from);
    }
}