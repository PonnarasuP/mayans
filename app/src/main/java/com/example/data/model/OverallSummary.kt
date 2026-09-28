package com.example.data.model

data class OverallSummary(
    val totalCollected: Double = 0.0,
    val totalPending: Double = 0.0,
    val totalExpected: Double = 0.0,
    val collectionRate: Float = 0f,
    val totalPaidTransactions: Int = 0,
    val totalPendingInstances: Int = 0,
    val totalCashPendingInstances: Int = 0,
    val upiCollected: Double = 0.0,
    val cashCollected: Double = 0.0,
    val trackedMonths: List<String> = emptyList(),
    val memberSummaries: List<MemberOverallSummary> = emptyList(),
    val monthlyBreakdowns: List<MonthSummaryItem> = emptyList()
)

data class MemberOverallSummary(
    val member: Member,
    val totalContributed: Double,
    val totalPending: Double,
    val totalMonthsTracked: Int,
    val monthsPaid: Int,
    val monthsPending: Int,
    val completionRate: Float
)

data class MonthSummaryItem(
    val monthYear: String,
    val monthDisplay: String,
    val totalCollected: Double,
    val totalPending: Double,
    val targetAmount: Double,
    val paidCount: Int,
    val pendingCount: Int,
    val collectionRate: Float
)
