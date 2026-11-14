package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.local.AccountRole
import com.example.ui.screens.*
import com.example.ui.viewmodel.WahjViewModel

@Composable
fun WahjNavHost(viewModel: WahjViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val sessionPrefs by viewModel.sessionPrefs.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val activeAccount by viewModel.activeAccount.collectAsStateWithLifecycle()
    val children by viewModel.children.collectAsStateWithLifecycle()
    val selectedChild by viewModel.selectedChild.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val generatedQr by viewModel.generatedQrPayload.collectAsStateWithLifecycle()
    val scannedQr by viewModel.scannedQrPayload.collectAsStateWithLifecycle()
    val qrError by viewModel.qrError.collectAsStateWithLifecycle()
    val lastGeneratedQrRaw by viewModel.lastGeneratedQrRaw.collectAsStateWithLifecycle()
    val pendingApproval by viewModel.pendingPairingApproval.collectAsStateWithLifecycle()

    LaunchedEffect(uiMessage) {
        val msg = uiMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearUiMessage()
    }

    val currentRole = remember(sessionPrefs.activeRole, activeAccount?.role) {
        val raw = activeAccount?.role ?: sessionPrefs.activeRole
        runCatching { AccountRole.valueOf(raw ?: AccountRole.PARENT.name) }
            .getOrDefault(AccountRole.PARENT)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { outerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = outerPadding.calculateBottomPadding().coerceAtMost(0.dp_zero))
        ) {
            NavHost(
                navController = navController,
                startDestination = WahjRoute.Splash
            ) {
                // 1. Splash
                composable<WahjRoute.Splash> {
                    SplashScreen(
                        sessionPrefs = sessionPrefs,
                        onNavigateToWelcome = {
                            navController.navigate(WahjRoute.Welcome) {
                                popUpTo(WahjRoute.Splash) { inclusive = true }
                            }
                        },
                        onNavigateToParent = {
                            navController.navigate(WahjRoute.ParentDashboard) {
                                popUpTo(WahjRoute.Splash) { inclusive = true }
                            }
                        },
                        onNavigateToChild = {
                            navController.navigate(WahjRoute.ChildDashboard) {
                                popUpTo(WahjRoute.Splash) { inclusive = true }
                            }
                        }
                    )
                }

                // 2. Welcome
                composable<WahjRoute.Welcome> {
                    WelcomeScreen(
                        onStartRoleSelection = { navController.navigate(WahjRoute.RoleSelection) },
                        onNavigateToLogin = { navController.navigate(WahjRoute.Login) }
                    )
                }

                // 3. Role Selection
                composable<WahjRoute.RoleSelection> {
                    RoleSelectionScreen(
                        onSelectParent = {
                            viewModel.selectRole(AccountRole.PARENT)
                            navController.navigate(WahjRoute.Register)
                        },
                        onSelectChild = {
                            viewModel.selectRole(AccountRole.CHILD)
                            navController.navigate(WahjRoute.Register)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 4. Login
                composable<WahjRoute.Login> {
                    LoginScreen(
                        existingAccounts = accounts,
                        authError = authError,
                        onLogin = { email, pass ->
                            viewModel.login(email, pass) { acc ->
                                val dest = if (acc.role == AccountRole.PARENT.name) {
                                    WahjRoute.ParentDashboard
                                } else {
                                    WahjRoute.ChildDashboard
                                }
                                navController.navigate(dest) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onQuickSwitchAccount = { acc ->
                            viewModel.switchAccount(acc) { switched ->
                                val dest = if (switched.role == AccountRole.PARENT.name) {
                                    WahjRoute.ParentDashboard
                                } else {
                                    WahjRoute.ChildDashboard
                                }
                                navController.navigate(dest) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onNavigateToRegister = {
                            viewModel.clearAuthError()
                            navController.navigate(WahjRoute.Register)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 5. Register
                composable<WahjRoute.Register> {
                    RegisterScreen(
                        initialRole = currentRole,
                        authError = authError,
                        onRegister = { name, email, pass, role ->
                            viewModel.register(name, email, pass, role) { acc ->
                                val dest: WahjRoute = if (acc.role == AccountRole.PARENT.name) {
                                    WahjRoute.ParentDashboard
                                } else {
                                    WahjRoute.ChildQrPairing
                                }
                                navController.navigate(dest) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onNavigateToLogin = {
                            viewModel.clearAuthError()
                            navController.navigate(WahjRoute.Login)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 6. Child Dashboard
                composable<WahjRoute.ChildDashboard> {
                    val activeChild = selectedChild
                    val childId = activeChild?.id ?: ""
                    val childMessages by viewModel.observeMessages(childId)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    val childApps by viewModel.observeAppRules(childId)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    val childSchedules by viewModel.observeSchedules(childId)
                        .collectAsStateWithLifecycle(initialValue = emptyList())

                    LaunchedEffect(childId) {
                        if (childId.isNotBlank()) viewModel.syncChildTelemetry(childId)
                    }

                    ChildDashboardScreen(
                        child = activeChild,
                        messages = childMessages,
                        appRules = childApps,
                        schedules = childSchedules,
                        onSyncNow = { if (childId.isNotBlank()) viewModel.syncChildTelemetry(childId) },
                        onRequestExtraTime = { mins, reason ->
                            if (childId.isNotBlank()) viewModel.requestExtraTime(childId, mins, reason)
                        },
                        onOpenMessages = {
                            if (childId.isNotBlank()) navController.navigate(WahjRoute.Messages(childId))
                        },
                        onOpenQrPairing = { navController.navigate(WahjRoute.ChildQrPairing) },
                        onToggleLocationSharing = { enabled ->
                            activeChild?.let { viewModel.toggleLocationSharing(it, enabled) }
                        },
                        onOpenSettings = { navController.navigate(WahjRoute.Settings) }
                    )
                }

                // 7. Child QR Pairing
                composable<WahjRoute.ChildQrPairing> {
                    ChildQrPairingScreen(
                        generatedQr = generatedQr,
                        pendingApproval = pendingApproval,
                        onGenerateQr = { name, type -> viewModel.generateChildQr(name, type) },
                        onPairingApproved = { pairedChildId ->
                            viewModel.clearPendingPairingApproval()
                            navController.navigate(WahjRoute.PairingSuccess(pairedChildId, isChildFlow = true)) {
                                popUpTo(WahjRoute.ChildQrPairing) { inclusive = true }
                            }
                        },
                        onOpenChildDashboard = {
                            navController.navigate(WahjRoute.ChildDashboard) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 8. Parent QR Scanner
                composable<WahjRoute.ParentQrScanner> {
                    ParentQrScannerScreen(
                        scannedPayload = scannedQr,
                        qrError = qrError,
                        lastGeneratedQrRaw = lastGeneratedQrRaw,
                        onQrScanned = { raw -> viewModel.verifyScannedQr(raw) },
                        onConfirmPairing = { name, relation ->
                            viewModel.confirmParentPairing(name, relation) { paired ->
                                navController.navigate(WahjRoute.PairingSuccess(paired.id, isChildFlow = false)) {
                                    popUpTo(WahjRoute.ParentQrScanner) { inclusive = true }
                                }
                            }
                        },
                        onResetScan = { viewModel.clearQrState() },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 9. Pairing Success
                composable<WahjRoute.PairingSuccess> { entry ->
                    val args = entry.toRoute<WahjRoute.PairingSuccess>()
                    val child by viewModel.observeChild(args.childId).collectAsStateWithLifecycle(initialValue = null)
                    PairingSuccessScreen(
                        childName = child?.name ?: "جهاز الطفل",
                        deviceName = child?.deviceName ?: "Android",
                        isChildFlow = args.isChildFlow,
                        onContinue = {
                            val target: WahjRoute = if (args.isChildFlow) WahjRoute.ChildDashboard else WahjRoute.ParentDashboard
                            navController.navigate(target) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                // 10. Parent Dashboard
                composable<WahjRoute.ParentDashboard> {
                    val primaryChildId = selectedChild?.id ?: children.firstOrNull()?.id ?: ""
                    val topApps by viewModel.observeAppRules(primaryChildId)
                        .collectAsStateWithLifecycle(initialValue = emptyList())

                    ParentDashboardScreen(
                        parentName = activeAccount?.fullName ?: "ولي الأمر",
                        children = children,
                        alerts = alerts,
                        topApps = topApps,
                        onAddChildClick = {
                            viewModel.clearQrState()
                            navController.navigate(WahjRoute.ParentQrScanner)
                        },
                        onOpenChildDetail = { id ->
                            viewModel.selectChild(id)
                            navController.navigate(WahjRoute.ChildDetail(id))
                        },
                        onOpenScreenTime = { id ->
                            viewModel.selectChild(id)
                            navController.navigate(WahjRoute.ScreenTime(id))
                        },
                        onOpenAlerts = { navController.navigate(WahjRoute.Alerts) },
                        onOpenSettings = { navController.navigate(WahjRoute.Settings) },
                        onOpenAccounts = { navController.navigate(WahjRoute.AccountsDevices) },
                        onOpenSubscriptions = { navController.navigate(WahjRoute.Subscriptions) }
                    )
                }

                // 11. Child Detail
                composable<WahjRoute.ChildDetail> { entry ->
                    val args = entry.toRoute<WahjRoute.ChildDetail>()
                    val child by viewModel.observeChild(args.childId).collectAsStateWithLifecycle(initialValue = null)
                    ChildDetailScreen(
                        child = child,
                        onSyncTelemetry = { viewModel.syncChildTelemetry(args.childId) },
                        onToggleLock = { child?.let { viewModel.toggleRemoteLock(it) } },
                        onOpenApps = { navController.navigate(WahjRoute.AppManagement(args.childId)) },
                        onOpenScreenTime = { navController.navigate(WahjRoute.ScreenTime(args.childId)) },
                        onOpenMessages = { navController.navigate(WahjRoute.Messages(args.childId)) },
                        onOpenLocation = { navController.navigate(WahjRoute.Location(args.childId)) },
                        onOpenReports = { navController.navigate(WahjRoute.Reports(args.childId)) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 12. App Management
                composable<WahjRoute.AppManagement> { entry ->
                    val args = entry.toRoute<WahjRoute.AppManagement>()
                    val child by viewModel.observeChild(args.childId).collectAsStateWithLifecycle(initialValue = null)
                    val rules by viewModel.observeAppRules(args.childId).collectAsStateWithLifecycle(initialValue = emptyList())
                    AppManagementScreen(
                        childName = child?.name ?: "",
                        rules = rules,
                        onToggleAllowed = { rule ->
                            val nowRestricted = !rule.isRestricted
                            viewModel.updateAppRule(
                                rule.copy(isRestricted = nowRestricted, isAllowed = !nowRestricted)
                            )
                        },
                        onUpdateAppLimit = { rule, newLimit ->
                            viewModel.updateAppRule(rule.copy(dailyLimitMinutes = newLimit))
                        },
                        onRefreshInstalledApps = { viewModel.syncChildTelemetry(args.childId) },
                        onTestRestrictedAlert = { appName -> viewModel.testRestrictedAppAlert(args.childId, appName) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 13. Screen Time Management
                composable<WahjRoute.ScreenTime> { entry ->
                    val args = entry.toRoute<WahjRoute.ScreenTime>()
                    val child by viewModel.observeChild(args.childId).collectAsStateWithLifecycle(initialValue = null)
                    val schedules by viewModel.observeSchedules(args.childId).collectAsStateWithLifecycle(initialValue = emptyList())
                    val requests by viewModel.observeTimeRequests(args.childId).collectAsStateWithLifecycle(initialValue = emptyList())
                    ScreenTimeScreen(
                        child = child,
                        schedules = schedules,
                        timeRequests = requests,
                        onSavePolicy = { limit, sStart, sEnd, bStart, bEnd ->
                            child?.let { viewModel.updateScreenTimePolicy(it, limit, sStart, sEnd, bStart, bEnd) }
                        },
                        onGrantExtraTime = { mins ->
                            child?.let { viewModel.grantExtraTime(it, mins) }
                        },
                        onResolveRequest = { req, approve ->
                            viewModel.resolveTimeRequest(req, approve)
                        },
                        onToggleLock = {
                            child?.let { viewModel.toggleRemoteLock(it) }
                        },
                        onAddSchedule = { title, type, start, end ->
                            viewModel.upsertSchedule(
                                com.example.data.local.ScheduleEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    childId = args.childId,
                                    title = title,
                                    type = type,
                                    startTime = start,
                                    endTime = end,
                                    daysSummary = "يومياً",
                                    isEnabled = true
                                )
                            )
                        },
                        onDeleteSchedule = { id -> viewModel.deleteSchedule(id) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 14. Messages
                composable<WahjRoute.Messages> { entry ->
                    val args = entry.toRoute<WahjRoute.Messages>()
                    val child by viewModel.observeChild(args.childId).collectAsStateWithLifecycle(initialValue = null)
                    val messages by viewModel.observeMessages(args.childId).collectAsStateWithLifecycle(initialValue = emptyList())
                    MessagesScreen(
                        childName = child?.name ?: "العائلة",
                        currentRole = currentRole,
                        messages = messages,
                        onSendMessage = { content, isPreset, delay ->
                            viewModel.sendMessage(args.childId, currentRole, content, isPreset, delay)
                        },
                        onMarkRead = { viewModel.markMessagesRead(args.childId, currentRole) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 15. Location
                composable<WahjRoute.Location> { entry ->
                    val args = entry.toRoute<WahjRoute.Location>()
                    val child by viewModel.observeChild(args.childId).collectAsStateWithLifecycle(initialValue = null)
                    val zones by viewModel.observeSafeZones(args.childId).collectAsStateWithLifecycle(initialValue = emptyList())
                    val history by viewModel.observeLocationHistory(args.childId).collectAsStateWithLifecycle(initialValue = emptyList())
                    LocationScreen(
                        child = child,
                        safeZones = zones,
                        history = history,
                        onRefreshLocation = { viewModel.syncChildTelemetry(args.childId) },
                        onToggleSharing = { enabled ->
                            child?.let { viewModel.toggleLocationSharing(it, enabled) }
                        },
                        onAddSafeZone = { name, lat, lng, radius ->
                            viewModel.addSafeZone(args.childId, name, lat, lng, radius)
                        },
                        onToggleSafeZone = { zone -> viewModel.toggleSafeZone(zone) },
                        onDeleteSafeZone = { id -> viewModel.deleteSafeZone(id) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 16. Reports
                composable<WahjRoute.Reports> { entry ->
                    val args = entry.toRoute<WahjRoute.Reports>()
                    val child by viewModel.observeChild(args.childId).collectAsStateWithLifecycle(initialValue = null)
                    val rules by viewModel.observeAppRules(args.childId).collectAsStateWithLifecycle(initialValue = emptyList())
                    val schedules by viewModel.observeSchedules(args.childId).collectAsStateWithLifecycle(initialValue = emptyList())
                    val requests by viewModel.observeTimeRequests(args.childId).collectAsStateWithLifecycle(initialValue = emptyList())
                    ReportsScreen(
                        child = child,
                        appRules = rules,
                        schedules = schedules,
                        timeRequests = requests,
                        onBack = { navController.popBackStack() }
                    )
                }

                // 17. Alerts
                composable<WahjRoute.Alerts> {
                    AlertsScreen(
                        alerts = alerts,
                        onMarkRead = { viewModel.markAlertRead(it) },
                        onMarkAllRead = { viewModel.markAllAlertsRead() },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 18. Settings
                composable<WahjRoute.Settings> {
                    SettingsScreen(
                        themeMode = sessionPrefs.themeMode,
                        hasParentPin = !sessionPrefs.parentPinHash.isNullOrBlank(),
                        onChangeTheme = { viewModel.setThemeMode(it) },
                        onSetParentPin = { pin ->
                            if (currentRole == AccountRole.PARENT) {
                                viewModel.setParentPin(pin)
                            }
                        },
                        onOpenSubscriptions = {
                            if (currentRole == AccountRole.PARENT) {
                                navController.navigate(WahjRoute.Subscriptions)
                            }
                        },
                        onOpenAccounts = { navController.navigate(WahjRoute.AccountsDevices) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 19. Subscriptions
                composable<WahjRoute.Subscriptions> {
                    SubscriptionsScreen(onBack = { navController.popBackStack() })
                }

                // 20. Accounts & Devices
                composable<WahjRoute.AccountsDevices> {
                    AccountsDevicesScreen(
                        activeAccount = activeAccount,
                        allAccounts = accounts,
                        children = if (currentRole == AccountRole.PARENT) children else emptyList(),
                        lanAddress = viewModel.getLocalIpAndPort(),
                        onSwitchAccount = { acc ->
                            if (currentRole == AccountRole.PARENT || acc.role == AccountRole.CHILD.name) {
                                viewModel.switchAccount(acc) { switched ->
                                    val dest = if (switched.role == AccountRole.PARENT.name) {
                                        WahjRoute.ParentDashboard
                                    } else {
                                        WahjRoute.ChildDashboard
                                    }
                                    navController.navigate(dest) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            } else {
                                // Child cannot switch to parent account without logging in
                                navController.navigate(WahjRoute.Login)
                            }
                        },
                        onUnlinkChild = { id ->
                            if (currentRole == AccountRole.PARENT) viewModel.unlinkChild(id)
                        },
                        onAddChild = {
                            if (currentRole == AccountRole.PARENT) {
                                viewModel.clearQrState()
                                navController.navigate(WahjRoute.ParentQrScanner)
                            }
                        },
                        onLogout = {
                            viewModel.logout {
                                navController.navigate(WahjRoute.Welcome) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private val Int.dp_zero get() = androidx.compose.ui.unit.Dp(0f)
