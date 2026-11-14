package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.system.DevicePolicyHelper
import com.example.system.UsageStatsHelper
import com.example.ui.components.*
import com.example.ui.theme.WahjAmberNotice
import com.example.ui.theme.WahjOnlineGreen

@Composable
fun ParentDashboardScreen(
    parentName: String,
    children: List<ChildProfileEntity>,
    alerts: List<AlertEntity>,
    topApps: List<AppRuleEntity>,
    onAddChildClick: () -> Unit,
    onOpenChildDetail: (String) -> Unit,
    onOpenScreenTime: (String) -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenSubscriptions: () -> Unit
) {
    val unreadAlerts = alerts.count { !it.isRead }
    val onlineCount = children.count { it.isOnline }
    val totalScreenMinutes = children.sumOf { it.usedTodayMinutes }

    Scaffold(
        topBar = {
            WahjTopBar(
                title = "لوحة تحكم ولي الأمر",
                subtitle = "أهلاً $parentName",
                actions = {
                    IconButton(
                        onClick = onOpenAlerts,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dashboard_alerts_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadAlerts > 0) {
                                    Badge { Text(unreadAlerts.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "التنبيهات")
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dashboard_settings_button")
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "الإعدادات")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddChildClick,
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                text = { Text("إضافة طفل") },
                modifier = Modifier.testTag("fab_add_child")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary KPI grid (2x2)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WahjStatTile(
                        label = "الأطفال المرتبطون",
                        value = children.size.toString(),
                        icon = Icons.Outlined.People,
                        modifier = Modifier.weight(1f)
                    )
                    WahjStatTile(
                        label = "الأجهزة المتصلة",
                        value = "$onlineCount / ${children.size}",
                        icon = Icons.Outlined.Devices,
                        accent = WahjOnlineGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WahjStatTile(
                        label = "استخدام اليوم",
                        value = formatMinutesArabic(totalScreenMinutes),
                        icon = Icons.Outlined.Timer,
                        modifier = Modifier.weight(1f)
                    )
                    WahjStatTile(
                        label = "التنبيهات الجديدة",
                        value = unreadAlerts.toString(),
                        icon = Icons.Outlined.NotificationsActive,
                        accent = if (unreadAlerts > 0) WahjAmberNotice else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick shortcuts row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenAccounts,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الحسابات والأجهزة", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onOpenSubscriptions,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الاشتراكات", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Most used apps summary
            if (topApps.isNotEmpty()) {
                item {
                    WahjCard {
                        Text("التطبيقات الأكثر استخداماً اليوم", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        topApps.take(4).forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(app.appName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    formatMinutesArabic(app.usedTodayMinutes),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Children section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الأجهزة المرتبطة", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onAddChildClick) {
                        Text("+ ربط جهاز عبر QR")
                    }
                }
            }

            if (children.isEmpty()) {
                item {
                    WahjCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Outlined.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("لا يوجد أطفال مرتبطون بعد", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "اضغط على \"إضافة طفل\" لمسح رمز QR من جهاز طفلك وربطه بشكل آمن.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onAddChildClick, shape = RoundedCornerShape(12.dp)) {
                                Text("مسح QR وإضافة طفل")
                            }
                        }
                    }
                }
            } else {
                items(children, key = { it.id }) { child ->
                    ChildDashboardCard(
                        child = child,
                        onViewDetails = { onOpenChildDetail(child.id) },
                        onControlDevice = { onOpenScreenTime(child.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChildDashboardCard(
    child: ChildProfileEntity,
    onViewDetails: () -> Unit,
    onControlDevice: () -> Unit
) {
    WahjCard(modifier = Modifier.testTag("child_card_${child.id}")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (child.deviceType == "تابلت") Icons.Outlined.TabletAndroid else Icons.Outlined.Smartphone,
                    contentDescription = child.deviceType,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(child.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    WahjStatusBadge(
                        text = if (child.isOnline) "متصل" else "غير متصل",
                        isPositive = child.isOnline
                    )
                }
                Text(
                    "${child.deviceType} • ${child.deviceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MiniTelemetryPill(
                icon = Icons.Outlined.BatteryChargingFull,
                text = "${child.batteryLevel}%"
            )
            MiniTelemetryPill(
                icon = Icons.Outlined.Schedule,
                text = formatMinutesArabic(child.usedTodayMinutes)
            )
            MiniTelemetryPill(
                icon = Icons.Outlined.AccessTime,
                text = formatTimestampArabic(child.lastSeenAt)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Outlined.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = child.lastAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onViewDetails,
                modifier = Modifier
                    .weight(1f)
                    .testTag("view_child_details_${child.id}"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("عرض التفاصيل")
            }
            Button(
                onClick = onControlDevice,
                modifier = Modifier
                    .weight(1f)
                    .testTag("control_child_device_${child.id}"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("التحكم بالجهاز")
            }
        }
    }
}

@Composable
private fun MiniTelemetryPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ChildDetailScreen(
    child: ChildProfileEntity?,
    onSyncTelemetry: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenScreenTime: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenReports: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            WahjTopBar(
                title = child?.name ?: "تفاصيل الطفل",
                subtitle = child?.deviceName,
                onBack = onBack,
                actions = {
                    IconButton(onClick = onSyncTelemetry) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "تحديث البيانات")
                    }
                }
            )
        }
    ) { padding ->
        if (child == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("لم يتم العثور على ملف الطفل")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            WahjCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${child.name} (${child.relation})", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${child.deviceType} • ${child.osVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    WahjStatusBadge(
                        text = if (child.isDeviceLocked) "مقفل عن بُعد" else if (child.isOnline) "متصل الآن" else "غير متصل",
                        isPositive = !child.isDeviceLocked && child.isOnline
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${child.batteryLevel}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("البطارية", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatMinutesArabic(child.usedTodayMinutes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("استخدام اليوم", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            formatMinutesArabic((child.dailyLimitMinutes + child.bonusMinutesToday - child.usedTodayMinutes).coerceAtLeast(0)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("المتبقي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Button(
                onClick = onToggleLock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("detail_lock_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (child.isDeviceLocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (child.isDeviceLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (child.isDeviceLocked) "إلغاء وضع القفل في وهج" else "قفل الجهاز عن بُعد")
            }

            Text(
                text = "ملاحظة: يتم قفل الشاشة عبر صلاحيات Android DevicePolicyManager الرسمية؛ ولا يدعم النظام فتح قفل شاشة الجهاز عن بُعد لحماية الخصوصية.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("أدوات الإدارة والمتابعة", style = MaterialTheme.typography.titleMedium)

            DetailNavTile(Icons.Outlined.Apps, "إدارة التطبيقات", "السماح والتقييد ومدة كل تطبيق", onOpenApps)
            DetailNavTile(Icons.Outlined.Timer, "إدارة وقت الشاشة والجدول", "الدراسة والنوم والوقت الإضافي", onOpenScreenTime)
            DetailNavTile(Icons.Outlined.ChatBubbleOutline, "الرسائل العائلية", "رسائل فورية ومجدولة باسم وهج", onOpenMessages)
            DetailNavTile(Icons.Outlined.Map, "الموقع الجغرافي والمناطق الآمنة", child.lastAddress, onOpenLocation)
            DetailNavTile(Icons.Outlined.BarChart, "التقارير الإحصائية", "تحليل يومي وأسبوعي للاستخدام والالتزام", onOpenReports)
        }
    }
}

@Composable
private fun DetailNavTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    WahjCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ChevronLeft, contentDescription = null)
        }
    }
}

@Composable
fun AppManagementScreen(
    childName: String,
    rules: List<AppRuleEntity>,
    onToggleAllowed: (AppRuleEntity) -> Unit,
    onUpdateAppLimit: (AppRuleEntity, Int) -> Unit,
    onRefreshInstalledApps: () -> Unit,
    onTestRestrictedAlert: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasUsagePermission by remember { mutableStateOf(UsageStatsHelper.hasUsageAccessPermission(context)) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(rules, searchQuery) {
        if (searchQuery.isBlank()) rules
        else rules.filter { it.appName.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            WahjTopBar(
                title = "إدارة التطبيقات",
                subtitle = childName,
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        hasUsagePermission = UsageStatsHelper.hasUsageAccessPermission(context)
                        onRefreshInstalledApps()
                    }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "تحديث القائمة")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WahjCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إذن إحصائيات الاستخدام (Usage Access)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (hasUsagePermission) "مفعل — يتم قراءة أوقات الاستخدام من UsageStatsManager."
                                else "مطلوب لعرض مدة استخدام كل تطبيق يومياً وأسبوعياً بشكل فعلي.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(UsageStatsHelper.buildUsageAccessSettingsIntent())
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (hasUsagePermission) "مفعل" else "تفعيل")
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("ابحث عن تطبيق أو فئة") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            if (filtered.isEmpty()) {
                item {
                    WahjCard {
                        Text(
                            "لا توجد تطبيقات مطابقة أو لم يتم مزامنة التطبيقات المثبتة بعد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { rule ->
                    WahjCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.appName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${rule.category} • استخدام اليوم: ${formatMinutesArabic(rule.usedTodayMinutes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = rule.isAllowed && !rule.isRestricted,
                                onCheckedChange = { onToggleAllowed(rule) }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "الحد اليومي: ${rule.dailyLimitMinutes} د",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onUpdateAppLimit(rule, (rule.dailyLimitMinutes - 15).coerceAtLeast(15)) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) { Text("-15د") }
                                OutlinedButton(
                                    onClick = { onUpdateAppLimit(rule, (rule.dailyLimitMinutes + 15).coerceAtMost(300)) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) { Text("+15د") }
                                if (rule.isRestricted) {
                                    TextButton(onClick = { onTestRestrictedAlert(rule.appName) }) {
                                        Text("تنبيه")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenTimeScreen(
    child: ChildProfileEntity?,
    schedules: List<ScheduleEntity>,
    timeRequests: List<TimeRequestEntity>,
    onSavePolicy: (dailyLimit: Int, studyStart: String, studyEnd: String, bedtimeStart: String, bedtimeEnd: String) -> Unit,
    onGrantExtraTime: (Int) -> Unit,
    onResolveRequest: (TimeRequestEntity, Boolean) -> Unit,
    onToggleLock: () -> Unit,
    onAddSchedule: (title: String, type: String, start: String, end: String) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isAdminActive = remember { mutableStateOf(DevicePolicyHelper.isDeviceAdminActive(context)) }

    var dailyLimit by remember(child?.dailyLimitMinutes) { mutableFloatStateOf((child?.dailyLimitMinutes ?: 180).toFloat()) }
    var studyStart by remember(child?.studyStart) { mutableStateOf(child?.studyStart ?: "16:00") }
    var studyEnd by remember(child?.studyEnd) { mutableStateOf(child?.studyEnd ?: "18:00") }
    var bedtimeStart by remember(child?.bedtimeStart) { mutableStateOf(child?.bedtimeStart ?: "21:30") }
    var bedtimeEnd by remember(child?.bedtimeEnd) { mutableStateOf(child?.bedtimeEnd ?: "06:30") }
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            WahjTopBar(
                title = "إدارة وقت الشاشة",
                subtitle = child?.name,
                onBack = onBack
            )
        }
    ) { padding ->
        if (child == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("اختر طفلاً أولاً")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Device Admin & Remote Lock Card
            WahjCard {
                Text("القفل عن بُعد (DevicePolicyManager)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "يستخدم وهج صلاحيات إدارة الجهاز الرسمية لقفل شاشة الهاتف عند انتهاء الوقت. لا يمكن فتح قفل النظام عن بُعد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            context.startActivity(DevicePolicyHelper.buildActivateAdminIntent(context))
                            isAdminActive.value = DevicePolicyHelper.isDeviceAdminActive(context)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isAdminActive.value) "صلاحية الإدارة مفعلة" else "تفعيل إدارة الجهاز")
                    }
                    Button(
                        onClick = onToggleLock,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (child.isDeviceLocked) "إلغاء القفل" else "قفل الشاشة الآن")
                    }
                }
            }

            // Daily Limit & Study / Bedtime Scheduler
            WahjCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الحد اليومي للاستخدام", style = MaterialTheme.typography.titleMedium)
                    Text(formatMinutesArabic(dailyLimit.toInt()), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = dailyLimit,
                    onValueChange = { dailyLimit = it },
                    valueRange = 30f..480f,
                    steps = 14
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("جدول وقت الدراسة", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = studyStart,
                        onValueChange = { studyStart = it },
                        label = { Text("بداية الدراسة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = studyEnd,
                        onValueChange = { studyEnd = it },
                        label = { Text("نهاية الدراسة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("جدول وقت النوم", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bedtimeStart,
                        onValueChange = { bedtimeStart = it },
                        label = { Text("بداية النوم") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bedtimeEnd,
                        onValueChange = { bedtimeEnd = it },
                        label = { Text("الاستيقاظ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onSavePolicy(dailyLimit.toInt(), studyStart, studyEnd, bedtimeStart, bedtimeEnd) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ إعدادات الوقت والجداول")
                }
            }

            // Grant Extra Time
            WahjCard {
                Text("السماح بوقت إضافي اليوم", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60).forEach { mins ->
                        FilledTonalButton(
                            onClick = { onGrantExtraTime(mins) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+$mins د")
                        }
                    }
                }
            }

            // Pending child requests
            val pending = timeRequests.filter { it.status == RequestStatus.PENDING.name }
            if (pending.isNotEmpty()) {
                WahjCard {
                    Text("طلبات الوقت الإضافي الواردة", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    pending.forEach { req ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("+${req.requestedMinutes} دقيقة", style = MaterialTheme.typography.titleSmall)
                                Text(req.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { onResolveRequest(req, false) }) { Text("رفض") }
                                Button(onClick = { onResolveRequest(req, true) }) { Text("موافقة") }
                            }
                        }
                    }
                }
            }

            // Daily schedules
            WahjCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الجدول اليومي المخصص", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showAddScheduleDialog = true }) {
                        Text("+ إضافة فترة")
                    }
                }
                schedules.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${item.startTime} - ${item.endTime} • ${item.daysSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteSchedule(item.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "حذف الفترة")
                        }
                    }
                }
            }
        }
    }

    if (showAddScheduleDialog) {
        var title by remember { mutableStateOf("") }
        var start by remember { mutableStateOf("19:00") }
        var end by remember { mutableStateOf("20:00") }
        AlertDialog(
            onDismissRequest = { showAddScheduleDialog = false },
            title = { Text("إضافة فترة في الجدول اليومي") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("اسم الفترة") })
                    OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("وقت البداية") })
                    OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("وقت النهاية") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        onAddSchedule(title, ScheduleType.STUDY.name, start, end)
                        showAddScheduleDialog = false
                    }
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showAddScheduleDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
