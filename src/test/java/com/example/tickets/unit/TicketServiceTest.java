package com.example.tickets.unit;

import com.example.tickets.domain.*;
import com.example.tickets.repository.*;
import com.example.tickets.service.*;
import com.example.tickets.web.dto.CreateTicketRequest;
import com.example.tickets.web.error.ConflictException;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class TicketServiceTest {
    private TicketRepository tickets;
    private CategoryRepository categories;
    private TicketStatusHistoryRepository history;
    private TicketService service;
    private final Clock fixed = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        tickets = Mockito.mock(TicketRepository.class);
        categories = Mockito.mock(CategoryRepository.class);
        history = Mockito.mock(TicketStatusHistoryRepository.class);
        service = new TicketService(tickets, categories, history, new TicketStatusMachine(), fixed);
    }

    @Test
    void createsTicketWithStatusNewAndHistoryRecord() {
        var cat = new Category("Hardware");
        Mockito.when(categories.findById(1L)).thenReturn(Optional.of(cat));
        Mockito.when(tickets.save(any())).thenAnswer(i -> i.getArgument(0));

        var t = service.create(new CreateTicketRequest("t", "d", 1L, TicketPriority.HIGH,
                fixed.instant().plusSeconds(3600)));

        assertThat(t.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(t.getCreatedAt()).isEqualTo(fixed.instant());
        Mockito.verify(history).save(any(TicketStatusHistory.class));
    }

    @Test
    void rejectsTransitionFromResolved() {
        var t = new Ticket("t", "d", new Category("x"), TicketPriority.LOW,
                fixed.instant(), fixed.instant().plusSeconds(60));
        t.setStatus(TicketStatus.RESOLVED);
        Mockito.when(tickets.findById(9L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.changeStatus(9L, TicketStatus.IN_PROGRESS))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void overdueWhenDueAtEqualsNowForOpenTicket() {
        var t = new Ticket("t", "d", new Category("x"), TicketPriority.LOW,
                fixed.instant().minusSeconds(60), fixed.instant());
        // dueAt == now, статус NEW -> по правилу "срок истёк" (isBefore) не просрочена
        assertThat(t.isOverdue(fixed.instant())).isFalse();
        assertThat(t.isOverdue(fixed.instant().plusSeconds(1))).isTrue();
    }

    @Test
    void notOverdueWhenResolved() {
        var t = new Ticket("t", "d", new Category("x"), TicketPriority.LOW,
                fixed.instant().minusSeconds(7200), fixed.instant().minusSeconds(60));
        t.setStatus(TicketStatus.RESOLVED);
        assertThat(t.isOverdue(fixed.instant())).isFalse();
    }

    @Test
    void cannotEditClosedTicket() {
        var t = new Ticket("t", "d", new Category("x"), TicketPriority.LOW,
                fixed.instant(), fixed.instant().plusSeconds(60));
        t.setStatus(TicketStatus.CANCELLED);
        Mockito.when(tickets.findById(5L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.update(5L,
                new com.example.tickets.web.dto.UpdateTicketRequest("new", null, null)))
                .isInstanceOf(ConflictException.class);
    }
}