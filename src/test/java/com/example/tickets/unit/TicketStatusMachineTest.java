package com.example.tickets.unit;

import com.example.tickets.domain.TicketStatus;
import com.example.tickets.service.TicketStatusMachine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketStatusMachineTest {
    private final TicketStatusMachine m = new TicketStatusMachine();

    @Test void newToInProgressAllowed()   { assertThat(m.canTransition(TicketStatus.NEW, TicketStatus.IN_PROGRESS)).isTrue(); }
    @Test void newToCancelledAllowed()    { assertThat(m.canTransition(TicketStatus.NEW, TicketStatus.CANCELLED)).isTrue(); }
    @Test void newToResolvedForbidden()   { assertThat(m.canTransition(TicketStatus.NEW, TicketStatus.RESOLVED)).isFalse(); }
    @Test void inProgressToResolved()     { assertThat(m.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED)).isTrue(); }
    @Test void resolvedCannotReopen()     { assertThat(m.canTransition(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS)).isFalse(); }
    @Test void cancelledCannotReopen()    { assertThat(m.canTransition(TicketStatus.CANCELLED, TicketStatus.NEW)).isFalse(); }
}