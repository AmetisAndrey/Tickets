package com.example.tickets.report

import com.example.tickets.domain.TicketPriority
import com.example.tickets.domain.TicketStatus
import com.example.tickets.service.TicketReportDataPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

class TicketReportDataPortTest {

    @Test
    fun `java port returns rows passed through`() {
        val from = Instant.parse("2025-01-01T00:00:00Z")
        val to   = Instant.parse("2025-01-02T00:00:00Z")
        val row  = TicketReportRow(
            1, TicketStatus.NEW, TicketPriority.LOW,
            Instant.parse("2025-01-01T12:00:00Z"),
            Instant.parse("2025-01-01T18:00:00Z"),
            null
        )
        val port: TicketReportDataPort = mock()
        whenever(port.findCreatedBetween(from, to)).thenReturn(listOf(row))

        val result = port.findCreatedBetween(from, to)

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(1)
        assertThat(result[0].status).isEqualTo(TicketStatus.NEW)
    }
}