package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import com.example.data.remote.CloudSyncState
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppConfig
import com.example.data.model.Contribution
import com.example.data.model.MemberOverallSummary
import com.example.data.model.MemberWithContribution
import com.example.data.model.MonthSummaryItem
import com.example.data.model.OverallSummary
import com.example.ui.WelfareViewModel
import com.example.ui.components.AdBannerCard
import com.example.ui.components.QrCodeCanvas
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.theme.AmberOnPendingContainer
import com.example.ui.theme.AmberPending
import com.example.ui.theme.AmberPendingContainer
import com.example.ui.theme.EmeraldOnSuccessContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
import com.example.ui.theme.RoseOnOverdueContainer
import com.example.ui.theme.RoseOverdue
import com.example.ui.theme.RoseOverdueContainer
import com.example.util.UpiHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: WelfareViewModel,
    selectedMonth: String,
    isAdminMode: Boolean,
    config: AppConfig,
    contributions: List<MemberWithContribution>,
    cashPendingCount: Int,
    searchQuery: String,
    statusFilter: String,
    onNavigateToCashVerification: () -> Unit,
    onNavigateToMembers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showQrBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForPayment by remember { mutableStateOf<MemberWithContribution?>(null) }

    val overallSummary by viewModel.overallSummary.collectAsStateWithLifecycle()
    val dashboardTab by viewModel.dashboardTab.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    // Aggregate statistics
    val totalMembers = contributions.size
    val paidMembers = contributions.count { it.isPaid }
    val cashPendingMembers = contributions.count { it.isCashPending }
    val overdueMembers = contributions.count { it.isOverdue }
    val pendingMembers = contributions.count { it.displayStatus == Contribution.STATUS_PENDING }

    val monthlyTarget = totalMembers * config.monthlyAmount
    val totalCollected = paidMembers * config.monthlyAmount
    val totalPending = (totalMembers - paidMembers) * config.monthlyAmount
    val progressPercent = if (monthlyTarget > 0) (totalCollected / monthlyTarget).toFloat() else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tab Mode Selector: Overall Summary (All-Time) vs Monthly View
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.setDashboardTab("OVERALL") }
                            .testTag("tab_overall_summary"),
                        color = if (dashboardTab == "OVERALL") MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (dashboardTab == "OVERALL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Overall Summary",
                                fontSize = 13.sp,
                                fontWeight = if (dashboardTab == "OVERALL") FontWeight.Bold else FontWeight.Medium,
                                color = if (dashboardTab == "OVERALL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.setDashboardTab("MONTHLY") }
                            .testTag("tab_monthly_summary"),
                        color = if (dashboardTab == "MONTHLY") MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (dashboardTab == "MONTHLY") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Monthly View",
                                fontSize = 13.sp,
                                fontWeight = if (dashboardTab == "MONTHLY") FontWeight.Bold else FontWeight.Medium,
                                color = if (dashboardTab == "MONTHLY") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Centralized Cloud Database Real-Time Status Pill
        item {
            val isConnected = syncState == CloudSyncState.CONNECTED || syncState == CloudSyncState.SYNCED
            val isSyncing = syncState == CloudSyncState.SYNCING
            val isOffline = syncState == CloudSyncState.OFFLINE_CACHE

            val pillBg = when {
                isConnected -> Color(0xFF064E3B).copy(alpha = 0.12f)
                isSyncing -> Color(0xFF78350F).copy(alpha = 0.12f)
                isOffline -> Color(0xFF1E3A8A).copy(alpha = 0.12f)
                else -> Color(0xFF4C1D95).copy(alpha = 0.12f)
            }
            val pillTextColor = when {
                isConnected -> Color(0xFF059669)
                isSyncing -> Color(0xFFD97706)
                isOffline -> Color(0xFF2563EB)
                else -> Color(0xFF7C3AED)
            }
            val pillText = when {
                isConnected -> "🟢 Centralized Database: Firebase Firestore Active (Real-Time)"
                isSyncing -> "🔄 Syncing with Central Cloud Database..."
                isOffline -> "⚡ Central Database: Offline Cache Active (Local Room SQLite)"
                else -> "⚙️ Central Database: Ready for Firebase Credentials"
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        viewModel.syncAllToCloud { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .testTag("dashboard_cloud_status_pill"),
                color = pillBg,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = pillTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pillText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pillTextColor
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Sync Now",
                        tint = pillTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (dashboardTab == "OVERALL") {
            // 1. Overall Hero Financial Card
            item {
                OverallFinancialOverviewCard(
                    overallSummary = overallSummary,
                    config = config,
                    onShareReport = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "${config.fundTitle} - Overall Summary")
                            putExtra(Intent.EXTRA_TEXT, viewModel.buildOverallShareReportText())
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Overall Summary via"))
                    },
                    onQuickPay = { showQrBottomSheet = true }
                )
            }

            // 2. Quick Action Banner
            item {
                QuickActionBanner(
                    isAdminMode = isAdminMode,
                    config = config,
                    selectedMonth = selectedMonth,
                    cashPendingCount = cashPendingCount,
                    onShowQr = { showQrBottomSheet = true },
                    onSendReminders = {
                        val count = viewModel.sendMissedReminderNotifications(context)
                        Toast.makeText(
                            context,
                            "Push notifications dispatched to $count members with pending dues.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onViewCashApprovals = onNavigateToCashVerification
                )
            }

            // 3. Native Ad Banner
            item {
                AdBannerCard(campaignIndex = 0)
            }

            // 4. Monthly Trend Comparison Header & Items
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Breakdown Trends (${overallSummary.monthlyBreakdowns.size} Months)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(overallSummary.monthlyBreakdowns, key = { it.monthYear }) { monthItem ->
                MonthSummaryRowCard(
                    monthItem = monthItem,
                    onViewMonth = {
                        viewModel.selectMonth(monthItem.monthYear)
                        viewModel.setDashboardTab("MONTHLY")
                    }
                )
            }

            // 5. Member Lifetime Contribution Ledger Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Member All-Time Ledger (${overallSummary.memberSummaries.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isAdminMode) {
                        FilledTonalButton(
                            onClick = onNavigateToMembers,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("manage_members_btn")
                        ) {
                            Text("Manage", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 6. Member Lifetime Contribution Items
            items(overallSummary.memberSummaries, key = { it.member.id }) { item ->
                MemberOverallSummaryCard(
                    summary = item,
                    monthlyAmount = config.monthlyAmount,
                    onPay = {
                        UpiHelper.launchUpiPayment(
                            context = context,
                            upiId = config.upiId,
                            name = config.upiName,
                            amount = item.totalPending.coerceAtLeast(config.monthlyAmount),
                            note = "MAYAN Welfare Contribution ${item.member.name}"
                        )
                    }
                )
            }
        } else {
            // MONTHLY VIEW
            // Overall summary ribbon banner for quick awareness
            item {
                OverallSummaryBanner(
                    totalCollected = overallSummary.totalCollected,
                    totalPending = overallSummary.totalPending,
                    onSwitchToOverall = { viewModel.setDashboardTab("OVERALL") }
                )
            }

            // Month Selector Bar
            item {
                MonthSelectorCard(
                    currentMonth = selectedMonth,
                    availableMonths = viewModel.availableMonths,
                    onMonthSelected = { viewModel.selectMonth(it) }
                )
            }

            // Financial Overview Card for the Selected Month
            item {
                FinancialOverviewCard(
                    monthName = WelfareViewModel.formatMonthDisplay(selectedMonth),
                    totalCollected = totalCollected,
                    totalPending = totalPending,
                    targetAmount = monthlyTarget,
                    progressPercent = progressPercent,
                    paidCount = paidMembers,
                    totalCount = totalMembers
                )
            }

            // Quick Pay & Action Banner
            item {
                QuickActionBanner(
                    isAdminMode = isAdminMode,
                    config = config,
                    selectedMonth = selectedMonth,
                    cashPendingCount = cashPendingCount,
                    onShowQr = { showQrBottomSheet = true },
                    onSendReminders = {
                        val count = viewModel.sendMissedReminderNotifications(context)
                        Toast.makeText(
                            context,
                            "Push notifications dispatched to $count members with pending dues.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onViewCashApprovals = onNavigateToCashVerification
                )
            }

            // Native Ad Banner
            item {
                AdBannerCard(campaignIndex = 0)
            }

            // Filter & Search Controls
            item {
                FilterAndSearchSection(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    statusFilter = statusFilter,
                    onFilterChange = { viewModel.setStatusFilter(it) },
                    paidCount = paidMembers,
                    pendingCount = pendingMembers + overdueMembers,
                    cashPendingCount = cashPendingMembers
                )
            }

            // Member list header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Member Contributions (${contributions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isAdminMode) {
                        FilledTonalButton(
                            onClick = onNavigateToMembers,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("manage_members_btn")
                        ) {
                            Text("Manage Members", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Member Items for Selected Month
            if (contributions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No members match the current filter")
                        }
                    }
                }
            } else {
                items(contributions, key = { it.member.id }) { item ->
                    MemberContributionCard(
                        item = item,
                        monthlyAmount = config.monthlyAmount,
                        isAdminMode = isAdminMode,
                        onPayClicked = {
                            selectedItemForPayment = item
                        },
                        onQuickUpi = {
                            UpiHelper.launchUpiPayment(
                                context = context,
                                upiId = config.upiId,
                                name = config.upiName,
                                amount = config.monthlyAmount,
                                note = "MAYAN Welfare ${item.member.name} $selectedMonth"
                            )
                        }
                    )
                }
            }
        }
    }

    // QR & Direct UPI Payment Bottom Sheet
    if (showQrBottomSheet) {
        val upiUri = UpiHelper.buildUpiUri(
            upiId = config.upiId,
            name = config.upiName,
            amount = config.monthlyAmount,
            note = "MAYAN Welfare Contribution $selectedMonth"
        ).toString()

        ModalBottomSheet(
            onDismissRequest = { showQrBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .testTag("upi_qr_bottom_sheet"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pay Contribution via UPI",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${config.fundTitle} • Month: ${WelfareViewModel.formatMonthDisplay(selectedMonth)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                QrCodeCanvas(
                    content = upiUri,
                    size = 190.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Amount: ₹${config.monthlyAmount.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable {
                        UpiHelper.copyToClipboard(context, config.upiId, "UPI ID")
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UPI: ${config.upiId}",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy UPI ID",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        UpiHelper.launchUpiPayment(
                            context = context,
                            upiId = config.upiId,
                            name = config.upiName,
                            amount = config.monthlyAmount,
                            note = "MAYAN Welfare Contribution $selectedMonth"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_upi_apps_btn")
                ) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pay ₹${config.monthlyAmount.toInt()} with UPI Apps")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showQrBottomSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Record Payment Dialog
    selectedItemForPayment?.let { item ->
        RecordPaymentDialog(
            item = item,
            monthYear = selectedMonth,
            isAdminMode = isAdminMode,
            defaultAmount = config.monthlyAmount,
            onDismiss = { selectedItemForPayment = null },
            onSubmitUpi = { memberId, amount, utr ->
                viewModel.recordUpiPayment(memberId, amount, utr)
                Toast.makeText(context, "UPI Payment recorded!", Toast.LENGTH_SHORT).show()
            },
            onSubmitCash = { memberId, amount, note ->
                viewModel.submitCashPayment(memberId, amount, note)
                Toast.makeText(context, "Cash payment submitted for Admin verification!", Toast.LENGTH_SHORT).show()
            },
            onAdminDirectRecord = { memberId, amount, method, ref, remarks ->
                viewModel.adminDirectRecord(memberId, amount, method, ref, remarks)
                Toast.makeText(context, "Payment verified & saved by Admin!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun OverallSummaryBanner(
    totalCollected: Double,
    totalPending: Double,
    onSwitchToOverall: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSwitchToOverall() },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Overall All-Time Summary",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row {
                        Text(
                            text = "Collected: ₹${totalCollected.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                        Text(
                            text = " • ",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Pending: ₹${totalPending.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "View All-Time",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OverallFinancialOverviewCard(
    overallSummary: OverallSummary,
    config: AppConfig,
    onShareReport: () -> Unit,
    onQuickPay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Overall Financial Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "All-Time Group Welfare Fund",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${overallSummary.trackedMonths.size} Months",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Two big stat cards: Total Collected vs Total Pending
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // OVERALL COLLECTED
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OVERALL COLLECTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "₹${overallSummary.totalCollected.toInt()}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${overallSummary.totalPaidTransactions} payments verified",
                            fontSize = 11.sp,
                            color = Color(0xFF6EE7B7)
                        )
                    }
                }

                // OVERALL PENDING
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OVERALL PENDING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFECDD3)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "₹${overallSummary.totalPending.toInt()}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFEE2E2)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${overallSummary.totalPendingInstances} pending dues",
                            fontSize = 11.sp,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lifetime Target & Progress
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lifetime Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${overallSummary.collectionRate.toInt()}% of ₹${overallSummary.totalExpected.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (overallSummary.collectionRate / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF334155),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Breakdown Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UPI: ₹${overallSummary.upiCollected.toInt()}",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Money,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cash: ₹${overallSummary.cashCollected.toInt()}",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShareReport,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Overall", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onQuickPay,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pay UPI QR", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MemberOverallSummaryCard(
    summary: MemberOverallSummary,
    monthlyAmount: Double,
    onPay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (summary.totalPending == 0.0) EmeraldSuccess.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = summary.member.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = if (summary.totalPending == 0.0) EmeraldSuccess else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = summary.member.name,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (summary.member.isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Admin",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Text(
                            text = summary.member.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (summary.totalPending == 0.0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(EmeraldSuccessContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "100% Cleared",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldOnSuccessContainer
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(RoseOverdueContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "₹${summary.totalPending.toInt()} Due",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseOnOverdueContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Collected vs Pending numbers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Contributed",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${summary.totalContributed.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Months Cleared",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${summary.monthsPaid} / ${summary.totalMonthsTracked}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Pending Dues",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${summary.totalPending.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.totalPending > 0) RoseOverdue else EmeraldSuccess
                    )
                }
            }

            if (summary.totalPending > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = onPay,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay Dues (₹${summary.totalPending.toInt()})", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSummaryRowCard(
    monthItem: MonthSummaryItem,
    onViewMonth: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthItem.monthDisplay,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${monthItem.collectionRate.toInt()}% Collected",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (monthItem.collectionRate / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = EmeraldSuccess,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Collected: ₹${monthItem.totalCollected.toInt()}",
                        fontSize = 12.sp,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Pending: ₹${monthItem.totalPending.toInt()}",
                        fontSize = 12.sp,
                        color = RoseOverdue,
                        fontWeight = FontWeight.Medium
                    )
                }
                TextButton(
                    onClick = onViewMonth,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("View Month", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun MonthSelectorCard(
    currentMonth: String,
    availableMonths: List<String>,
    onMonthSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val currentIndex = availableMonths.indexOf(currentMonth)
            val hasPrevious = currentIndex < availableMonths.size - 1
            val hasNext = currentIndex > 0

            IconButton(
                onClick = {
                    if (hasPrevious) onMonthSelected(availableMonths[currentIndex + 1])
                },
                enabled = hasPrevious
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = WelfareViewModel.formatMonthDisplay(currentMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Contribution Period",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    if (hasNext) onMonthSelected(availableMonths[currentIndex - 1])
                },
                enabled = hasNext
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
        }
    }
}

@Composable
private fun FinancialOverviewCard(
    monthName: String,
    totalCollected: Double,
    totalPending: Double,
    targetAmount: Double,
    progressPercent: Float,
    paidCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E3A8A)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$monthName Fund Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFDBEAFE),
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$paidCount / $totalCount Paid",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Total Collected",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF93C5FD)
                    )
                    Text(
                        text = "₹${totalCollected.toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Pending Dues",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFCA5A5)
                    )
                    Text(
                        text = "₹${totalPending.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFECACA)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progressPercent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF10B981),
                trackColor = Color(0xFF334155),
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progressPercent * 100).toInt()}% Target Reached",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFDBEAFE)
                )
                Text(
                    text = "Goal: ₹${targetAmount.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFDBEAFE)
                )
            }
        }
    }
}

@Composable
private fun QuickActionBanner(
    isAdminMode: Boolean,
    config: AppConfig,
    selectedMonth: String,
    cashPendingCount: Int,
    onShowQr: () -> Unit,
    onSendReminders: () -> Unit,
    onViewCashApprovals: () -> Unit
) {
    if (isAdminMode) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Admin Management Console",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSendReminders,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("broadcast_reminders_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Notify Dues", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onViewCashApprovals,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_cash_approvals_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (cashPendingCount > 0) "Verify ($cashPendingCount)" else "Verify Cash",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    } else {
        // General Member Quick Pay
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pay Monthly ₹${config.monthlyAmount.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Instant UPI to ${config.upiId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Button(
                    onClick = onShowQr,
                    modifier = Modifier.testTag("quick_pay_upi_btn"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pay Now", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FilterAndSearchSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    statusFilter: String,
    onFilterChange: (String) -> Unit,
    paidCount: Int,
    pendingCount: Int,
    cashPendingCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by member name or phone...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("member_search_input"),
            shape = RoundedCornerShape(12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            item {
                FilterChip(
                    selected = statusFilter == "ALL",
                    onClick = { onFilterChange("ALL") },
                    label = { Text("All") },
                    modifier = Modifier.testTag("filter_all")
                )
            }
            item {
                FilterChip(
                    selected = statusFilter == "PAID",
                    onClick = { onFilterChange("PAID") },
                    label = { Text("Paid ($paidCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldSuccessContainer,
                        selectedLabelColor = EmeraldOnSuccessContainer
                    ),
                    modifier = Modifier.testTag("filter_paid")
                )
            }
            item {
                FilterChip(
                    selected = statusFilter == "PENDING",
                    onClick = { onFilterChange("PENDING") },
                    label = { Text("Pending ($pendingCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RoseOverdueContainer,
                        selectedLabelColor = RoseOnOverdueContainer
                    ),
                    modifier = Modifier.testTag("filter_pending")
                )
            }
            if (cashPendingCount > 0) {
                item {
                    FilterChip(
                        selected = statusFilter == "CASH_PENDING",
                        onClick = { onFilterChange("CASH_PENDING") },
                        label = { Text("Cash Review ($cashPendingCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPendingContainer,
                            selectedLabelColor = AmberOnPendingContainer
                        ),
                        modifier = Modifier.testTag("filter_cash")
                    )
                }
            }
        }
    }
}

@Composable
fun MemberContributionCard(
    item: MemberWithContribution,
    monthlyAmount: Double,
    isAdminMode: Boolean,
    onPayClicked: () -> Unit,
    onQuickUpi: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_item_${item.member.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            item.isPaid -> EmeraldSuccess.copy(alpha = 0.15f)
                            item.isCashPending -> AmberPending.copy(alpha = 0.15f)
                            item.isOverdue -> RoseOverdue.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.member.name.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = when {
                        item.isPaid -> EmeraldSuccess
                        item.isCashPending -> AmberPending
                        item.isOverdue -> RoseOverdue
                        else -> MaterialTheme.colorScheme.primary
                    },
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.member.role == "ADMIN") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "ADMIN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.member.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.transactionRef?.isNotBlank() == true) {
                    Text(
                        text = "Ref: ${item.transactionRef}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            }

            // Status Badge & Action
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(
                    status = item.displayStatus,
                    verifiedByAdmin = item.verifiedByAdmin
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (item.isPaid) {
                    Text(
                        text = "₹${(item.amount ?: monthlyAmount).toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = onPayClicked,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (isAdminMode) "Record" else "Pay", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    verifiedByAdmin: Boolean
) {
    val (bgColor, textColor, label, icon) = when (status) {
        Contribution.STATUS_PAID -> Quad(
            EmeraldSuccessContainer,
            EmeraldOnSuccessContainer,
            if (verifiedByAdmin) "Paid • Verified" else "Paid",
            Icons.Default.CheckCircle
        )
        Contribution.STATUS_CASH_PENDING -> Quad(
            AmberPendingContainer,
            AmberOnPendingContainer,
            "Cash Awaiting Approval",
            Icons.Default.HourglassTop
        )
        Contribution.STATUS_OVERDUE -> Quad(
            RoseOverdueContainer,
            RoseOnOverdueContainer,
            "Overdue",
            Icons.Default.Warning
        )
        else -> Quad(
            Color(0xFFF1F5F9),
            Color(0xFF475569),
            "Pending Due",
            Icons.Default.HourglassTop
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
