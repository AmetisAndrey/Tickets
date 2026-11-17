package com.example.tickets.report

import com.example.tickets.domain.TicketPriority
import com.example.tickets.domain.TicketStatus
import java.time.Instant

data class TicketReportRow(
    val id: Long,
    val status: TicketStatus,
    val priority: TicketPriority,
    val createdAt: Instant,
    val dueAt: Instant,
    val closedAt: Instant?
)