package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
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
import com.example.system.UsageStatsHelper
import com.example.ui.components.*

@Composable
fun ChildDashboardScreen(
    child: ChildProfileEntity?,
    messages: List<MessageEntity>,
    appRules: List<AppRuleEntity>,
    schedules: List<ScheduleEntity>,
    onSyncNow: () -> Unit,
    onRequestExtraTime: (minutes: Int, reason: String) -> Unit,
    onOpenMessages: () -> Unit,
    onOpenQrPairing: () -> Unit,
    onToggleLocationSharing: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var showExtraTimeDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onSyncNow()
    }

    Scaffold(
        topBar = {
            WahjTopBar(
                title = "لوحة الطفل — وهج",
                subtitle = child?.let { "مرحباً ${it.name}" } ?: "غير مرتبط بعد",
                actions = {
                    IconButton(onClick = onSyncNow) {
                        Icon(Icons.Outlined.Sync, contentDescription = "مزامنة البيانات")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "الإعدادات")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (child == null) {
                WahjCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WahjLineArtLogo(size = 84.dp)
                        Text("هذا الجهاز غير مرتبط بعائلة بعد", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "أنشئ رمز QR مؤقتاً ليقوم ولي الأمر بمسحه والموافقة على الربط.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onOpenQrPairing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إنشاء رمز QR للربط")
                        }
                    }
                }
                return@Column
            }

            // Post-pairing friendly welcome banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Face, contentDescription = child.name, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(child.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "ههههه، روح ادرس يا بطل! ❤️",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Transparent Location Sharing Indicator (Required by spec & privacy rules)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (child.locationSharingEnabled) Icons.Filled.LocationOn else Icons.Outlined.LocationOff,
                            contentDescription = "حالة مشاركة الموقع",
                            tint = if (child.locationSharingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (child.locationSharingEnabled)
                                "مشاركة الموقع مع ولي الأمر مفعلة بشفافية"
                            else "مشاركة الموقع متوقفة حالياً",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = child.locationSharingEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !LocationHelper.hasLocationPermission(context)) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                            onToggleLocationSharing(enabled)
                        }
                    )
                }
            }

            // Remaining Screen Time & Bedtime Card
            val totalAllowed = child.dailyLimitMinutes + child.bonusMinutesToday
            val remaining = (totalAllowed - child.usedTodayMinutes).coerceAtLeast(0)
            val ratio = if (totalAllowed > 0) (remaining.toFloat() / totalAllowed.toFloat()).coerceIn(0f, 1f) else 0f

            WahjCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الوقت المتبقي للاستخدام", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            formatMinutesArabic(remaining),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "المستخدم اليوم: ${formatMinutesArabic(child.usedTodayMinutes)} من ${formatMinutesArabic(totalAllowed)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ComplianceRingChart(
                        percentage = (ratio * 100).toInt(),
                        label = "متبقي"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showExtraTimeDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("child_request_extra_time_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.MoreTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طلب وقت إضافي")
                    }
                    OutlinedButton(
                        onClick = onOpenMessages,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("child_contact_parent_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تواصل مع ولي الأمر")
                    }
                }
            }

            // Study & Bedtime Schedule
            WahjCard {
                Text("جدول الدراسة ووقت النوم", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("وقت الدراسة", style = MaterialTheme.typography.labelMedium)
                            Text("${child.studyStart} - ${child.studyEnd}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("وقت النوم", style = MaterialTheme.typography.labelMedium)
                            Text("${child.bedtimeStart} - ${child.bedtimeEnd}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (schedules.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    schedules.forEach { sch ->
                        Text(
                            "• ${sch.title}: ${sch.startTime} - ${sch.endTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Incoming Messages Preview
            WahjCard(onClick = onOpenMessages) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الرسائل الواردة من ولي الأمر", style = MaterialTheme.typography.titleMedium)
                    Text("عرض الكل", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                val incoming = messages.filter { it.senderRole == AccountRole.PARENT.name }.takeLast(3)
                if (incoming.isEmpty()) {
                    Text(
                        "لا توجد رسائل واردة حالياً",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    incoming.forEach { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(msg.content, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text(formatClockTime(msg.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Allowed Apps
            WahjCard {
                Text("التطبيقات المسموحة", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val allowedApps = appRules.filter { it.isAllowed && !it.isRestricted }.take(8)
                if (allowedApps.isEmpty()) {
                    Text(
                        "سيتم عرض التطبيقات المسموحة بمجرد منح إذن إحصائيات الاستخدام ومزامنة الجهاز.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    allowedApps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(app.appName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "حد ${app.dailyLimitMinutes}د",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Official Android Permissions Setup (Transparent & Explicit)
            WahjCard {
                Text("صلاحيات الجهاز الرسمية", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { context.startActivity(UsageStatsHelper.buildUsageAccessSettingsIntent()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إذن الاستخدام")
                    }
                    OutlinedButton(
                        onClick = { context.startActivity(DevicePolicyHelper.buildActivateAdminIntent(context)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إدارة الجهاز")
                    }
                }
            }
        }
    }

    if (showExtraTimeDialog) {
        var minutes by remember { mutableIntStateOf(15) }
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showExtraTimeDialog = false },
            title = { Text("طلب وقت إضافي") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 45).forEach { m ->
                            FilterChip(
                                selected = minutes == m,
                                onClick = { minutes = m },
                                label = { Text("$m د") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("السبب (مثلاً: إنهاء واجب العلوم)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRequestExtraTime(minutes, reason)
                    showExtraTimeDialog = false
                }) {
                    Text("إرسال الطلب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExtraTimeDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
