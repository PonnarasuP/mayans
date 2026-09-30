package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.components.InFeedAdCard
import com.example.ui.theme.EmeraldOnSuccessContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
import com.example.ui.theme.RoseOnOverdueContainer
import com.example.ui.theme.RoseOverdue
import com.example.ui.theme.RoseOverdueContainer
import com.example.util.UpiHelper

@Composable
fun ReportsScreen(
    viewModel: WelfareViewModel,
    selectedMonth: String,
    config: AppConfig,
    contributions: List<MemberWithContribution>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val monthTitle = WelfareViewModel.formatMonthDisplay(selectedMonth)
    val overallSummary by viewModel.overallSummary.collectAsStateWithLifecycle()

    var activeReportTab by remember { mutableStateOf("OVERALL") } // "OVERALL" or "MONTHLY"

    val totalMembers = contributions.size
    val paidItems = contributions.filter { it.isPaid }
    val pendingItems = contributions.filter { !it.isPaid }

    val totalCollected = paidItems.sumOf { it.amount ?: config.monthlyAmount }
    val totalPending = pendingItems.sumOf { it.amount ?: config.monthlyAmount }
    val targetAmount = totalMembers * config.monthlyAmount
    val collectionRate = if (targetAmount > 0) ((totalCollected / targetAmount) * 100).toInt() else 0

    val upiCount = paidItems.count { it.paymentMethod == Contribution.METHOD_UPI }
    val cashCount = paidItems.count { it.paymentMethod == Contribution.METHOD_CASH }

    fun buildShareReportText(): String {
        val sb = StringBuilder()
        sb.append("📊 *${config.fundTitle} - Monthly Summary Report*\n")
        sb.append("🗓 Month: $monthTitle\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("💰 *Collection Summary*\n")
        sb.append("• Target: ₹${targetAmount.toInt()} (${totalMembers} members @ ₹${config.monthlyAmount.toInt()})\n")
        sb.append("• Collected: ₹${totalCollected.toInt()} ($collectionRate%)\n")
        sb.append("• Pending Dues: ₹${totalPending.toInt()}\n")
        sb.append("• Methods: $upiCount via UPI | $cashCount via Cash\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        sb.append("✅ *CONTRIBUTED MEMBERS (${paidItems.size}):*\n")
        if (paidItems.isEmpty()) {
            sb.append("None recorded yet.\n")
        } else {
            paidItems.forEachIndexed { i, item ->
                val methodStr = if (item.paymentMethod == Contribution.METHOD_UPI) "UPI" else "Cash"
                sb.append("${i + 1}. ${item.member.name} - ₹${(item.amount ?: config.monthlyAmount).toInt()} ($methodStr)\n")
            }
        }

        sb.append("\n⏳ *PENDING / DUE MEMBERS (${pendingItems.size}):*\n")
        if (pendingItems.isEmpty()) {
            sb.append("🎉 All members have contributed!\n")
        } else {
            pendingItems.forEachIndexed { i, item ->
                val statusStr = if (item.isCashPending) "Cash Review" else "Pending"
                sb.append("${i + 1}. ${item.member.name} - ₹${config.monthlyAmount.toInt()} ($statusStr)\n")
            }
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("UPI ID for Contributions: ${config.upiId}\n")
        sb.append("Generated for transparency among all members.")
        return sb.toString()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tab Selector: Overall Summary vs Monthly Breakdown
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
                            .clickable { activeReportTab = "OVERALL" }
                            .testTag("report_tab_overall"),
                        color = if (activeReportTab == "OVERALL") MaterialTheme.colorScheme.primary else Color.Transparent,
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
                                tint = if (activeReportTab == "OVERALL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Overall Summary",
                                fontSize = 13.sp,
                                fontWeight = if (activeReportTab == "OVERALL") FontWeight.Bold else FontWeight.Medium,
                                color = if (activeReportTab == "OVERALL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { activeReportTab = "MONTHLY" }
                            .testTag("report_tab_monthly"),
                        color = if (activeReportTab == "MONTHLY") MaterialTheme.colorScheme.primary else Color.Transparent,
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
                                tint = if (activeReportTab == "MONTHLY") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Monthly Report",
                                fontSize = 13.sp,
                                fontWeight = if (activeReportTab == "MONTHLY") FontWeight.Bold else FontWeight.Medium,
                                color = if (activeReportTab == "MONTHLY") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (activeReportTab == "OVERALL") {
            // OVERALL SUMMARY REPORT
            // 1. Overall Hero Financial Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Overall All-Time Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${overallSummary.trackedMonths.size} Months Tracked",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${overallSummary.collectionRate.toInt()}% Reached",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Row: Collected vs Pending vs Expected
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Collected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹${overallSummary.totalCollected.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldSuccess
                                )
                                Text(
                                    text = "${overallSummary.totalPaidTransactions} payments",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pending Dues",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹${overallSummary.totalPending.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RoseOverdue
                                )
                                Text(
                                    text = "${overallSummary.totalPendingInstances} pending",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lifetime Target",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹${overallSummary.totalExpected.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${overallSummary.memberSummaries.size} members",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { (overallSummary.collectionRate / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldSuccess,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "UPI: ₹${overallSummary.upiCollected.toInt()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Money,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cash: ₹${overallSummary.cashCollected.toInt()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Share Buttons for Overall Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val reportText = viewModel.buildOverallShareReportText()
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, reportText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Overall Summary Report")
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_overall_report_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Overall Report")
                            }

                            OutlinedButton(
                                onClick = {
                                    val reportText = viewModel.buildOverallShareReportText()
                                    UpiHelper.copyToClipboard(context, reportText, "Overall Summary Report")
                                },
                                modifier = Modifier.testTag("copy_overall_report_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                            }
                        }
                    }
                }
            }

            // 2. Native Ad Banner
            item {
                AdBannerCard(campaignIndex = 1)
            }

            // 3. Month-by-Month Breakdown Section
            item {
                Text(
                    text = "Month-by-Month Financial Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(overallSummary.monthlyBreakdowns, key = { it.monthYear }) { item ->
                MonthSummaryAuditCard(item = item)
            }

            // 4. Member Lifetime Transparency Ledger Header
            item {
                Text(
                    text = "Member All-Time Contribution Ledger (${overallSummary.memberSummaries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(overallSummary.memberSummaries, key = { _, item -> item.member.id }) { index, item ->
                ReportMemberOverallRow(
                    index = index + 1,
                    item = item
                )
            }
        } else {
            // MONTHLY REPORT
            // Month Picker
            item {
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
                        val currentIndex = viewModel.availableMonths.indexOf(selectedMonth)
                        val hasPrevious = currentIndex < viewModel.availableMonths.size - 1
                        val hasNext = currentIndex > 0

                        IconButton(
                            onClick = {
                                if (hasPrevious) viewModel.selectMonth(viewModel.availableMonths[currentIndex + 1])
                            },
                            enabled = hasPrevious
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Monthly Transparency Report",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                if (hasNext) viewModel.selectMonth(viewModel.availableMonths[currentIndex - 1])
                            },
                            enabled = hasNext
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }
                }
            }

            // Summary Card for Selected Month
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Monthly Audit Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$collectionRate% Reached",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Collected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹${totalCollected.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldSuccess
                                )
                                Text(
                                    text = "${paidItems.size} members",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pending Dues",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹${totalPending.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RoseOverdue
                                )
                                Text(
                                    text = "${pendingItems.size} members",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Monthly Target",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹${targetAmount.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$totalMembers members",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { (collectionRate / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldSuccess,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "UPI: $upiCount",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Money,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cash: $cashCount",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val reportText = buildShareReportText()
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, reportText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Monthly Report")
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_report_whatsapp_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Report to Group")
                            }

                            OutlinedButton(
                                onClick = {
                                    val reportText = buildShareReportText()
                                    UpiHelper.copyToClipboard(context, reportText, "Monthly Report")
                                },
                                modifier = Modifier.testTag("copy_report_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                            }
                        }
                    }
                }
            }

            // Sponsored block
            item {
                AdBannerCard(campaignIndex = 2)
            }

            // Member Breakdown Section
            item {
                Text(
                    text = "Member-by-Member Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(contributions, key = { _, item -> item.member.id }) { index, item ->
                ReportMemberRow(
                    index = index + 1,
                    item = item,
                    defaultAmount = config.monthlyAmount
                )

                if (index == 2 || (index > 2 && (index - 2) % 4 == 0)) {
                    InFeedAdCard(
                        campaignIndex = (index + 2) % 3,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthSummaryAuditCard(
    item: MonthSummaryItem
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.monthDisplay,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${item.collectionRate.toInt()}% Reached",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.collectionRate >= 80f) EmeraldSuccess else Color(0xFFD97706)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (item.collectionRate / 100f).coerceIn(0f, 1f) },
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Collected: ₹${item.totalCollected.toInt()} (${item.paidCount} paid)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldSuccess
                )
                Text(
                    text = "Pending: ₹${item.totalPending.toInt()} (${item.pendingCount} due)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RoseOverdue
                )
            }
        }
    }
}

@Composable
private fun ReportMemberOverallRow(
    index: Int,
    item: MemberOverallSummary
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index.",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.member.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.member.isAdmin) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Admin",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Text(
                    text = "Cleared ${item.monthsPaid} of ${item.totalMonthsTracked} months • ${item.member.phone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Paid: ₹${item.totalContributed.toInt()}",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess,
                    fontSize = 13.sp
                )
                Text(
                    text = if (item.totalPending == 0.0) "All Cleared" else "Due: ₹${item.totalPending.toInt()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.totalPending == 0.0) EmeraldSuccess else RoseOverdue
                )
            }
        }
    }
}

@Composable
private fun ReportMemberRow(
    index: Int,
    item: MemberWithContribution,
    defaultAmount: Double
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index.",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.member.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (item.isPaid) {
                        "Mode: ${item.paymentMethod} ${if (!item.transactionRef.isNullOrBlank()) "(${item.transactionRef})" else ""}"
                    } else if (item.isCashPending) {
                        "Cash payment submitted, pending verification"
                    } else {
                        "Pending contribution"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${(item.amount ?: defaultAmount).toInt()}",
                    fontWeight = FontWeight.Bold,
                    color = if (item.isPaid) EmeraldSuccess else RoseOverdue
                )
                Text(
                    text = if (item.isPaid) "PAID" else if (item.isCashPending) "REVIEW" else "DUE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isPaid) EmeraldSuccess else if (item.isCashPending) Color(0xFFD97706) else RoseOverdue
                )
            }
        }
    }
}
