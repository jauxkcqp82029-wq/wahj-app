package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ScheduleSend
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
import com.example.system.LocationHelper
import com.example.ui.components.*

private val PRESET_PARENT_MESSAGES = listOf(
    "حان وقت الدراسة.",
    "اترك الهاتف وابدأ واجباتك.",
    "حان وقت النوم.",
    "ممتاز! استمر.",
    "وقت الراحة.",
    "اتصل بي عندما تستطيع."
)

@Composable
fun MessagesScreen(
    childName: String,
    currentRole: AccountRole,
    messages: List<MessageEntity>,
    onSendMessage: (content: String, isPreset: Boolean, delayMinutes: Int) -> Unit,
    onMarkRead: () -> Unit,
    onBack: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var scheduleMinutes by remember { mutableIntStateOf(0) }
    var showSchedulePicker by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        onMarkRead()
    }

    Scaffold(
        topBar = {
            WahjTopBar(
                title = "الرسائل العائلية",
                subtitle = "التواصل مع $childName",
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (currentRole == AccountRole.PARENT) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PRESET_PARENT_MESSAGES.forEach { preset ->
                                SuggestionChip(
                                    onClick = { onSendMessage(preset, true, scheduleMinutes) },
                                    label = { Text(preset) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentRole == AccountRole.PARENT) {
                            IconButton(
                                onClick = { showSchedulePicker = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    if (scheduleMinutes > 0) Icons.AutoMirrored.Outlined.ScheduleSend else Icons.Outlined.Schedule,
                                    contentDescription = "جدولة الرسالة",
                                    tint = if (scheduleMinutes > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            placeholder = {
                                Text(
                                    if (scheduleMinutes > 0) "رسالة مجدولة بعد $scheduleMinutes د..."
                                    else "اكتب رسالة..."
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input"),
                            shape = RoundedCornerShape(14.dp)
                        )
                        FilledIconButton(
                            onClick = {
                                if (draft.isNotBlank()) {
                                    onSendMessage(draft, false, scheduleMinutes)
                                    draft = ""
                                    scheduleMinutes = 0
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("send_message_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    WahjCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("ابدأ المحادثة مع $childName", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "تصلك والطفل إشعارات فورية باسم وهج مع حالة الاستلام والقراءة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.senderRole == currentRole.name
                    val statusLabel = when (msg.status) {
                        MessageStatus.SCHEDULED.name -> "مجدولة"
                        MessageStatus.SENT.name -> "تم الإرسال"
                        MessageStatus.DELIVERED.name -> "وصلت ✓"
                        MessageStatus.READ.name -> "تمت القراءة ✓✓"
                        else -> ""
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(msg.content, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${formatClockTime(msg.createdAt)} • $statusLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSchedulePicker) {
        AlertDialog(
            onDismissRequest = { showSchedulePicker = false },
            title = { Text("جدولة إرسال الرسالة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "إرسال فوري", 5 to "بعد 5 دقائق", 15 to "بعد 15 دقيقة", 30 to "بعد 30 دقيقة", 60 to "بعد ساعة").forEach { (mins, label) ->
                        FilterChip(
                            selected = scheduleMinutes == mins,
                            onClick = {
                                scheduleMinutes = mins
                                showSchedulePicker = false
                            },
                            label = { Text(label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSchedulePicker = false }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
fun LocationScreen(
    child: ChildProfileEntity?,
    safeZones: List<SafeZoneEntity>,
    history: List<LocationHistoryEntity>,
    onRefreshLocation: () -> Unit,
    onToggleSharing: (Boolean) -> Unit,
    onAddSafeZone: (name: String, lat: Double, lng: Double, radius: Int) -> Unit,
    onToggleSafeZone: (SafeZoneEntity) -> Unit,
    onDeleteSafeZone: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showAddZoneDialog by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            onRefreshLocation()
        }
    }

    Scaffold(
        topBar = {
            WahjTopBar(
                title = "الموقع الجغرافي",
                subtitle = child?.name,
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            onRefreshLocation()
                        } else {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    }) {
                        Icon(Icons.Outlined.MyLocation, contentDescription = "تحديث الموقع")
                    }
                }
            )
        }
    ) { padding ->
        if (child == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("اختر طفلاً لعرض موقعه")
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
            InteractiveFamilyMap(
                childLat = child.lastLatitude,
                childLng = child.lastLongitude,
                childName = child.name,
                safeZones = safeZones,
                history = history
            )

            WahjCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("آخر موقع معروف", style = MaterialTheme.typography.titleSmall)
                        Text(child.lastAddress, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "آخر تحديث: ${formatTimestampArabic(child.locationUpdatedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            LocationHelper.openExternalMap(
                                context,
                                child.lastLatitude,
                                child.lastLongitude,
                                child.name
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("الخرائط")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("حالة مشاركة الموقع", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "يظهر للطفل مؤشر واضح دائماً عند تفعيل مشاركة الموقع.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = child.locationSharingEnabled,
                        onCheckedChange = onToggleSharing
                    )
                }
            }

            // Safe Zones
            WahjCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المناطق الآمنة", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showAddZoneDialog = true }) {
                        Text("+ إضافة منطقة")
                    }
                }
                if (safeZones.isEmpty()) {
                    Text(
                        "لم تتم إضافة مناطق آمنة بعد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    safeZones.forEach { zone ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(zone.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "نطاق ${zone.radiusMeters} متر • تنبيه الوصول والمغادرة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = zone.isActive, onCheckedChange = { onToggleSafeZone(zone) })
                            IconButton(onClick = { onDeleteSafeZone(zone.id) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "حذف المنطقة")
                            }
                        }
                    }
                }
            }

            // Location History
            WahjCard {
                Text("سجل المواقع المسموح به", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (history.isEmpty()) {
                    Text(
                        "لا توجد نقاط مسجلة بعد. اضغط على زر التحديث لقراءة الموقع الحالي عند توفر الإذن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    history.take(8).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(entry.address, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(formatTimestampArabic(entry.recordedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showAddZoneDialog && child != null) {
        var name by remember { mutableStateOf("") }
        var radius by remember { mutableStateOf("250") }
        AlertDialog(
            onDismissRequest = { showAddZoneDialog = false },
            title = { Text("إضافة منطقة آمنة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المنطقة (مثلاً: المدرسة)") }
                    )
                    OutlinedTextField(
                        value = radius,
                        onValueChange = { radius = it },
                        label = { Text("نصف القطر بالمتر") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        onAddSafeZone(
                            name,
                            child.lastLatitude,
                            child.lastLongitude,
                            radius.toIntOrNull()?.coerceIn(50, 5000) ?: 250
                        )
                        showAddZoneDialog = false
                    }
                }) { Text("إضافة") }
            },
            dismissButton = {
                TextButton(onClick = { showAddZoneDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
