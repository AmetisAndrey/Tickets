package com.example.tickets.service;

import com.example.tickets.report.TicketReportRow;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class JpaTicketReportDataAdapter implements TicketReportDataPort {

    private final EntityManager em;

    public JpaTicketReportDataAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<TicketReportRow> findCreatedBetween(Instant from, Instant to) {
        return em.createQuery("""
            select new com.example.tickets.report.TicketReportRow(
                t.id, t.status, t.priority, t.createdAt, t.dueAt, t.closedAt)
            from Ticket t
            where t.createdAt >= :from and t.createdAt < :to
            """, TicketReportRow.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }
}