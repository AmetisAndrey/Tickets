package com.example.tickets.report

import com.example.tickets.domain.TicketPriority
import com.example.tickets.domain.TicketStatus
import com.example.tickets.service.TicketReportDataPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class ReportServiceTest {
    private val now = Instant.parse("2025-01-15T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val port: TicketReportDataPort = mock()
    private val service = ReportService(port, clock)
    private val from = now.minus(Duration.ofDays(1))
    private val to = now.plus(Duration.ofDays(1))

    @Test
    fun `empty set returns zero and null average`() {
        whenever(port.findCreatedBetween(from, to)).thenReturn(emptyList())
        val s = service.summary(from, to)
        assertThat(s.totalCreated).isZero()
        assertThat(s.avgResolutionMinutes).isNull()
    }

    @Test
    fun `no resolved tickets returns null average`() {
        whenever(port.findCreatedBetween(from, to)).thenReturn(listOf(
            TicketReportRow(1, TicketStatus.NEW, TicketPriority.HIGH, now, now.plusSeconds(60), null)
        ))
        val s = service.summary(from, to)
        assertThat(s.avgResolutionMinutes).isNull()
        assertThat(s.byStatus["NEW"]).isEqualTo(1)
    }

    @Test
    fun `computes distribution overdue and average`() {
        whenever(port.findCreatedBetween(from, to)).thenReturn(listOf(
            TicketReportRow(1, TicketStatus.RESOLVED, TicketPriority.HIGH, now, now.plusSeconds(600), now.plusSeconds(120)),
            TicketReportRow(2, TicketStatus.RESOLVED, TicketPriority.LOW, now, now.plusSeconds(600), now.plusSeconds(240)),
            TicketReportRow(3, TicketStatus.NEW, TicketPriority.LOW, now, now.minusSeconds(60), null)
        ))
        val s = service.summary(from, to)
        assertThat(s.totalCreated).isEqualTo(3)
        assertThat(s.byStatus["RESOLVED"]).isEqualTo(2)
        assertThat(s.byStatus["NEW"]).isEqualTo(1)
        assertThat(s.byPriority["HIGH"]).isEqualTo(1)
        assertThat(s.openOverdue).isEqualTo(1)
        assertThat(s.avgResolutionMinutes).isEqualTo(3.0)
    }

    @Test
    fun `invalid period rejected`() {
        assertThrows(IllegalArgumentException::class.java) { service.summary(to, from) }
    }
}