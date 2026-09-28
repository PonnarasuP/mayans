package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AppConfig
import com.example.data.model.AuditLog
import com.example.data.model.Contribution
import com.example.data.model.Member
import com.example.data.model.MemberOverallSummary
import com.example.data.model.MemberWithContribution
import com.example.data.model.MonthSummaryItem
import com.example.data.model.OverallSummary
import com.example.data.repository.WelfareRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import com.example.data.remote.CloudSyncState
import com.example.data.remote.FirestoreSyncManager
import java.util.Date
import java.util.Locale

class WelfareViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val syncManager = FirestoreSyncManager(application, database)
    private val repository = WelfareRepository(database, syncManager)

    val syncState: StateFlow<CloudSyncState> = syncManager.syncState
    val syncStatusMessage: StateFlow<String> = syncManager.statusMessage
    val lastSyncTimestamp: StateFlow<Long?> = syncManager.lastSyncTimestamp
    val isRealtimeActive: StateFlow<Boolean> = syncManager.isRealtimeActive

    init {
        syncManager.startRealtimeSync(viewModelScope)
    }

    fun syncAllToCloud(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = syncManager.syncAllLocalToCloud()
            onResult(res.first, res.second)
        }
    }

    fun fetchAllFromCloud(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = syncManager.fetchAllFromCloud()
            onResult(res.first, res.second)
        }
    }

    private val _selectedMonthYear = MutableStateFlow(getCurrentMonthYear())
    val selectedMonthYear: StateFlow<String> = _selectedMonthYear.asStateFlow()

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private val _selectedMemberId = MutableStateFlow<Long?>(2L) // Default to Aarthi Sundaram
    val selectedMemberId: StateFlow<Long?> = _selectedMemberId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL") // ALL, PAID, PENDING, CASH_PENDING, OVERDUE
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _dashboardTab = MutableStateFlow("OVERALL") // "OVERALL" or "MONTHLY"
    val dashboardTab: StateFlow<String> = _dashboardTab.asStateFlow()

    val appConfig: StateFlow<AppConfig> = repository.config
        .map { it ?: AppConfig() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppConfig()
        )

    val allMembers: StateFlow<List<Member>> = repository.allMembers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val auditLogs: StateFlow<List<AuditLog>> = repository.auditLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cashPendingList: StateFlow<List<MemberWithContribution>> =
        repository.cashPendingVerifications
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val overallSummary: StateFlow<OverallSummary> = combine(
        repository.allMembers,
        repository.allContributions,
        appConfig
    ) { members, contributions, config ->
        val activeMembers = members.filter { it.isActive }
        val monthlyAmount = config.monthlyAmount

        val distinctMonths = contributions.map { it.monthYear }.distinct().sortedDescending()
        val trackedMonths = if (distinctMonths.isEmpty()) listOf(getCurrentMonthYear()) else distinctMonths

        var totalCollected = 0.0
        var upiCollected = 0.0
        var cashCollected = 0.0
        var paidTransactions = 0
        var cashPendingCount = 0

        contributions.forEach { c ->
            if (c.status == Contribution.STATUS_PAID) {
                totalCollected += c.amount
                paidTransactions++
                if (c.paymentMethod == Contribution.METHOD_UPI) {
                    upiCollected += c.amount
                } else if (c.paymentMethod == Contribution.METHOD_CASH) {
                    cashCollected += c.amount
                }
            } else if (c.status == Contribution.STATUS_CASH_PENDING) {
                cashPendingCount++
            }
        }

        val totalExpected = trackedMonths.size * activeMembers.size * monthlyAmount
        val totalPending = maxOf(0.0, totalExpected - totalCollected)
        val collectionRate = if (totalExpected > 0) ((totalCollected / totalExpected) * 100).toFloat() else 0f

        val memberSummaries = activeMembers.map { member ->
            val memberContribs = contributions.filter { it.memberId == member.id }
            val memberPaidContribs = memberContribs.filter { it.status == Contribution.STATUS_PAID }
            val memberTotalPaid = memberPaidContribs.sumOf { it.amount }
            val memberExpected = trackedMonths.size * monthlyAmount
            val memberPending = maxOf(0.0, memberExpected - memberTotalPaid)
            val monthsPaid = memberPaidContribs.map { it.monthYear }.distinct().size
            val monthsPending = maxOf(0, trackedMonths.size - monthsPaid)
            val rate = if (memberExpected > 0) ((memberTotalPaid / memberExpected) * 100).toFloat() else 0f

            MemberOverallSummary(
                member = member,
                totalContributed = memberTotalPaid,
                totalPending = memberPending,
                totalMonthsTracked = trackedMonths.size,
                monthsPaid = monthsPaid,
                monthsPending = monthsPending,
                completionRate = rate
            )
        }.sortedByDescending { it.totalContributed }

        val monthBreakdowns = trackedMonths.map { month ->
            val monthContribs = contributions.filter { it.monthYear == month }
            val monthPaid = monthContribs.filter { it.status == Contribution.STATUS_PAID }
            val monthCollected = monthPaid.sumOf { it.amount }
            val monthTarget = activeMembers.size * monthlyAmount
            val monthPending = maxOf(0.0, monthTarget - monthCollected)
            val paidCount = monthPaid.size
            val pendingCount = maxOf(0, activeMembers.size - paidCount)
            val rate = if (monthTarget > 0) ((monthCollected / monthTarget) * 100).toFloat() else 0f

            MonthSummaryItem(
                monthYear = month,
                monthDisplay = formatMonthDisplay(month),
                totalCollected = monthCollected,
                totalPending = monthPending,
                targetAmount = monthTarget,
                paidCount = paidCount,
                pendingCount = pendingCount,
                collectionRate = rate
            )
        }

        OverallSummary(
            totalCollected = totalCollected,
            totalPending = totalPending,
            totalExpected = totalExpected,
            collectionRate = collectionRate,
            totalPaidTransactions = paidTransactions,
            totalPendingInstances = maxOf(0, (trackedMonths.size * activeMembers.size) - paidTransactions),
            totalCashPendingInstances = cashPendingCount,
            upiCollected = upiCollected,
            cashCollected = cashCollected,
            trackedMonths = trackedMonths,
            memberSummaries = memberSummaries,
            monthlyBreakdowns = monthBreakdowns
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverallSummary()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val rawMonthlyContributions: StateFlow<List<MemberWithContribution>> =
        _selectedMonthYear.flatMapLatest { month ->
            repository.getMembersWithContributionForMonth(month)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredMonthlyContributions: StateFlow<List<MemberWithContribution>> =
        combine(rawMonthlyContributions, _searchQuery, _statusFilter) { list, query, filter ->
            list.filter { item ->
                val matchesQuery = query.isBlank() ||
                        item.member.name.contains(query, ignoreCase = true) ||
                        item.member.phone.contains(query)

                val matchesFilter = when (filter) {
                    "PAID" -> item.isPaid
                    "PENDING" -> item.displayStatus == Contribution.STATUS_PENDING
                    "CASH_PENDING" -> item.isCashPending
                    "OVERDUE" -> item.isOverdue
                    else -> true
                }

                matchesQuery && matchesFilter
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val availableMonths: List<String> = generateAvailableMonths()

    fun selectMonth(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    fun selectMember(memberId: Long) {
        _selectedMemberId.value = memberId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun setDashboardTab(tab: String) {
        _dashboardTab.value = tab
    }

    fun authenticateAdmin(pin: String): Boolean {
        val currentPin = appConfig.value.adminPin
        return if (pin == currentPin) {
            _isAdminMode.value = true
            true
        } else {
            false
        }
    }

    fun exitAdminMode() {
        _isAdminMode.value = false
    }

    fun toggleAdminMode() {
        _isAdminMode.value = !_isAdminMode.value
    }

    fun recordUpiPayment(memberId: Long, amount: Double, utr: String) {
        viewModelScope.launch {
            repository.recordUpiPayment(
                memberId = memberId,
                monthYear = _selectedMonthYear.value,
                amount = amount,
                utrNumber = utr,
                isAutoApproved = true
            )
        }
    }

    fun submitCashPayment(memberId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            repository.submitCashPayment(
                memberId = memberId,
                monthYear = _selectedMonthYear.value,
                amount = amount,
                note = note
            )
        }
    }

    fun verifyCashPayment(
        context: Context,
        contributionId: Long,
        memberName: String,
        amount: Double,
        approved: Boolean
    ) {
        if (!_isAdminMode.value) return
        viewModelScope.launch {
            repository.verifyCashPayment(contributionId, approved)
            NotificationHelper.sendCashVerificationNotification(
                context = context,
                memberName = memberName,
                amount = amount,
                isApproved = approved
            )
        }
    }

    fun adminDirectRecord(
        memberId: Long,
        amount: Double,
        method: String,
        ref: String,
        remarks: String
    ) {
        if (!_isAdminMode.value) return
        viewModelScope.launch {
            repository.adminRecordPaymentDirect(
                memberId = memberId,
                monthYear = _selectedMonthYear.value,
                amount = amount,
                method = method,
                ref = ref,
                remarks = remarks
            )
        }
    }

    fun addMember(member: Member) {
        if (!_isAdminMode.value) return
        viewModelScope.launch {
            repository.addMember(member)
        }
    }

    fun updateMember(member: Member) {
        if (!_isAdminMode.value) return
        viewModelScope.launch {
            repository.updateMember(member)
        }
    }

    fun deleteMember(member: Member) {
        if (!_isAdminMode.value) return
        viewModelScope.launch {
            repository.deleteMember(member)
        }
    }

    fun updateConfig(config: AppConfig) {
        if (!_isAdminMode.value) return
        viewModelScope.launch {
            repository.updateConfig(config)
        }
    }

    fun sendMissedReminderNotifications(context: Context): Int {
        if (!_isAdminMode.value) return 0
        val currentList = rawMonthlyContributions.value
        val pendingOrOverdue = currentList.filter {
            it.displayStatus == Contribution.STATUS_PENDING || it.isOverdue
        }
        val monthYear = _selectedMonthYear.value

        var count = 0
        for ((index, item) in pendingOrOverdue.withIndex()) {
            NotificationHelper.sendContributionReminderNotification(
                context = context,
                memberName = item.member.name,
                monthYear = monthYear,
                amount = appConfig.value.monthlyAmount,
                notificationId = NotificationHelper.NOTIFICATION_ID_BASE + index
            )
            count++
        }

        viewModelScope.launch {
            repository.markMissedContributionsOverdue(monthYear)
            repository.logNotificationBroadcast(count, monthYear)
        }
        return count
    }

    fun buildAuditLogsExportText(): String {
        val logs = auditLogs.value
        val config = appConfig.value
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        val sb = StringBuilder()
        sb.append("📋 *${config.fundTitle} - Activity & Financial Audit Trail*\n")
        sb.append("Administrator: ${config.adminName} (${config.adminEmail})\n")
        sb.append("Total Saved Log Entries: ${logs.size}\n")
        sb.append("Exported: ${sdf.format(Date())}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        logs.forEachIndexed { i, log ->
            val dateStr = sdf.format(Date(log.timestamp))
            sb.append("${i + 1}. [${log.action}] by ${log.performedByRole} • $dateStr\n")
            sb.append("   ${log.description}\n\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("All transactions and activities are securely saved in local database.")
        return sb.toString()
    }

    fun buildOverallShareReportText(): String {
        val summary = overallSummary.value
        val config = appConfig.value
        val sb = StringBuilder()
        sb.append("📊 *${config.fundTitle} - Overall Summary (All-Time)*\n")
        sb.append("🗓 Tracked Months (${summary.trackedMonths.size}): ${summary.trackedMonths.joinToString(", ") { formatMonthDisplay(it) }}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("💰 *OVERALL FINANCIAL STATUS*\n")
        sb.append("• Total Collected: ₹${summary.totalCollected.toInt()} (${summary.collectionRate.toInt()}%)\n")
        sb.append("• Total Pending Dues: ₹${summary.totalPending.toInt()}\n")
        sb.append("• Total Lifetime Target: ₹${summary.totalExpected.toInt()}\n")
        sb.append("• Modes Split: ₹${summary.upiCollected.toInt()} via UPI | ₹${summary.cashCollected.toInt()} via Cash\n")
        sb.append("• Completed Payments: ${summary.totalPaidTransactions} | Pending Instances: ${summary.totalPendingInstances}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        sb.append("👥 *MEMBER ALL-TIME CONTRIBUTIONS & DUES:*\n")
        summary.memberSummaries.forEachIndexed { i, m ->
            val statusTag = if (m.totalPending <= 0) "✅ 100% Cleared" else "⏳ ₹${m.totalPending.toInt()} Due (${m.monthsPending} mo)"
            sb.append("${i + 1}. ${m.member.name}: Paid ₹${m.totalContributed.toInt()} / ₹${(m.totalContributed + m.totalPending).toInt()} • $statusTag\n")
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("UPI ID for Contributions: ${config.upiId}\n")
        sb.append("Generated for complete transparency among all members.")
        return sb.toString()
    }

    companion object {
        fun getCurrentMonthYear(): String {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            return sdf.format(Calendar.getInstance().time)
        }

        fun generateAvailableMonths(): List<String> {
            val months = mutableListOf<String>()
            val cal = Calendar.getInstance()
            // Add next month, current month, and past 5 months
            cal.add(Calendar.MONTH, 1)
            for (i in 0 until 7) {
                val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
                months.add(sdf.format(cal.time))
                cal.add(Calendar.MONTH, -1)
            }
            return months
        }

        fun formatMonthDisplay(monthYear: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM", Locale.US)
                val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
                val date = inputFormat.parse(monthYear)
                if (date != null) outputFormat.format(date) else monthYear
            } catch (_: Exception) {
                monthYear
            }
        }
    }
}
