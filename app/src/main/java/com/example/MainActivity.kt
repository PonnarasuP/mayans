package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.WelfareViewModel
import com.example.ui.components.AdminPinDialog
import com.example.ui.components.CompactAdBanner
import com.example.ui.screens.CashVerificationScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.NotificationHelper

enum class WelfareScreen {
    DASHBOARD,
    REPORTS,
    CASH_VERIFY,
    MEMBERS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)

        // Initialize Google Mobile Ads (AdMob)
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize AdMob: ${e.message}")
        }

        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: WelfareViewModel = viewModel()) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(WelfareScreen.DASHBOARD) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingScreenAfterPin by remember { mutableStateOf<WelfareScreen?>(null) }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val selectedMonth by viewModel.selectedMonthYear.collectAsStateWithLifecycle()
    val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val contributions by viewModel.filteredMonthlyContributions.collectAsStateWithLifecycle()
    val rawContributions by viewModel.rawMonthlyContributions.collectAsStateWithLifecycle()
    val cashPendingList by viewModel.cashPendingList.collectAsStateWithLifecycle()
    val allMembers by viewModel.allMembers.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = config.fundTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    if (isAdminMode) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("admin_mode_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ADMIN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Exit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.exitAdminMode()
                                            Toast.makeText(context, "Switched to Member mode", Toast.LENGTH_SHORT).show()
                                            if (currentScreen == WelfareScreen.SETTINGS) {
                                                currentScreen = WelfareScreen.DASHBOARD
                                            }
                                        }
                                        .testTag("exit_admin_btn")
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable {
                                    pendingScreenAfterPin = null
                                    showPinDialog = true
                                }
                                .testTag("admin_login_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Admin Login",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Admin Login",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                CompactAdBanner(
                    campaignIndex = 0,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == WelfareScreen.DASHBOARD,
                        onClick = { currentScreen = WelfareScreen.DASHBOARD },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("nav_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentScreen == WelfareScreen.MEMBERS,
                        onClick = { currentScreen = WelfareScreen.MEMBERS },
                        icon = { Icon(Icons.Default.Group, contentDescription = "Members") },
                        label = { Text("Members") },
                        modifier = Modifier.testTag("nav_members")
                    )

                    // Settings menu is ONLY available for ADMIN as requested
                    if (isAdminMode) {
                        NavigationBarItem(
                            selected = currentScreen == WelfareScreen.SETTINGS,
                            onClick = { currentScreen = WelfareScreen.SETTINGS },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings") },
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            WelfareScreen.DASHBOARD -> DashboardScreen(
                viewModel = viewModel,
                selectedMonth = selectedMonth,
                isAdminMode = isAdminMode,
                config = config,
                contributions = contributions,
                cashPendingCount = cashPendingList.size,
                searchQuery = searchQuery,
                statusFilter = statusFilter,
                onNavigateToCashVerification = {
                    if (isAdminMode) {
                        currentScreen = WelfareScreen.CASH_VERIFY
                    } else {
                        pendingScreenAfterPin = WelfareScreen.CASH_VERIFY
                        showPinDialog = true
                    }
                },
                onNavigateToMembers = { currentScreen = WelfareScreen.MEMBERS },
                modifier = Modifier.padding(innerPadding)
            )

            WelfareScreen.REPORTS -> ReportsScreen(
                viewModel = viewModel,
                selectedMonth = selectedMonth,
                config = config,
                contributions = rawContributions,
                modifier = Modifier.padding(innerPadding)
            )

            WelfareScreen.CASH_VERIFY -> CashVerificationScreen(
                viewModel = viewModel,
                cashPendingList = cashPendingList,
                isAdminMode = isAdminMode,
                onUnlockAdmin = {
                    pendingScreenAfterPin = WelfareScreen.CASH_VERIFY
                    showPinDialog = true
                },
                modifier = Modifier.padding(innerPadding)
            )

            WelfareScreen.MEMBERS -> MembersScreen(
                viewModel = viewModel,
                members = allMembers,
                isAdminMode = isAdminMode,
                modifier = Modifier.padding(innerPadding)
            )

            WelfareScreen.SETTINGS -> {
                if (!isAdminMode) {
                    currentScreen = WelfareScreen.DASHBOARD
                } else {
                    SettingsScreen(
                        viewModel = viewModel,
                        config = config,
                        isAdminMode = isAdminMode,
                        auditLogs = auditLogs,
                        onToggleRole = {
                            viewModel.exitAdminMode()
                            Toast.makeText(context, "Switched to Member mode", Toast.LENGTH_SHORT).show()
                            currentScreen = WelfareScreen.DASHBOARD
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        AdminPinDialog(
            correctPin = config.adminPin,
            onDismiss = {
                showPinDialog = false
                pendingScreenAfterPin = null
            },
            onAuthenticated = {
                viewModel.authenticateAdmin(config.adminPin)
                showPinDialog = false
                Toast.makeText(context, "Admin Mode Unlocked!", Toast.LENGTH_SHORT).show()
                pendingScreenAfterPin?.let { target ->
                    currentScreen = target
                    pendingScreenAfterPin = null
                }
            }
        )
    }
}
