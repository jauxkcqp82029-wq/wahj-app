package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.local.AccountRole
import com.example.data.local.WahjSessionPrefs
import com.example.ui.components.WahjCard
import com.example.ui.components.WahjLineArtLogo
import com.example.ui.components.WahjTopBar
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    sessionPrefs: WahjSessionPrefs,
    onNavigateToWelcome: () -> Unit,
    onNavigateToParent: () -> Unit,
    onNavigateToChild: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "splash_logo_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        label = "splash_alpha"
    )

    LaunchedEffect(sessionPrefs.activeAccountId, sessionPrefs.activeRole) {
        visible = true
        delay(1100)
        when {
            sessionPrefs.activeAccountId != null && sessionPrefs.activeRole == AccountRole.PARENT.name ->
                onNavigateToParent()
            sessionPrefs.activeAccountId != null && sessionPrefs.activeRole == AccountRole.CHILD.name ->
                onNavigateToChild()
            else -> onNavigateToWelcome()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .padding(24.dp)
        ) {
            WahjLineArtLogo(size = 112.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "وهج | WAHJ",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "رعاية ذكية تمنح عائلتك الأمان والتوازن",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WelcomeScreen(
    onStartRoleSelection: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                WahjLineArtLogo(size = 42.dp)
                Column {
                    Text(
                        text = "وهج",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Smart Family Control",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Image(
                painter = painterResource(id = R.drawable.img_wahj_welcome),
                contentDescription = "رعاية ذكية للعائلة عبر تطبيق وهج",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "توازن رقمي آمن لعائلتك",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "راقب أوقات الدراسة والنوم، نظم التطبيقات، وابقَ على تواصل فوري مع أطفالك بشفافية كاملة واحترام للخصوصية.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            WahjFeatureRow(
                icon = Icons.Outlined.QrCode2,
                title = "ربط مشفر برمز QR مؤقت",
                desc = "مصادقة أحادية الاستخدام وموافقة صريحة بين جهاز ولي الأمر والطفل."
            )
            Spacer(modifier = Modifier.height(10.dp))
            WahjFeatureRow(
                icon = Icons.Outlined.Schedule,
                title = "جداول الدراسة ووقت النوم",
                desc = "تحكم بمدة الاستخدام اليومي وامنح وقتاً إضافياً بلمسة واحدة."
            )
            Spacer(modifier = Modifier.height(10.dp))
            WahjFeatureRow(
                icon = Icons.Outlined.Shield,
                title = "صلاحيات Android الرسمية والشفافة",
                desc = "دون تجسس أو إخفاء للتطبيق، مع مؤشر واضح لمشاركة الموقع."
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onStartRoleSelection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("welcome_start_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("ابدأ الآن", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("welcome_login_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("لدي حساب بالفعل — تسجيل الدخول")
            }
        }
    }
}

@Composable
private fun WahjFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    WahjCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun RoleSelectionScreen(
    onSelectParent: () -> Unit,
    onSelectChild: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { WahjTopBar(title = "اختيار نوع الحساب", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "من سيستخدم هذا الجهاز؟",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "تختلف الصلاحيات والواجهة حسب نوع الحساب المختار لضمان الفصل التام بين إدارة ولي الأمر وتجربة الطفل.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            WahjCard(
                onClick = onSelectParent,
                modifier = Modifier.testTag("role_parent_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = "حساب ولي أمر",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ولي أمر", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "إضافة أكثر من طفل، مسح QR للربط، إدارة التطبيقات ووقت الشاشة، الخريطة والتقارير.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            WahjCard(
                onClick = onSelectChild,
                modifier = Modifier.testTag("role_child_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = "حساب طفل",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("طفل", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "إنشاء رمز QR للربط، متابعة الوقت المتبقي وجدول الدراسة، الرسائل الواردة، وطلب وقت إضافي.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = "حساب الطفل معزول تماماً ولا يمكنه الوصول إلى إعدادات ولي الأمر أو تعديل قيود الأجهزة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    existingAccounts: List<AccountEntity>,
    authError: String?,
    onLogin: (email: String, password: String) -> Unit,
    onQuickSwitchAccount: (AccountEntity) -> Unit,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { WahjTopBar(title = "تسجيل الدخول", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "مرحباً بعودتك إلى وهج",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "أدخل بيانات حسابك للوصول إلى لوحة العائلة والأجهزة المرتبطة.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني") },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_email_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "إظهار أو إخفاء كلمة المرور"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input"),
                shape = RoundedCornerShape(14.dp)
            )

            AnimatedVisibility(visible = !authError.isNullOrBlank(), enter = fadeIn()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = authError.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }

            Button(
                onClick = { onLogin(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_submit_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("تسجيل الدخول", style = MaterialTheme.typography.titleMedium)
            }

            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("ليس لديك حساب؟ أنشئ حساباً جديداً")
            }

            if (existingAccounts.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "الحسابات المسجلة على هذا الجهاز",
                    style = MaterialTheme.typography.titleSmall
                )
                existingAccounts.forEach { acc ->
                    WahjCard(onClick = { onQuickSwitchAccount(acc) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(acc.fullName, style = MaterialTheme.typography.titleSmall)
                                Text(acc.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            AssistChip(
                                onClick = { onQuickSwitchAccount(acc) },
                                label = {
                                    Text(if (acc.role == AccountRole.PARENT.name) "ولي أمر" else "طفل")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    initialRole: AccountRole,
    authError: String?,
    onRegister: (fullName: String, email: String, password: String, role: AccountRole) -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(initialRole) }
    var localError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { WahjTopBar(title = "إنشاء حساب جديد", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "إنشاء حساب في وهج",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedRole == AccountRole.PARENT,
                    onClick = { selectedRole = AccountRole.PARENT },
                    label = { Text("حساب ولي أمر") },
                    leadingIcon = { Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedRole == AccountRole.CHILD,
                    onClick = { selectedRole = AccountRole.CHILD },
                    label = { Text("حساب طفل") },
                    leadingIcon = { Icon(Icons.Outlined.Face, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("الاسم الكامل") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_name_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني") },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_email_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور (6 أحرف فأكثر)") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_password_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("تأكيد كلمة المرور") },
                leadingIcon = { Icon(Icons.Outlined.VerifiedUser, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            val displayError = localError ?: authError
            if (!displayError.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = displayError,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (password != confirmPassword) {
                        localError = "كلمتا المرور غير متطابقتين"
                    } else {
                        localError = null
                        onRegister(fullName, email, password, selectedRole)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("register_submit_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("إنشاء الحساب والمتابعة", style = MaterialTheme.typography.titleMedium)
            }

            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("لديك حساب مسجل؟ سجل الدخول")
            }
        }
    }
}
