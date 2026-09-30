package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppConfig
import com.example.data.model.Contribution
import com.example.data.model.Member
import com.example.data.model.MemberWithContribution
import com.example.ui.WelfareViewModel
import com.example.ui.components.AdBannerCard
import com.example.ui.components.CompactAdBanner
import com.example.ui.components.InFeedAdCard
import com.example.ui.components.QrCodeCanvas
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.theme.AmberPending
import com.example.ui.theme.AmberPendingContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
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
    val clipboardManager = LocalClipboardManager.current

    // Observe active member for Member view
    val allMembers by viewModel.allMembers.collectAsStateWithLifecycle()
    val currentMember by viewModel.currentDeviceMember.collectAsStateWithLifecycle()
    val memberMonthlyContribution by viewModel.memberMonthlyContribution.collectAsStateWithLifecycle()
    val memberOverallSummary by viewModel.memberOverallSummary.collectAsStateWithLifecycle()
    val pendingMonthsList by viewModel.memberPendingMonths.collectAsStateWithLifecycle()

    var showMemberSelectorDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showCashDialogForMember by remember { mutableStateOf(false) }
    var showSetMonthlyAmountDialog by remember { mutableStateOf(false) }
    var newMonthlyAmountInput by remember(config.monthlyAmount) { mutableStateOf(if (config.monthlyAmount > 0) config.monthlyAmount.toInt().toString() else "") }
    var customQrAmount by remember(config.monthlyAmount) { mutableStateOf(if (config.monthlyAmount > 0) config.monthlyAmount.toInt().toString() else "") }
    var customCashAmount by remember(config.monthlyAmount) { mutableStateOf(if (config.monthlyAmount > 0) config.monthlyAmount.toInt().toString() else "") }
    var cashNote by remember { mutableStateOf("") }
    var selectedItemForPayment by remember { mutableStateOf<MemberWithContribution?>(null) }
    var memberSearchFilter by remember { mutableStateOf("") }

    // Month navigation calculations
    val availableMonths = viewModel.availableMonths
    val currentMonthIndex = availableMonths.indexOf(selectedMonth)
    val hasPrevMonth = currentMonthIndex != -1 && currentMonthIndex < availableMonths.size - 1
    val hasNextMonth = currentMonthIndex > 0

    // Stats for Admin
    val totalCount = contributions.size
    val paidCount = contributions.count { it.isPaid }
    val pendingCount = contributions.count { it.displayStatus == Contribution.STATUS_PENDING || it.isOverdue }
    val cashPendingList = contributions.filter { it.isCashPending }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // -------------------------------------------------------------
        // 1. MONTH SELECTOR (Monthly Wise Navigation)
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("month_selector_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (hasPrevMonth) {
                                viewModel.selectMonth(availableMonths[currentMonthIndex + 1])
                            }
                        },
                        enabled = hasPrevMonth
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = WelfareViewModel.formatMonthDisplay(selectedMonth),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            if (hasNextMonth) {
                                viewModel.selectMonth(availableMonths[currentMonthIndex - 1])
                            }
                        },
                        enabled = hasNextMonth
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }
        }

        // =============================================================
        // 2. MEMBER VIEW (When in Member Mode)
        // User request: "Member can only see monthly payment status, overall status and pending."
        // =============================================================
        if (!isAdminMode) {
            // Identity Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMemberSelectorDialog = true }
                        .testTag("member_profile_chip"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Member Profile",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currentMember?.name ?: "Select Your Name",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        TextButton(onClick = { showMemberSelectorDialog = true }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch", fontSize = 12.sp)
                        }
                    }
                }
            }

            // A. Monthly Payment Status (Monthly Wise)
            item {
                val isPaid = memberMonthlyContribution?.isPaid == true
                val isCashPending = memberMonthlyContribution?.isCashPending == true

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("monthly_payment_status_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = when {
                            isPaid -> EmeraldSuccessContainer.copy(alpha = 0.5f)
                            isCashPending -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            else -> AmberPendingContainer.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Payment Status",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = WelfareViewModel.formatMonthDisplay(selectedMonth),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isPaid -> EmeraldSuccess
                                            isCashPending -> MaterialTheme.colorScheme.secondary
                                            else -> AmberPending
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = when {
                                        isPaid -> "PAID"
                                        isCashPending -> "VERIFYING CASH"
                                        else -> "PENDING"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (isPaid) {
                            val c = memberMonthlyContribution
                            val paidAmt = c?.amount ?: config.monthlyAmount
                            val paidStr = if (paidAmt > 0) "₹${paidAmt.toInt()}" else "Contribution"
                            Text(
                                text = "$paidStr contributed via ${c?.paymentMethod ?: "UPI"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldSuccess
                            )
                            if (!c?.transactionRef.isNullOrBlank()) {
                                Text(
                                    text = "Ref/UTR: ${c?.transactionRef}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (isCashPending) {
                            val c = memberMonthlyContribution
                            val claimAmt = c?.amount ?: config.monthlyAmount
                            val claimStr = if (claimAmt > 0) " of ₹${claimAmt.toInt()}" else ""
                            Text(
                                text = "Cash payment$claimStr submitted. Waiting for Admin approval.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val dueAmt = config.monthlyAmount
                            Text(
                                text = if (dueAmt > 0) "Contribution of ₹${dueAmt.toInt()} is pending for this month." else "Contribution is pending for this month.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Payment Options Monthly Wise
                            Text(
                                text = "Payment Options",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val upiUri = UpiHelper.buildUpiUri(
                                            upiId = config.upiId,
                                            name = config.upiName,
                                            amount = config.monthlyAmount,
                                            note = "Welfare $selectedMonth - ${currentMember?.name ?: ""}"
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                        try {
                                            context.startActivity(Intent.createChooser(intent, "Pay via UPI App"))
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "No UPI app found. Please use QR code or copy UPI ID.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pay_upi_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (config.monthlyAmount > 0) "Pay ₹${config.monthlyAmount.toInt()}" else "Pay Online", fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = { showQrDialog = true },
                                    modifier = Modifier.testTag("show_qr_btn")
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("QR", fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = { showCashDialogForMember = true },
                                    modifier = Modifier.testTag("pay_cash_btn")
                                ) {
                                    Icon(Icons.Default.Money, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cash", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Sponsored Ad Banner right below the Current Month Status & Payment card
            item {
                AdBannerCard(
                    campaignIndex = 1,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // B. Overall Status
            item {
                val summary = memberOverallSummary
                val totalContributed = summary?.totalContributed ?: 0.0
                val totalPending = summary?.totalPending ?: 0.0
                val monthsPaid = summary?.monthsPaid ?: 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overall_status_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Overall Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Contributed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${totalContributed.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                            }
                            Column {
                                Text("Months Cleared", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$monthsPaid Months", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column {
                                Text("Overall Pending", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (totalPending <= 0) "Cleared" else "₹${totalPending.toInt()}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalPending <= 0) EmeraldSuccess else RoseOverdue
                                )
                            }
                        }
                    }
                }
            }

            // C. Pending Dues (List of all pending months)
            item {
                val pendingMonths = pendingMonthsList

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_dues_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pending Contributions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (pendingMonths.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(RoseOverdue.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${pendingMonths.size} Pending",
                                        color = RoseOverdue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        if (pendingMonths.isEmpty()) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "All contributions are up to date! Thank you.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            for (month in pendingMonths) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = WelfareViewModel.formatMonthDisplay(month),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (config.monthlyAmount > 0) "Due: ₹${config.monthlyAmount.toInt()}" else "Due",
                                            fontSize = 11.sp,
                                            color = RoseOverdue
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.selectMonth(month)
                                            val upiUri = UpiHelper.buildUpiUri(
                                                upiId = config.upiId,
                                                name = config.upiName,
                                                amount = config.monthlyAmount,
                                                note = "Welfare $month - ${currentMember?.name ?: ""}"
                                            )
                                            val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                            try {
                                                context.startActivity(Intent.createChooser(intent, "Pay via UPI App"))
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "Please copy UPI ID: ${config.upiId}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Text(if (config.monthlyAmount > 0) "Pay ₹${config.monthlyAmount.toInt()}" else "Pay", fontSize = 12.sp)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }

            // Sponsored in-feed ad below pending list
            item {
                InFeedAdCard(
                    campaignIndex = 2,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // D. Members Contribution Monthly-wise (Transparency for group)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Members (${WelfareViewModel.formatMonthDisplay(selectedMonth)})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$paidCount / $totalCount Paid",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            items(contributions, key = { it.member.id }) { item ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.member.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            val rowAmt = item.amount ?: config.monthlyAmount
                            val rowAmtStr = if (rowAmt > 0) "₹${rowAmt.toInt()}" else ""
                            Text(
                                text = if (item.isPaid) "Paid${if (rowAmtStr.isNotBlank()) " $rowAmtStr" else ""} via ${item.paymentMethod ?: "UPI"}" else if (rowAmtStr.isNotBlank()) "Due $rowAmtStr" else "Due",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        item.isPaid -> EmeraldSuccessContainer
                                        item.isCashPending -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> AmberPendingContainer
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when {
                                    item.isPaid -> "✅ Paid"
                                    item.isCashPending -> "⏳ Verifying"
                                    else -> "⏳ Pending"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    item.isPaid -> EmeraldSuccess
                                    item.isCashPending -> MaterialTheme.colorScheme.secondary
                                    else -> AmberPending
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Ad in Member view
            item {
                AdBannerCard(
                    campaignIndex = 0,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // =============================================================
        // 3. ADMIN VIEW (When in Admin Mode)
        // User request: "DASHBOARD to see members contribution and payment option monthly wise."
        // =============================================================
        if (isAdminMode) {
            // Optional Setup Banner if monthly amount is not configured yet
            if (config.monthlyAmount <= 0.0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_monthly_amount_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Set Monthly Contribution Amount",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Configure default contribution amount for members (e.g. ₹500, ₹1000).",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { showSetMonthlyAmountDialog = true }) {
                                Text("Set Amount", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Admin Summary Overview
            item {
                val totalCollectedCalc = contributions.filter { it.isPaid }.sumOf { it.amount ?: config.monthlyAmount }
                val totalPendingCalc = contributions.filter { !it.isPaid }.sumOf { it.amount ?: config.monthlyAmount }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_summary_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Collection Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("ADMIN VIEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Collected", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${totalCollectedCalc.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                            }
                            Column {
                                Text("Total Pending", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${totalPendingCalc.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RoseOverdue)
                            }
                            Column {
                                Text("Paid Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$paidCount / $totalCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        LinearProgressIndicator(
                            progress = { if (totalCount > 0) paidCount.toFloat() / totalCount else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Sponsored Ad Banner right below Admin Summary
            item {
                AdBannerCard(
                    campaignIndex = 0,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Cash Verification Alert (if any pending)
            if (cashPendingList.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cash_verification_alert_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberPendingContainer.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberPending, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${cashPendingList.size} Cash Payments Pending Verification",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            cashPendingList.forEach { pendingItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(pendingItem.member.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        val claimAmt = pendingItem.amount ?: config.monthlyAmount
                                        val claimAmtStr = if (claimAmt > 0) "₹${claimAmt.toInt()} " else ""
                                        Text("${claimAmtStr}(Cash claim)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = {
                                                pendingItem.contributionId?.let { cid ->
                                                    viewModel.verifyCashPayment(
                                                        context = context,
                                                        contributionId = cid,
                                                        memberName = pendingItem.member.name,
                                                        amount = pendingItem.amount ?: config.monthlyAmount,
                                                        approved = true
                                                    )
                                                    Toast.makeText(context, "Approved cash payment for ${pendingItem.member.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Approve", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                pendingItem.contributionId?.let { cid ->
                                                    viewModel.verifyCashPayment(
                                                        context = context,
                                                        contributionId = cid,
                                                        memberName = pendingItem.member.name,
                                                        amount = pendingItem.amount ?: config.monthlyAmount,
                                                        approved = false
                                                    )
                                                    Toast.makeText(context, "Rejected cash claim", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Reject", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search Filter for Admin
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Members Contributions (${contributions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = onNavigateToMembers) {
                        Text("+ Add Member", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = memberSearchFilter,
                    onValueChange = { memberSearchFilter = it },
                    placeholder = { Text("Search member by name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (memberSearchFilter.isNotBlank()) {
                            IconButton(onClick = { memberSearchFilter = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_search_member_input")
                )
            }

            // List of members with Admin action
            val filteredList = contributions.filter {
                memberSearchFilter.isBlank() || it.member.name.contains(memberSearchFilter, ignoreCase = true)
            }

            if (filteredList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("No Members Found", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = if (contributions.isEmpty()) "Tap '+ Add Member' above to register members with their name." else "No members match '$memberSearchFilter'.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(filteredList, key = { _, item -> item.member.id }) { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_member_row_${item.member.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.member.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when {
                                                item.isPaid -> EmeraldSuccessContainer
                                                item.isCashPending -> MaterialTheme.colorScheme.secondaryContainer
                                                else -> AmberPendingContainer
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when {
                                            item.isPaid -> "PAID"
                                            item.isCashPending -> "VERIFY CASH"
                                            else -> "PENDING"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            item.isPaid -> EmeraldSuccess
                                            item.isCashPending -> MaterialTheme.colorScheme.secondary
                                            else -> AmberPending
                                        }
                                    )
                                }
                            }

                            val itemAmount = item.amount ?: config.monthlyAmount
                            val itemAmountStr = if (itemAmount > 0) "₹${itemAmount.toInt()}" else ""
                            if (item.isPaid) {
                                Text(
                                    text = "${if (itemAmountStr.isNotBlank()) "$itemAmountStr • " else ""}${item.paymentMethod ?: "UPI"}",
                                    fontSize = 12.sp,
                                    color = EmeraldSuccess
                                )
                                if (!item.transactionRef.isNullOrBlank()) {
                                    Text(
                                        text = "Ref: ${item.transactionRef}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = if (itemAmountStr.isNotBlank()) "Due: $itemAmountStr" else "Due",
                                    fontSize = 12.sp,
                                    color = RoseOverdue
                                )
                            }
                        }

                        // Admin Action Button
                        if (!item.isPaid) {
                            Button(
                                onClick = { selectedItemForPayment = item },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Record", fontSize = 12.sp)
                            }
                        } else {
                            IconButton(onClick = { selectedItemForPayment = item }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "View Details", tint = EmeraldSuccess)
                            }
                        }
                    }
                }
                    // In-feed ad unit after 3rd member and periodically in list
                    if (index == 2 || (index > 2 && (index - 2) % 4 == 0)) {
                        InFeedAdCard(
                            campaignIndex = (index + 1) % 3,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Bottom Ad in Admin view
            item {
                AdBannerCard(
                    campaignIndex = 2,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // -------------------------------------------------------------
    // DIALOGS & SHEETS
    // -------------------------------------------------------------

    // 1. Member Selector Dialog
    if (showMemberSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showMemberSelectorDialog = false },
            title = {
                Text("Select Your Name (Member)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Choose your name so this phone displays your personal contribution status and receives only your notifications:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(allMembers) { member ->
                            val isSelected = currentMember?.id == member.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable {
                                        viewModel.setDeviceMember(member)
                                        showMemberSelectorDialog = false
                                        Toast.makeText(context, "Active member set to ${member.name}", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = member.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMemberSelectorDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 2. QR Code Dialog for Member Payment
    if (showQrDialog) {
        val parsedQrAmount = customQrAmount.toDoubleOrNull() ?: if (config.monthlyAmount > 0) config.monthlyAmount else 0.0
        val upiUri = UpiHelper.buildUpiUri(
            upiId = config.upiId,
            name = config.upiName,
            amount = parsedQrAmount,
            note = "Welfare $selectedMonth - ${currentMember?.name ?: ""}"
        )

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Text(
                    text = if (parsedQrAmount > 0) "Scan to Pay ₹${parsedQrAmount.toInt()}" else "Scan & Pay via UPI",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            QrCodeCanvas(content = upiUri.toString(), size = 170.dp)
                        }
                    }

                    if (config.upiId.isNotBlank()) {
                        Text(
                            text = "UPI ID: ${config.upiId}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(config.upiId))
                                Toast.makeText(context, "UPI ID copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy UPI ID")
                        }
                    } else {
                        Text(
                            text = "UPI ID not yet configured. Admin can set it in Settings.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    CompactAdBanner(
                        campaignIndex = 2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showQrDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // 3. Member Cash Payment Claim Dialog
    if (showCashDialogForMember) {
        AlertDialog(
            onDismissRequest = { showCashDialogForMember = false },
            title = {
                Text("Submit Cash Contribution", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter the amount handed over to Admin for $selectedMonth:",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = customCashAmount,
                        onValueChange = { customCashAmount = it },
                        label = { Text("Amount Paid (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cashNote,
                        onValueChange = { cashNote = it },
                        label = { Text("Note to Admin (Optional)") },
                        placeholder = { Text("e.g. Paid in cash directly to admin") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    CompactAdBanner(
                        campaignIndex = 1,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val member = currentMember
                        val enteredAmount = customCashAmount.toDoubleOrNull() ?: config.monthlyAmount
                        if (member != null) {
                            viewModel.submitCashPayment(
                                memberId = member.id,
                                amount = enteredAmount,
                                note = cashNote.trim()
                            )
                            showCashDialogForMember = false
                            cashNote = ""
                            Toast.makeText(context, "Cash contribution of ₹${enteredAmount.toInt()} submitted for Admin verification!", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Submit for Verification")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCashDialogForMember = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for Admin to set monthly amount directly from dashboard
    if (showSetMonthlyAmountDialog) {
        AlertDialog(
            onDismissRequest = { showSetMonthlyAmountDialog = false },
            title = {
                Text("Set Monthly Contribution Amount", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter the standard monthly contribution per member for the fund:",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = newMonthlyAmountInput,
                        onValueChange = { newMonthlyAmountInput = it },
                        label = { Text("Monthly Amount (₹)") },
                        placeholder = { Text("e.g. 500, 1000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    CompactAdBanner(
                        campaignIndex = 0,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = newMonthlyAmountInput.toDoubleOrNull() ?: 0.0
                        viewModel.updateConfig(config.copy(monthlyAmount = amt))
                        showSetMonthlyAmountDialog = false
                        Toast.makeText(context, "Monthly contribution set to ₹${amt.toInt()}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Amount")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSetMonthlyAmountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Record Payment Dialog (Admin recording payment)
    selectedItemForPayment?.let { item ->
        RecordPaymentDialog(
            item = item,
            monthYear = selectedMonth,
            isAdminMode = isAdminMode,
            defaultAmount = config.monthlyAmount,
            onDismiss = { selectedItemForPayment = null },
            onSubmitUpi = { memberId, amount, utr ->
                viewModel.recordUpiPayment(memberId, amount, utr)
                selectedItemForPayment = null
                Toast.makeText(context, "Payment recorded for ${item.member.name}!", Toast.LENGTH_SHORT).show()
            },
            onSubmitCash = { memberId, amount, note ->
                viewModel.submitCashPayment(memberId, amount, note)
                selectedItemForPayment = null
                Toast.makeText(context, "Cash payment claim submitted!", Toast.LENGTH_SHORT).show()
            },
            onAdminDirectRecord = { memberId, amount, method, ref, remarks ->
                viewModel.adminDirectRecord(memberId, amount, method, ref, remarks)
                selectedItemForPayment = null
                Toast.makeText(context, "Contribution confirmed for ${item.member.name}!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
