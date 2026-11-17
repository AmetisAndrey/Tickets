package com.example.tickets.service;

import com.example.tickets.report.TicketReportRow;

import java.time.Instant;
import java.util.List;


public interface TicketReportDataPort {
    List<TicketReportRow> findCreatedBetween(Instant from, Instant to);
}