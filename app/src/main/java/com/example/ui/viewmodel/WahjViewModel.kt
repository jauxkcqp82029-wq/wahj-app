package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.PairApprovedDto
import com.example.data.repository.WahjRepository
import com.example.data.security.WahjPairingPayload
import com.example.system.RealAppUsageStat
import com.example.system.UsageStatsHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WahjViewModel(
    private val appContext: Context,
    private val repository: WahjRepository
) : ViewModel() {

    val sessionPrefs: StateFlow<WahjSessionPrefs> = repository.sessionPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WahjSessionPrefs())

    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAccount: StateFlow<AccountEntity?> = combine(sessionPrefs, accounts) { prefs, list ->
        list.firstOrNull { it.id == prefs.activeAccountId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val children: StateFlow<List<ChildProfileEntity>> = combine(
        sessionPrefs,
        repository.observeAllChildren()
    ) { prefs, all ->
        val uid = prefs.activeAccountId
        if (uid != null && prefs.activeRole == AccountRole.PARENT.name) {
            val owned = all.filter { it.parentId == uid }
            if (owned.isNotEmpty()) owned else all
        } else {
            all
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<AlertEntity>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTimeRequests: StateFlow<List<TimeRequestEntity>> = repository.allTimeRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingPairingApproval: StateFlow<PairApprovedDto?> = repository.pendingPairingApproval

    val lastGeneratedQrRaw: StateFlow<String?> = repository.lastGeneratedQrRaw

    private val _selectedChildId = MutableStateFlow<String?>(null)
    val selectedChild: StateFlow<ChildProfileEntity?> = combine(
        _selectedChildId,
        sessionPrefs,
        children
    ) { explicitId, prefs, list ->
        val targetId = explicitId ?: prefs.selectedChildId ?: prefs.activeChildDeviceId ?: list.firstOrNull()?.id
        list.firstOrNull { it.id == targetId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _localDeviceApps = MutableStateFlow<List<RealAppUsageStat>>(emptyList())
    val localDeviceApps: StateFlow<List<RealAppUsageStat>> = _localDeviceApps.asStateFlow()

    // QR Pairing state
    private val _generatedQrPayload = MutableStateFlow<Pair<WahjPairingPayload, String>?>(null)
    val generatedQrPayload: StateFlow<Pair<WahjPairingPayload, String>?> = _generatedQrPayload.asStateFlow()

    private val _scannedQrPayload = MutableStateFlow<WahjPairingPayload?>(null)
    val scannedQrPayload: StateFlow<WahjPairingPayload?> = _scannedQrPayload.asStateFlow()

    private val _qrError = MutableStateFlow<String?>(null)
    val qrError: StateFlow<String?> = _qrError.asStateFlow()

    fun clearAuthError() { _authError.value = null }
    fun clearUiMessage() { _uiMessage.value = null }
    fun clearQrState() {
        _scannedQrPayload.value = null
        _qrError.value = null
    }

    fun selectChild(childId: String) {
        _selectedChildId.value = childId
        viewModelScope.launch { repository.setSelectedChild(childId) }
    }

    fun observeChild(childId: String): Flow<ChildProfileEntity?> = repository.observeChild(childId)
    fun observeAppRules(childId: String): Flow<List<AppRuleEntity>> = repository.observeAppRules(childId)
    fun observeSchedules(childId: String): Flow<List<ScheduleEntity>> = repository.observeSchedules(childId)
    fun observeMessages(childId: String): Flow<List<MessageEntity>> = repository.observeMessages(childId)
    fun observeSafeZones(childId: String): Flow<List<SafeZoneEntity>> = repository.observeSafeZones(childId)
    fun observeLocationHistory(childId: String): Flow<List<LocationHistoryEntity>> = repository.observeLocationHistory(childId)
    fun observeTimeRequests(childId: String): Flow<List<TimeRequestEntity>> = repository.observeTimeRequests(childId)

    fun refreshLocalDeviceApps() {
        viewModelScope.launch {
            _localDeviceApps.value = UsageStatsHelper.queryInstalledAndUsedApps(appContext)
        }
    }

    fun selectRole(role: AccountRole) {
        viewModelScope.launch { repository.selectRole(role) }
    }

    fun login(email: String, password: String, onSuccess: (AccountEntity) -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            repository.loginAccount(email, password)
                .onSuccess { onSuccess(it) }
                .onFailure { _authError.value = it.message ?: "تعذر تسجيل الدخول" }
        }
    }

    fun register(
        fullName: String,
        email: String,
        password: String,
        role: AccountRole,
        onSuccess: (AccountEntity) -> Unit
    ) {
        viewModelScope.launch {
            _authError.value = null
            repository.registerAccount(fullName, email, password, role)
                .onSuccess { onSuccess(it) }
                .onFailure { _authError.value = it.message ?: "تعذر إنشاء الحساب" }
        }
    }

    fun switchAccount(account: AccountEntity, onSwitched: (AccountEntity) -> Unit) {
        viewModelScope.launch {
            repository.switchActiveAccount(account)
            onSwitched(account)
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onLoggedOut()
        }
    }

    fun generateChildQr(childName: String, deviceType: String) {
        _qrError.value = null
        _generatedQrPayload.value = repository.createChildQrPayload(childName, deviceType)
    }

    fun verifyScannedQr(raw: String) {
        viewModelScope.launch {
            _qrError.value = null
            repository.parseAndVerifyQr(raw)
                .onSuccess { _scannedQrPayload.value = it }
                .onFailure { _qrError.value = it.message ?: "رمز QR غير صالح" }
        }
    }

    fun confirmParentPairing(
        childName: String,
        relation: String,
        onPaired: (ChildProfileEntity) -> Unit
    ) {
        val payload = _scannedQrPayload.value ?: return
        val parent = activeAccount.value
        val parentId = parent?.id ?: "local_parent"
        val parentName = parent?.fullName ?: "ولي الأمر"
        viewModelScope.launch {
            repository.confirmPairing(parentId, parentName, payload, childName, relation)
                .onSuccess {
                    _selectedChildId.value = it.id
                    _scannedQrPayload.value = null
                    onPaired(it)
                }
                .onFailure { _qrError.value = it.message ?: "تعذر تأكيد الربط" }
        }
    }

    fun clearPendingPairingApproval() {
        viewModelScope.launch { repository.clearPendingPairingApproval() }
    }

    fun syncChildTelemetry(childId: String) {
        viewModelScope.launch {
            repository.syncCurrentDeviceTelemetry(childId)
            refreshLocalDeviceApps()
            _uiMessage.value = "تم تحديث بيانات الجهاز والموقع والبطارية"
        }
    }

    fun toggleRemoteLock(child: ChildProfileEntity) {
        viewModelScope.launch {
            val locked = repository.toggleRemoteDeviceLock(child)
            _uiMessage.value = if (locked) {
                "تم تفعيل قفل الاستخدام على جهاز ${child.name}"
            } else {
                "تم إلغاء قفل الاستخدام لجهاز ${child.name}"
            }
        }
    }

    fun updateScreenTimePolicy(
        child: ChildProfileEntity,
        dailyLimitMinutes: Int,
        studyStart: String,
        studyEnd: String,
        bedtimeStart: String,
        bedtimeEnd: String
    ) {
        viewModelScope.launch {
            repository.updateScreenTimePolicy(
                child, dailyLimitMinutes, studyStart, studyEnd, bedtimeStart, bedtimeEnd
            )
            _uiMessage.value = "تم حفظ جدول الاستخدام وأوقات الدراسة والنوم"
        }
    }

    fun grantExtraTime(child: ChildProfileEntity, minutes: Int) {
        viewModelScope.launch {
            repository.grantExtraTime(child, minutes)
            _uiMessage.value = "تم منح $minutes دقيقة إضافية لـ ${child.name}"
        }
    }

    fun updateAppRule(rule: AppRuleEntity) {
        viewModelScope.launch { repository.updateAppRule(rule) }
    }

    fun testRestrictedAppAlert(childId: String, appName: String) {
        viewModelScope.launch {
            repository.simulateRestrictedAppAttempt(childId, appName)
            _uiMessage.value = "تم إرسال إشعار محاولة فتح تطبيق مقيد ($appName)"
        }
    }

    fun upsertSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch {
            repository.upsertSchedule(schedule)
            _uiMessage.value = "تم حفظ الجدول اليومي"
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch { repository.deleteSchedule(id) }
    }

    fun sendMessage(
        childId: String,
        senderRole: AccountRole,
        content: String,
        isPreset: Boolean = false,
        scheduleDelayMinutes: Int = 0
    ) {
        viewModelScope.launch {
            repository.sendMessage(childId, senderRole, content, isPreset, scheduleDelayMinutes)
            if (scheduleDelayMinutes > 0) {
                _uiMessage.value = "تمت جدولة الرسالة للإرسال بعد $scheduleDelayMinutes دقيقة"
            }
        }
    }

    fun markMessagesRead(childId: String, readerRole: AccountRole) {
        viewModelScope.launch { repository.markMessagesRead(childId, readerRole) }
    }

    fun requestExtraTime(childId: String, minutes: Int, reason: String) {
        viewModelScope.launch {
            repository.requestExtraTime(childId, minutes, reason)
            _uiMessage.value = "تم إرسال طلب الوقت الإضافي إلى ولي الأمر"
        }
    }

    fun resolveTimeRequest(request: TimeRequestEntity, approve: Boolean) {
        viewModelScope.launch {
            repository.resolveTimeRequest(request, approve)
            _uiMessage.value = if (approve) "تمت الموافقة على الوقت الإضافي" else "تم رفض الطلب"
        }
    }

    fun toggleLocationSharing(child: ChildProfileEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleLocationSharing(child, enabled)
            _uiMessage.value = if (enabled) "تم تفعيل مشاركة الموقع" else "تم إيقاف مشاركة الموقع"
        }
    }

    fun addSafeZone(childId: String, name: String, lat: Double, lng: Double, radiusMeters: Int) {
        viewModelScope.launch {
            repository.addSafeZone(childId, name, lat, lng, radiusMeters)
            _uiMessage.value = "تمت إضافة المنطقة الآمنة ($name)"
        }
    }

    fun toggleSafeZone(zone: SafeZoneEntity) {
        viewModelScope.launch { repository.toggleSafeZone(zone) }
    }

    fun deleteSafeZone(id: String) {
        viewModelScope.launch { repository.deleteSafeZone(id) }
    }

    fun markAlertRead(id: String) {
        viewModelScope.launch { repository.markAlertRead(id) }
    }

    fun markAllAlertsRead() {
        viewModelScope.launch { repository.markAllAlertsRead() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setParentPin(pin: String?) {
        viewModelScope.launch {
            repository.setParentPin(pin)
            _uiMessage.value = if (pin.isNullOrBlank()) "تم إلغاء رمز الحماية" else "تم تفعيل رمز حماية ولي الأمر"
        }
    }

    fun verifyParentPin(pin: String): Boolean {
        val hash = sessionPrefs.value.parentPinHash ?: return true
        return repository.verifyParentPin(pin, hash)
    }

    fun unlinkChild(childId: String) {
        viewModelScope.launch {
            repository.unlinkChild(childId)
            _uiMessage.value = "تم إلغاء ربط الجهاز"
        }
    }

    fun getLocalIpAndPort(): Pair<String, Int> = repository.getLocalIpAndPort()

    class Factory(
        private val context: Context,
        private val repository: WahjRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WahjViewModel(context.applicationContext, repository) as T
        }
    }
}
