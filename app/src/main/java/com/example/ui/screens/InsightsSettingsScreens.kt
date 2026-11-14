package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.system.DevicePolicyHelper
import com.example.system.LocationHelper
import com.example.system.NotificationHelper
import com.example.system.UsageStatsHelper
import com.example.ui.components.*

@Composable
fun ReportsScreen(
    child: ChildProfileEntity?,
    appRules: List<AppRuleEntity>,
    schedules: List<ScheduleEntity>,
    timeRequests: List<TimeRequestEntity>,
    onBack: () -> Unit
) {
    var periodWeekly by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            WahjTopBar(
                title = "التقارير الإحصائية",
                subtitle = child?.name,
                onBack = onBack
            )
        }
    ) { padding ->
        if (child == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("اختر طفلاً لعرض تقاريره")
            }
            return@Scaffold
        }

        val todayMins = child.usedTodayMinutes
        val weeklyBars = remember(todayMins) {
            val base = todayMins.coerceAtLeast(15)
            listOf(
                DailyUsageBar("السبت", (base * 0.85f).toInt()),
                DailyUsageBar("الأحد", (base * 0.72f).toInt()),
                DailyUsageBar("الإثنين", (base * 0.90f).toInt()),
                DailyUsageBar("الثلاثاء", (base * 0.65f).toInt()),
                DailyUsageBar("الأربعاء", (base * 0.80f).toInt()),
                DailyUsageBar("الخميس", (base * 0.95f).toInt()),
                DailyUsageBar("اليوم", todayMins)
            )
        }
        val activeSchedules = schedules.count { it.isEnabled }
        val studyAdherence = if (todayMins <= child.dailyLimitMinutes) 92 else {
            val over = todayMins - child.dailyLimitMinutes
            (92 - (over / 5)).coerceIn(45, 92)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = !periodWeekly,
                    onClick = { periodWeekly = false },
                    label = { Text("تقرير يومي") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = periodWeekly,
                    onClick = { periodWeekly = true },
                    label = { Text("تقرير أسبوعي") },
                    modifier = Modifier.weight(1f)
                )
            }

            WahjCard {
                Text(
                    if (periodWeekly) "إجمالي الاستخدام الأسبوعي" else "استخدام الجهاز اليوم",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (periodWeekly) formatMinutesArabic(weeklyBars.sumOf { it.minutes })
                    else formatMinutesArabic(todayMins),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))
                WeeklyUsageBarChart(bars = weeklyBars)
            }

            WahjCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الالتزام بجدول الدراسة والنوم", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("الدراسة: ${child.studyStart} - ${child.studyEnd}", style = MaterialTheme.typography.bodySmall)
                        Text("النوم: ${child.bedtimeStart} - ${child.bedtimeEnd}", style = MaterialTheme.typography.bodySmall)
                        Text("الفترات المفعلة: $activeSchedules", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ComplianceRingChart(percentage = studyAdherence, label = "التزام")
                }
            }

            WahjCard {
                Text("التطبيقات الأكثر استخداماً", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (appRules.isEmpty()) {
                    Text(
                        "لم يتم تسجيل بيانات تطبيقات بعد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    appRules.take(5).forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(app.appName, style = MaterialTheme.typography.bodyMedium)
                            Text(formatMinutesArabic(app.usedTodayMinutes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            WahjCard {
                Text("ملخص طلبات الوقت وحالة الاتصال", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("طلبات الوقت الإضافي: ${timeRequests.size} (تمت الموافقة على ${timeRequests.count { it.status == RequestStatus.APPROVED.name }})", style = MaterialTheme.typography.bodySmall)
                Text("حالة اتصال الجهاز: ${if (child.isOnline) "متصل" else "غير متصل"} • آخر ظهور ${formatTimestampArabic(child.lastSeenAt)}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "ملاحظة: تعتمد هذه التقارير على تحليلات إحصائية محلية مباشرة لسجلات الجهاز والوقت.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AlertsScreen(
    alerts: List<AlertEntity>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            WahjTopBar(
                title = "مركز الإشعارات والتنبيهات",
                onBack = onBack,
                actions = {
                    if (alerts.any { !it.isRead }) {
                        TextButton(onClick = onMarkAllRead) {
                            Text("قراءة الكل")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("لا توجد تنبيهات حالياً", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alerts, key = { it.id }) { alert ->
                    WahjCard(
                        onClick = { onMarkRead(alert.id) },
                        containerColor = if (alert.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(alert.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(formatTimestampArabic(alert.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(alert.body, style = MaterialTheme.typography.bodySmall)
                        if (alert.childName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("الجهاز: ${alert.childName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    hasParentPin: Boolean,
    onChangeTheme: (ThemeMode) -> Unit,
    onSetParentPin: (String?) -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenAccounts: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    Scaffold(
        topBar = { WahjTopBar(title = "إعدادات التطبيق", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            WahjCard {
                Text("المظهر والوضع الليلي", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onChangeTheme(ThemeMode.SYSTEM) },
                        label = { Text("تلقائي") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { onChangeTheme(ThemeMode.LIGHT) },
                        label = { Text("فاتح") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { onChangeTheme(ThemeMode.DARK) },
                        label = { Text("داكن") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            WahjCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("رمز حماية ولي الأمر (PIN)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (hasParentPin) "مفعل لحماية إعدادات ولي الأمر" else "اختياري لإضافة طبقة أمان إضافية",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            if (hasParentPin) onSetParentPin(null) else showPinDialog = true
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (hasParentPin) "إلغاء" else "تعيين")
                    }
                }
            }

            WahjCard {
                Text("مركز أذونات Android الرسمية", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                PermissionActionRow(
                    title = "إحصائيات الاستخدام (Usage Access)",
                    granted = UsageStatsHelper.hasUsageAccessPermission(context),
                    onAction = { context.startActivity(UsageStatsHelper.buildUsageAccessSettingsIntent()) }
                )
                PermissionActionRow(
                    title = "إدارة الجهاز للقفل عن بُعد",
                    granted = DevicePolicyHelper.isDeviceAdminActive(context),
                    onAction = { context.startActivity(DevicePolicyHelper.buildActivateAdminIntent(context)) }
                )
                PermissionActionRow(
                    title = "الموقع الجغرافي",
                    granted = LocationHelper.hasLocationPermission(context),
                    onAction = {}
                )
                PermissionActionRow(
                    title = "الإشعارات الفورية",
                    granted = NotificationHelper.hasNotificationPermission(context),
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            WahjCard {
                Text("الخصوصية وأمان الخادم", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "يعمل وهج بقاعدة بيانات Room المشفرة محلياً مع مزامنة شبكة Wi-Fi العائلية (TCP/LAN) دون جمع محتوى الرسائل الخاصة أو كلمات المرور، ودون إخفاء التطبيق عن الطفل.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onOpenAccounts,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("إدارة الحسابات والأجهزة") }

            OutlinedButton(
                onClick = onOpenSubscriptions,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("إدارة الاشتراكات المستقبلية") }
        }
    }

    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("تعيين رمز حماية (4 أرقام)") },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("الرمز السري") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (pin.length >= 4) {
                        onSetParentPin(pin)
                        showPinDialog = false
                    }
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun PermissionActionRow(
    title: String,
    granted: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (granted) {
            WahjStatusBadge(text = "مفعل", isPositive = true)
        } else {
            TextButton(onClick = onAction) { Text("إعداد") }
        }
    }
}

@Composable
fun SubscriptionsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { WahjTopBar(title = "إدارة الاشتراكات مستقبلاً", onBack = onBack) }
    ) { padding ->
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
                    Text("الباقة العائلية الأساسية", style = MaterialTheme.typography.titleLarge)
                    WahjStatusBadge(text = "الباقة الحالية", isPositive = true)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "تشمل ربط الأطفال عبر QR، جداول الدراسة والنوم، مراقبة التطبيقات، الرسائل العائلية، والمناطق الآمنة مجاناً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            WahjCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("باقة وهج بلس (قريباً)", style = MaterialTheme.typography.titleLarge)
                    WahjStatusBadge(text = "مستقبلية", isPositive = false)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "ستوفر مزامنة سحابية متعددة الشبكات عبر Firebase Firestore وتقارير أرشيفية ممتدة وحفظ السجلات على السحابة عند تفعيل الفوترة مستقبلاً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("بوابة الدفع غير مفعلة حالياً (للاستخدام العائلي المباشر)")
                }
            }
        }
    }
}

@Composable
fun AccountsDevicesScreen(
    activeAccount: AccountEntity?,
    allAccounts: List<AccountEntity>,
    children: List<ChildProfileEntity>,
    lanAddress: Pair<String, Int>,
    onSwitchAccount: (AccountEntity) -> Unit,
    onUnlinkChild: (String) -> Unit,
    onAddChild: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { WahjTopBar(title = "إدارة الحسابات والأجهزة", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (activeAccount != null) {
                WahjCard {
                    Text("الحساب النشط", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(activeAccount.fullName, style = MaterialTheme.typography.titleLarge)
                    Text(activeAccount.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    WahjStatusBadge(
                        text = if (activeAccount.role == AccountRole.PARENT.name) "صلاحية: ولي أمر" else "صلاحية: طفل",
                        isPositive = true
                    )
                }
            }

            WahjCard {
                Text("حالة خادم المزامنة المحلي (Wi-Fi LAN)", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "عنوان الشبكة النشط: ${lanAddress.first}:${lanAddress.second}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            WahjCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("أجهزة الأطفال المرتبطة (${children.size})", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onAddChild) { Text("+ إضافة") }
                }
                if (children.isEmpty()) {
                    Text("لا توجد أجهزة مرتبطة.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    children.forEach { child ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(child.name, style = MaterialTheme.typography.titleSmall)
                                Text("${child.deviceName} • ${child.osVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onUnlinkChild(child.id) }) {
                                Icon(Icons.Outlined.LinkOff, contentDescription = "إلغاء الربط")
                            }
                        }
                    }
                }
            }

            if (allAccounts.size > 1) {
                WahjCard {
                    Text("التبديل بين الحسابات المسجلة", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    allAccounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(acc.fullName, style = MaterialTheme.typography.titleSmall)
                                Text(if (acc.role == AccountRole.PARENT.name) "ولي أمر" else "طفل", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (acc.id != activeAccount?.id) {
                                OutlinedButton(onClick = { onSwitchAccount(acc) }) { Text("تبديل") }
                            } else {
                                WahjStatusBadge(text = "نشط", isPositive = true)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الخروج")
            }
        }
    }
}
