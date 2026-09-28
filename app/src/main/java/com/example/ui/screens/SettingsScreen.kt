package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.CloudSyncState
import com.example.ui.components.AdBannerCard
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppConfig
import com.example.data.model.AuditLog
import com.example.ui.WelfareViewModel
import com.example.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: WelfareViewModel,
    config: AppConfig,
    isAdminMode: Boolean,
    auditLogs: List<AuditLog>,
    onToggleRole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // If NOT admin, show explicit Security Gate
    if (!isAdminMode) {
        AdminLockedGateScreen(
            adminName = config.adminName,
            onUnlock = onToggleRole,
            modifier = modifier
        )
        return
    }

    // Admin Authenticated View
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val isRealtimeActive by viewModel.isRealtimeActive.collectAsStateWithLifecycle()

    var isSyncingNow by remember { mutableStateOf(false) }
    var showArchitectureDialog by remember { mutableStateOf(false) }

    var upiId by remember(config) { mutableStateOf(config.upiId) }
    var upiName by remember(config) { mutableStateOf(config.upiName) }
    var monthlyAmount by remember(config) { mutableStateOf(config.monthlyAmount.toInt().toString()) }
    var contactPhone by remember(config) { mutableStateOf(config.contactPhone) }
    var adminPin by remember(config) { mutableStateOf(config.adminPin) }
    var showPin by remember { mutableStateOf(false) }
    var autoNotify by remember(config) { mutableStateOf(config.autoNotifyMissed) }
    var auditFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(auditLogs, auditFilter) {
        when (auditFilter) {
            "PAYMENTS" -> auditLogs.filter { it.action.contains("PAYMENT") || it.action.contains("UPI") }
            "CASH" -> auditLogs.filter { it.action.contains("CASH") }
            "ADMIN" -> auditLogs.filter { it.performedByRole == "ADMIN" }
            else -> auditLogs
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Admin Session Header Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Avatar",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Administrator: ${config.adminName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = config.adminEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Button(
                            onClick = onToggleRole,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("lock_admin_mode_btn")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lock Admin", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "🛡️ You are logged in with full administrative authority. You can modify fund accounts, manage the PIN, verify cash claims, and dispatch notifications. Lock before letting members view the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 2. Centralized Cloud Database (Firestore) Management Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("centralized_database_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Centralized Cloud Database",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Firebase Firestore • Real-Time Multi-Device",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { showArchitectureDialog = true },
                            modifier = Modifier.testTag("database_info_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Database Architecture Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Connection status badge
                    val isConnected = syncState == CloudSyncState.CONNECTED || syncState == CloudSyncState.SYNCED
                    val isSyncing = syncState == CloudSyncState.SYNCING
                    val isOffline = syncState == CloudSyncState.OFFLINE_CACHE
                    
                    val badgeBg = when {
                        isConnected -> Color(0xFF064E3B).copy(alpha = 0.15f)
                        isSyncing -> Color(0xFF78350F).copy(alpha = 0.15f)
                        isOffline -> Color(0xFF1E3A8A).copy(alpha = 0.15f)
                        else -> Color(0xFF4C1D95).copy(alpha = 0.15f)
                    }
                    val badgeTextColor = when {
                        isConnected -> Color(0xFF059669)
                        isSyncing -> Color(0xFFD97706)
                        isOffline -> Color(0xFF2563EB)
                        else -> Color(0xFF7C3AED)
                    }
                    val badgeText = when {
                        isConnected -> "🟢 Central Database Connected & Active"
                        isSyncing -> "🔄 Syncing with Cloud Database..."
                        isOffline -> "⚡ Offline Cache Active (Local SQLite Room)"
                        else -> "⚙️ Central Cloud Storage Initialized"
                    }
                    val badgeIcon = when {
                        isConnected -> Icons.Default.CloudDone
                        isSyncing -> Icons.Default.Sync
                        isOffline -> Icons.Default.Storage
                        else -> Icons.Default.Cloud
                    }

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = badgeBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = null,
                                tint = badgeTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = badgeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeTextColor
                            )
                        }
                    }

                    Text(
                        text = syncMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Last Synced & Multi-Device Live Sync badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val lastSyncStr = if (lastSyncTimestamp != null) {
                            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
                            "Last Synced: ${sdf.format(Date(lastSyncTimestamp!!))}"
                        } else {
                            "Status: Saved in Central Database"
                        }
                        Text(
                            text = lastSyncStr,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = if (isRealtimeActive) "⚡ Multi-Device Sync: ON" else "⚡ Room Local Cache: ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider()

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isSyncingNow = true
                                viewModel.syncAllToCloud { success, msg ->
                                    isSyncingNow = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isSyncingNow,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_to_cloud_btn")
                        ) {
                            if (isSyncingNow) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Syncing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync to Cloud", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                isSyncingNow = true
                                viewModel.fetchAllFromCloud { success, msg ->
                                    isSyncingNow = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isSyncingNow,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pull_from_cloud_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pull from Cloud", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2b. Google AdMob Monetization & Banner Unit Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Google AdMob Monetization",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Connected",
                                fontSize = 11.sp,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "Configured with your production Google AdMob IDs. Banner ads are served across the Dashboard, Cash Verification, and Reports views.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Application ID:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("ca-app-pub-9304808102028896~2048644415", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Banner Ad Unit ID:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("ca-app-pub-9304808102028896/6043729743", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Format & Placements:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Standard Banner (320x50) • 3 Views", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Live Banner Preview:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AdBannerCard(campaignIndex = 0)
                }
            }
        }

        // 3. Fund & UPI Configuration (Admin Only Editable)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fund & UPI Parameters (Admin Only)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("Receiver UPI ID (Contributions Target)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_upi_id_input")
                    )

                    OutlinedTextField(
                        value = upiName,
                        onValueChange = { upiName = it },
                        label = { Text("Fund / Payee Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = monthlyAmount,
                        onValueChange = { monthlyAmount = it },
                        label = { Text("Monthly Contribution Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Admin Contact Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val amount = monthlyAmount.toDoubleOrNull() ?: 500.0
                            viewModel.updateConfig(
                                config.copy(
                                    upiId = upiId.trim(),
                                    upiName = upiName.trim(),
                                    monthlyAmount = amount,
                                    contactPhone = contactPhone.trim(),
                                    autoNotifyMissed = autoNotify
                                )
                            )
                            Toast.makeText(context, "Fund settings updated and saved!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_settings_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Fund Parameters")
                    }
                }
            }
        }

        // 4. Admin Security PIN Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Security PIN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "This secret PIN is required to unlock this Settings page, approve physical cash payments, and trigger notification broadcasts. Do not share with other members.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = adminPin,
                        onValueChange = { if (it.length <= 8) adminPin = it },
                        label = { Text("Security PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPin = !showPin }) {
                                Icon(
                                    imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle PIN Visibility"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_pin_input")
                    )

                    Button(
                        onClick = {
                            if (adminPin.length < 4) {
                                Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.updateConfig(config.copy(adminPin = adminPin.trim()))
                                Toast.makeText(context, "Admin PIN successfully updated!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_pin_btn")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Security PIN")
                    }
                }
            }
        }

        // 5. Notification Broadcasting Controls (ADMIN ONLY)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Push Notification Broadcasts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ADMIN ONLY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Monthly Dues Auto-Reminder",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Automatically alerts members with pending ₹500 dues on the 1st of each month.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = autoNotify,
                            onCheckedChange = {
                                autoNotify = it
                                viewModel.updateConfig(config.copy(autoNotifyMissed = it))
                            }
                        )
                    }

                    HorizontalDivider()

                    Button(
                        onClick = {
                            val count = viewModel.sendMissedReminderNotifications(context)
                            Toast.makeText(
                                context,
                                "Dispatched push reminder to $count members with pending dues.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_reminders_settings_btn")
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Broadcast Reminders to Pending Members")
                    }

                    OutlinedButton(
                        onClick = {
                            NotificationHelper.sendContributionReminderNotification(
                                context = context,
                                memberName = "Aarthi Sundaram",
                                monthYear = viewModel.selectedMonthYear.value,
                                amount = config.monthlyAmount
                            )
                            Toast.makeText(context, "Test notification dispatched to your phone!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_push_notification_btn")
                    ) {
                        Text("Trigger Test Dues Notification")
                    }
                }
            }
        }

        // 6. Activity & Financial Audit Trail (Complete Log of Everyone)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Activity & Financial Audit Trail",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "${config.fundTitle} - Activity Audit Trail")
                            putExtra(Intent.EXTRA_TEXT, viewModel.buildAuditLogsExportText())
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Activity Log via"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export Audit Log", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            // Filter chips for activities
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = auditFilter == "ALL",
                    onClick = { auditFilter = "ALL" },
                    label = { Text("All (${auditLogs.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = auditFilter == "PAYMENTS",
                    onClick = { auditFilter = "PAYMENTS" },
                    label = { Text("Payments", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = auditFilter == "CASH",
                    onClick = { auditFilter = "CASH" },
                    label = { Text("Cash Verifications", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = auditFilter == "ADMIN",
                    onClick = { auditFilter = "ADMIN" },
                    label = { Text("Admin Actions", fontSize = 11.sp) }
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "No activities matching selected filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredLogs.take(20), key = { it.id }) { log ->
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(log.timestamp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (log.performedByRole == "ADMIN") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (log.performedByRole == "ADMIN") MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondary
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = log.performedByRole,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.action,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = dateStr,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showArchitectureDialog) {
        AlertDialog(
            onDismissRequest = { showArchitectureDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Centralized Cloud Database",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Central Storage: Google Firebase Cloud Firestore centralizes everyone's member data, monthly dues, UPI payments, and cash submissions.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "2. Admin Exclusivity: Only admin (${config.adminName}) has permission to edit fund parameters, change the receiver UPI ID, and trigger push reminders. Non-admins cannot access or edit Settings.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "3. Real-Time Sync: Whenever any member records a payment, it instantly syncs to the central database for immediate admin verification.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "4. Offline-First Resilience: Integrated SQLite Room database ensures complete functionality and zero lag even when offline, syncing automatically when reconnected.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showArchitectureDialog = false },
                    modifier = Modifier.testTag("close_architecture_dialog_btn")
                ) {
                    Text("Got It")
                }
            }
        )
    }
}

/**
 * Screen displayed when a non-admin attempts to view Settings.
 */
@Composable
private fun AdminLockedGateScreen(
    adminName: String,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("admin_locked_gate_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Admin Access Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "The Settings page is restricted to the administrator ($adminName). Other members cannot edit fund parameters, change the UPI ID, or trigger notification reminders.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF064E3B).copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Centralized Database: All member contributions, payments, and activities are permanently stored in Firebase Cloud Firestore & local database.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onUnlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unlock_admin_settings_btn")
                ) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock with Admin PIN")
                }
            }
        }
    }
}
