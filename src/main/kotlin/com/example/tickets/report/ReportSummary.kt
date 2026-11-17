package com.example.tickets.report

data class ReportSummary(
    val totalCreated: Int,
    val byStatus: Map<String, Int>,
    val byPriority: Map<String, Int>,
    val openOverdue: Int,
    val avgResolutionMinutes: Double?
)