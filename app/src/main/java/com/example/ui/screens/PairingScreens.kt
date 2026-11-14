package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.repository.PairApprovedDto
import com.example.data.security.WahjPairingPayload
import com.example.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun ChildQrPairingScreen(
    generatedQr: Pair<WahjPairingPayload, String>?,
    pendingApproval: PairApprovedDto?,
    onGenerateQr: (childName: String, deviceType: String) -> Unit,
    onPairingApproved: (childId: String) -> Unit,
    onOpenChildDashboard: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var childName by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf("هاتف") }
    var remainingSeconds by remember { mutableIntStateOf(300) }
    var copiedFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(pendingApproval) {
        if (pendingApproval != null) {
            onPairingApproved(pendingApproval.childId)
        }
    }

    LaunchedEffect(generatedQr?.first?.issuedAtMs) {
        val payload = generatedQr?.first ?: return@LaunchedEffect
        while (true) {
            val left = ((payload.expiresAtMs - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
            remainingSeconds = left
            if (left == 0) break
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            WahjTopBar(
                title = "ربط جهاز الطفل",
                subtitle = "رمز QR مؤقت وآمن",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WahjCard {
                Text("1. بيانات جهاز الطفل", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = childName,
                    onValueChange = { childName = it },
                    label = { Text("اسم الطفل") },
                    placeholder = { Text("مثال: عمر") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("child_qr_name_input"),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("نوع الجهاز", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = deviceType == "هاتف",
                        onClick = { deviceType = "هاتف" },
                        label = { Text("هاتف ذكي") },
                        leadingIcon = { Icon(Icons.Outlined.Smartphone, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = deviceType == "تابلت",
                        onClick = { deviceType = "تابلت" },
                        label = { Text("جهاز لوحي (تابلت)") },
                        leadingIcon = { Icon(Icons.Outlined.TabletAndroid, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { onGenerateQr(childName, deviceType) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_qr_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (generatedQr == null) "إنشاء رمز QR آمن" else "تجديد رمز QR")
                }
            }

            if (generatedQr != null) {
                val (payload, rawToken) = generatedQr
                WahjCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("2. امسح الرمز من جهاز ولي الأمر", style = MaterialTheme.typography.titleMedium)
                            WahjStatusBadge(
                                text = if (remainingSeconds > 0) "صالح ${remainingSeconds / 60}:${"%02d".format(remainingSeconds % 60)}" else "منتهي الصلاحية",
                                isPositive = remainingSeconds > 0
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        QrCodeBitmapView(content = rawToken, sizeDp = 220.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "بانتظار مسح الرمز وموافقة ولي الأمر...\nرمز أحادي الاستخدام ومحمي بتحقق التوقيع الرقمي.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "الشبكة المحلية: ${payload.hostIp}:${payload.port}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("WAHJ QR", rawToken))
                                copiedFeedback = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (copiedFeedback) "تم نسخ رمز الربط المشفر" else "نسخ كود الربط (للتجربة على نفس الجهاز)")
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenChildDashboard,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("الانتقال إلى لوحة الطفل")
            }
        }
    }
}

@Composable
fun ParentQrScannerScreen(
    scannedPayload: WahjPairingPayload?,
    qrError: String?,
    lastGeneratedQrRaw: String?,
    onQrScanned: (String) -> Unit,
    onConfirmPairing: (childName: String, relation: String) -> Unit,
    onResetScan: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var manualCode by remember { mutableStateOf("") }
    var childNameInput by remember(scannedPayload) { mutableStateOf(scannedPayload?.childName ?: "") }
    var relation by remember { mutableStateOf("ابن") }
    val relations = listOf("ابن", "ابنة", "أخ", "أخت")

    Scaffold(
        topBar = {
            WahjTopBar(
                title = "مسح رمز QR للطفل",
                subtitle = "إضافة طفل وربط جهازه",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (scannedPayload == null) {
                WahjCard {
                    Text("توجيه الكاميرا نحو رمز QR في جهاز الطفل", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (hasCameraPermission) {
                        CameraQrScannerView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            onQrDetected = onQrScanned
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "يتطلب مسح QR إذن الكاميرا لقراءة الرمز المؤقت من شاشة الطفل.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("السماح باستخدام الكاميرا")
                            }
                        }
                    }
                }

                WahjCard {
                    Text("أو لصق كود الربط المشفر يدوياً", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        label = { Text("كود الربط المشفر (JSON)") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_qr_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onQrScanned(manualCode) },
                            enabled = manualCode.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تحقق من الرمز")
                        }
                        if (!lastGeneratedQrRaw.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    manualCode = lastGeneratedQrRaw
                                    onQrScanned(lastGeneratedQrRaw)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("استخدام آخر QR محلي")
                            }
                        }
                    }
                }
            } else {
                // Step 5-7: Parent confirms child name, relation, inspects device info, and approves pairing
                WahjCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("تم التحقق من رمز QR الآمن بنجاح", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoLine("الجهاز", scannedPayload.deviceModel)
                    InfoLine("النوع", scannedPayload.deviceType)
                    InfoLine("النظام", scannedPayload.osVersion)
                    InfoLine("عنوان الشبكة", "${scannedPayload.hostIp}:${scannedPayload.port}")
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = childNameInput,
                        onValueChange = { childNameInput = it },
                        label = { Text("اسم الطفل") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_child_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("صلة القرابة", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        relations.forEach { rel ->
                            FilterChip(
                                selected = relation == rel,
                                onClick = { relation = rel },
                                label = { Text(rel) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onConfirmPairing(childNameInput, relation) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_pairing_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("تأكيد الربط وإضافة الطفل")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onResetScan,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("مسح رمز آخر")
                    }
                }
            }

            if (!qrError.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = qrError,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PairingSuccessScreen(
    childName: String,
    deviceName: String,
    isChildFlow: Boolean,
    onContinue: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WahjLineArtLogo(size = 96.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "نجاح الربط",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "تم ربط الجهاز بنجاح",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$childName — $deviceName",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isChildFlow) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "ههههه، روح ادرس يا بطل! ❤️",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("pairing_success_continue_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isChildFlow) "فتح لوحة الطفل" else "الانتقال إلى لوحة التحكم")
            }
        }
    }
}
