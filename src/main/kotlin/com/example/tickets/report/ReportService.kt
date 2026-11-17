package com.example.tickets.report

import com.example.tickets.domain.TicketStatus
import com.example.tickets.service.TicketReportDataPort
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class ReportService(
    private val port: TicketReportDataPort,
    private val clock: Clock
) {
    fun summary(from: Instant, to: Instant): ReportSummary {
        require(from.isBefore(to)) { "from must be before to" }
        val now = clock.instant()
        val rows = port.findCreatedBetween(from, to)

        val resolved = rows.filter { it.status == TicketStatus.RESOLVED && it.closedAt != null }
        val avg = resolved
            .map { Duration.between(it.createdAt, it.closedAt!!).toMinutes().toDouble() }
            .takeIf { it.isNotEmpty() }
            ?.average()

        return ReportSummary(
            totalCreated = rows.size,
            byStatus = rows.groupingBy { it.status.name }.eachCount(),
            byPriority = rows.groupingBy { it.priority.name }.eachCount(),
            openOverdue = rows.count {
                (it.status == TicketStatus.NEW || it.status == TicketStatus.IN_PROGRESS) &&
                        it.dueAt.isBefore(now)
            },
            avgResolutionMinutes = avg
        )
    }
}