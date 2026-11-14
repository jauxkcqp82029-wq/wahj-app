package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.local.*
import com.example.data.security.CryptoHelper
import com.example.data.security.PairingTokenManager
import com.example.data.security.WahjPairingPayload
import com.example.data.sync.LanSyncPacket
import com.example.data.sync.LocalLanSyncEngine
import com.example.system.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class TelemetrySyncDto(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val usedTodayMinutes: Int,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val locationSharingEnabled: Boolean,
    val apps: List<AppSummaryDto> = emptyList()
)

@Serializable
data class AppSummaryDto(
    val packageName: String,
    val appName: String,
    val category: String,
    val dailyMinutes: Int
)

@Serializable
data class PairApprovedDto(
    val childId: String,
    val childName: String,
    val relation: String,
    val parentName: String
)

@Serializable
data class TimeRequestDto(
    val id: String,
    val requestedMinutes: Int,
    val reason: String
)

class WahjRepository(
    private val context: Context,
    private val db: WahjDatabase,
    private val prefs: WahjPreferences,
    private val lanSync: LocalLanSyncEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _pendingPairingApproval = MutableStateFlow<PairApprovedDto?>(null)
    val pendingPairingApproval: StateFlow<PairApprovedDto?> = _pendingPairingApproval.asStateFlow()

    val sessionPrefs: Flow<WahjSessionPrefs> = prefs.prefsFlow
    val allAccounts: Flow<List<AccountEntity>> = db.accountDao().observeAll()
    val allAlerts: Flow<List<AlertEntity>> = db.alertDao().observeAll()
    val allTimeRequests: Flow<List<TimeRequestEntity>> = db.timeRequestDao().observeAll()

    // Stores the most recent QR payload generated on this device for single-device testing convenience
    private val _lastGeneratedQrRaw = MutableStateFlow<String?>(null)
    val lastGeneratedQrRaw: StateFlow<String?> = _lastGeneratedQrRaw.asStateFlow()

    init {
        lanSync.startServer()
        observeLanPackets()
        MonitoringSyncWorker.schedulePeriodic(context)
    }

    private fun observeLanPackets() {
        scope.launch {
            lanSync.incomingPackets.collect { packet ->
                when (packet.type) {
                    "PAIR_APPROVED" -> {
                        val dto = runCatching {
                            json.decodeFromString<PairApprovedDto>(packet.payloadJson)
                        }.getOrNull() ?: return@collect
                        val health = BatteryTelemetryHelper.readSnapshot(context)
                        val existing = db.childDao().getById(dto.childId)
                        val child = (existing ?: ChildProfileEntity(
                            id = dto.childId,
                            parentId = "remote_parent",
                            name = dto.childName,
                            relation = dto.relation,
                            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                            deviceType = "هاتف",
                            osVersion = "Android ${Build.VERSION.RELEASE}",
                            isOnline = true,
                            batteryLevel = health.batteryPercent,
                            isCharging = health.isCharging,
                            lastSeenAt = System.currentTimeMillis(),
                            lastLatitude = 24.7136,
                            lastLongitude = 46.6753,
                            lastAddress = "بانتظار إذن الموقع",
                            locationUpdatedAt = System.currentTimeMillis()
                        )).copy(
                            peerHost = packet.parentHost,
                            peerPort = packet.parentPort,
                            isOnline = true,
                            lastSeenAt = System.currentTimeMillis()
                        )
                        db.childDao().upsert(child)
                        prefs.setActiveChildDeviceId(child.id)
                        _pendingPairingApproval.value = dto
                        NotificationHelper.showWahjNotification(
                            context,
                            NotificationHelper.CHANNEL_ALERTS,
                            1001,
                            "تم ربط الجهاز بنجاح",
                            context.getString(R.string.child_welcome_post_pair)
                        )
                    }

                    "MESSAGE" -> {
                        val content = packet.payloadJson
                        val msg = MessageEntity(
                            id = UUID.randomUUID().toString(),
                            childId = packet.childId,
                            senderRole = packet.senderRole,
                            content = content,
                            status = MessageStatus.DELIVERED.name
                        )
                        db.messageDao().upsert(msg)
                        val child = db.childDao().getById(packet.childId)
                        db.alertDao().insert(
                            AlertEntity(
                                id = UUID.randomUUID().toString(),
                                childId = packet.childId,
                                childName = child?.name ?: "العائلة",
                                title = "رسالة جديدة",
                                body = content,
                                category = AlertCategory.NEW_MESSAGE.name
                            )
                        )
                        NotificationHelper.showWahjNotification(
                            context,
                            NotificationHelper.CHANNEL_MESSAGES,
                            msg.id.hashCode(),
                            "رسالة جديدة",
                            content
                        )
                    }

                    "LOCK_COMMAND" -> {
                        val child = db.childDao().getById(packet.childId)
                        if (child != null) {
                            db.childDao().update(child.copy(isDeviceLocked = true))
                        }
                        val locked = DevicePolicyHelper.lockScreenIfAuthorized(context)
                        val msg = if (locked) "تم قفل شاشة الجهاز عن بُعد من ولي الأمر"
                        else "طلب ولي الأمر قفل الجهاز (فعّل صلاحية إدارة الجهاز)"
                        NotificationHelper.showWahjNotification(
                            context,
                            NotificationHelper.CHANNEL_ALERTS,
                            2001,
                            "أمر قفل الجهاز",
                            msg
                        )
                    }

                    "POLICY_UPDATE" -> {
                        val updated = runCatching {
                            json.decodeFromString<ChildProfileEntity>(packet.payloadJson)
                        }.getOrNull() ?: return@collect
                        db.childDao().upsert(updated)
                    }

                    "TELEMETRY" -> {
                        val dto = runCatching {
                            json.decodeFromString<TelemetrySyncDto>(packet.payloadJson)
                        }.getOrNull() ?: return@collect
                        val child = db.childDao().getById(packet.childId) ?: return@collect
                        db.childDao().update(
                            child.copy(
                                batteryLevel = dto.batteryLevel,
                                isCharging = dto.isCharging,
                                usedTodayMinutes = dto.usedTodayMinutes,
                                lastLatitude = dto.latitude,
                                lastLongitude = dto.longitude,
                                lastAddress = dto.address,
                                locationSharingEnabled = dto.locationSharingEnabled,
                                locationUpdatedAt = System.currentTimeMillis(),
                                lastSeenAt = System.currentTimeMillis(),
                                isOnline = true
                            )
                        )
                        if (dto.apps.isNotEmpty()) {
                            val existingRules = db.appRuleDao().getByChild(child.id).associateBy { it.packageName }
                            val merged = dto.apps.map { app ->
                                val prev = existingRules[app.packageName]
                                AppRuleEntity(
                                    id = prev?.id ?: "${child.id}_${app.packageName}",
                                    childId = child.id,
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    category = app.category,
                                    isAllowed = prev?.isAllowed ?: true,
                                    isRestricted = prev?.isRestricted ?: false,
                                    dailyLimitMinutes = prev?.dailyLimitMinutes ?: 60,
                                    usedTodayMinutes = app.dailyMinutes,
                                    notifyOnAttempt = prev?.notifyOnAttempt ?: true
                                )
                            }
                            db.appRuleDao().upsertAll(merged)
                        }
                        checkSafeZonesAndAlerts(child, dto.latitude, dto.longitude, dto.batteryLevel, dto.isCharging)
                    }

                    "TIME_REQUEST" -> {
                        val reqDto = runCatching {
                            json.decodeFromString<TimeRequestDto>(packet.payloadJson)
                        }.getOrNull() ?: return@collect
                        val child = db.childDao().getById(packet.childId)
                        db.timeRequestDao().upsert(
                            TimeRequestEntity(
                                id = reqDto.id,
                                childId = packet.childId,
                                requestedMinutes = reqDto.requestedMinutes,
                                reason = reqDto.reason,
                                status = RequestStatus.PENDING.name
                            )
                        )
                        val body = "طلب ${child?.name ?: "الطفل"} ${reqDto.requestedMinutes} دقيقة إضافية: ${reqDto.reason}"
                        db.alertDao().insert(
                            AlertEntity(
                                id = UUID.randomUUID().toString(),
                                childId = packet.childId,
                                childName = child?.name ?: "الطفل",
                                title = "طلب وقت إضافي",
                                body = body,
                                category = AlertCategory.EXTRA_TIME_REQUEST.name
                            )
                        )
                        NotificationHelper.showWahjNotification(
                            context,
                            NotificationHelper.CHANNEL_ALERTS,
                            reqDto.id.hashCode(),
                            "طلب وقت إضافي",
                            body
                        )
                    }
                }
            }
        }
    }

    suspend fun registerAccount(
        fullName: String,
        email: String,
        password: String,
        role: AccountRole
    ): Result<AccountEntity> {
        return runCatching {
            val cleanEmail = email.trim().lowercase()
            require(fullName.trim().length >= 2) { "يرجى إدخال الاسم الكامل" }
            require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "البريد الإلكتروني غير صالح" }
            require(password.length >= 6) { "كلمة المرور يجب أن تكون 6 أحرف على الأقل" }

            val existing = db.accountDao().findByEmail(cleanEmail)
            require(existing == null) { "هذا البريد مسجل مسبقاً. يمكنك تسجيل الدخول." }

            val salt = CryptoHelper.generateSalt()
            val hash = CryptoHelper.hashSecret(password, salt)
            val account = AccountEntity(
                id = UUID.randomUUID().toString(),
                fullName = fullName.trim(),
                email = cleanEmail,
                passwordHash = hash,
                salt = salt,
                role = role.name
            )
            db.accountDao().insert(account)
            prefs.setSession(account.id, role.name)
            account
        }
    }

    suspend fun loginAccount(email: String, password: String): Result<AccountEntity> {
        return runCatching {
            val cleanEmail = email.trim().lowercase()
            require(cleanEmail.isNotBlank() && password.isNotBlank()) { "يرجى إدخال البريد وكلمة المرور" }
            val account = db.accountDao().findByEmail(cleanEmail)
                ?: throw IllegalArgumentException("لا يوجد حساب مسجل بهذا البريد الإلكتروني")
            val valid = CryptoHelper.verifySecret(password, account.salt, account.passwordHash)
            require(valid) { "كلمة المرور غير صحيحة" }
            prefs.setSession(account.id, account.role)
            account
        }
    }

    suspend fun selectRole(role: AccountRole) {
        prefs.setSelectedRole(role.name)
    }

    suspend fun switchActiveAccount(account: AccountEntity) {
        prefs.setSession(account.id, account.role)
    }

    suspend fun logout() {
        prefs.clearSession()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        prefs.setThemeMode(mode)
    }

    suspend fun setSelectedChild(childId: String?) {
        prefs.setSelectedChildId(childId)
    }

    suspend fun setActiveChildDeviceId(childId: String) {
        prefs.setActiveChildDeviceId(childId)
    }

    suspend fun markChildWelcomeShown() {
        prefs.markChildWelcomeShown()
    }

    suspend fun setParentPin(pin: String?) {
        if (pin.isNullOrBlank()) {
            prefs.setParentPinHash(null)
        } else {
            val hash = CryptoHelper.hashSecret(pin, "wahj_pin")
            prefs.setParentPinHash(hash)
        }
    }

    fun verifyParentPin(pin: String, storedHash: String): Boolean {
        return CryptoHelper.verifySecret(pin, "wahj_pin", storedHash)
    }

    fun observeChildren(parentId: String?): Flow<List<ChildProfileEntity>> {
        return if (parentId != null) db.childDao().observeByParent(parentId)
        else db.childDao().observeAll()
    }

    fun observeAllChildren(): Flow<List<ChildProfileEntity>> = db.childDao().observeAll()
    fun observeChild(childId: String): Flow<ChildProfileEntity?> = db.childDao().observeById(childId)
    fun observeAppRules(childId: String): Flow<List<AppRuleEntity>> = db.appRuleDao().observeByChild(childId)
    fun observeSchedules(childId: String): Flow<List<ScheduleEntity>> = db.scheduleDao().observeByChild(childId)
    fun observeMessages(childId: String): Flow<List<MessageEntity>> = db.messageDao().observeByChild(childId)
    fun observeSafeZones(childId: String): Flow<List<SafeZoneEntity>> = db.safeZoneDao().observeByChild(childId)
    fun observeLocationHistory(childId: String): Flow<List<LocationHistoryEntity>> = db.locationHistoryDao().observeByChild(childId)
    fun observeTimeRequests(childId: String): Flow<List<TimeRequestEntity>> = db.timeRequestDao().observeByChild(childId)

    fun createChildQrPayload(childName: String, deviceType: String): Pair<WahjPairingPayload, String> {
        val tempId = UUID.randomUUID().toString()
        val payload = PairingTokenManager.createPayload(
            childTempId = tempId,
            childName = childName.ifBlank { "طفلي" },
            deviceType = deviceType,
            hostIp = lanSync.getLocalIpAddress(),
            port = lanSync.boundPort
        )
        val encoded = PairingTokenManager.encode(payload)
        _lastGeneratedQrRaw.value = encoded
        return payload to encoded
    }

    suspend fun parseAndVerifyQr(raw: String): Result<WahjPairingPayload> {
        val parsed = PairingTokenManager.decodeAndValidate(raw)
        if (parsed.isSuccess) {
            val payload = parsed.getOrThrow()
            if (db.usedNonceDao().exists(payload.nonce)) {
                return Result.failure(IllegalStateException("تم استخدام رمز QR هذا مسبقاً (أحادي الاستخدام)"))
            }
        }
        return parsed
    }

    suspend fun confirmPairing(
        parentId: String,
        parentName: String,
        payload: WahjPairingPayload,
        childNameOverride: String,
        relation: String
    ): Result<ChildProfileEntity> {
        return runCatching {
            require(!db.usedNonceDao().exists(payload.nonce)) {
                "تم استهلاك رمز الأمان مسبقاً"
            }
            db.usedNonceDao().insert(UsedNonceEntity(nonce = payload.nonce))

            val health = BatteryTelemetryHelper.readSnapshot(context)
            val realApps = UsageStatsHelper.queryInstalledAndUsedApps(context)
            val usedToday = realApps.sumOf { it.dailyMinutes }

            val childId = payload.childTempId
            val child = ChildProfileEntity(
                id = childId,
                parentId = parentId,
                name = childNameOverride.trim().ifBlank { payload.childName },
                relation = relation.ifBlank { "ابن" },
                deviceName = payload.deviceModel,
                deviceType = payload.deviceType,
                osVersion = payload.osVersion,
                isOnline = true,
                batteryLevel = health.batteryPercent,
                isCharging = health.isCharging,
                lastSeenAt = System.currentTimeMillis(),
                lastLatitude = 24.7136,
                lastLongitude = 46.6753,
                lastAddress = "الرياض — بانتظار تحديث الموقع المباشر",
                locationUpdatedAt = System.currentTimeMillis(),
                locationSharingEnabled = true,
                dailyLimitMinutes = 180,
                usedTodayMinutes = usedToday,
                pairingToken = payload.token,
                peerHost = payload.hostIp,
                peerPort = payload.port
            )
            db.childDao().upsert(child)

            // Seed real discovered launcher apps into AppRuleEntity
            val rules = realApps.take(25).map { stat ->
                AppRuleEntity(
                    id = "${childId}_${stat.packageName}",
                    childId = childId,
                    packageName = stat.packageName,
                    appName = stat.appName,
                    category = stat.category,
                    isAllowed = true,
                    isRestricted = false,
                    dailyLimitMinutes = 60,
                    usedTodayMinutes = stat.dailyMinutes,
                    notifyOnAttempt = true
                )
            }
            if (rules.isNotEmpty()) db.appRuleDao().upsertAll(rules)

            // Default family schedules
            db.scheduleDao().upsert(
                ScheduleEntity(
                    id = "${childId}_study",
                    childId = childId,
                    title = "وقت الدراسة والواجبات",
                    type = ScheduleType.STUDY.name,
                    startTime = "16:00",
                    endTime = "18:00",
                    daysSummary = "الأحد - الخميس",
                    isEnabled = true
                )
            )
            db.scheduleDao().upsert(
                ScheduleEntity(
                    id = "${childId}_bedtime",
                    childId = childId,
                    title = "وقت النوم",
                    type = ScheduleType.BEDTIME.name,
                    startTime = "21:30",
                    endTime = "06:30",
                    daysSummary = "يومياً",
                    isEnabled = true
                )
            )

            // Default home safe zone
            db.safeZoneDao().upsert(
                SafeZoneEntity(
                    id = "${childId}_home",
                    childId = childId,
                    name = "المنزل",
                    latitude = child.lastLatitude,
                    longitude = child.lastLongitude,
                    radiusMeters = 250,
                    isActive = true
                )
            )

            db.alertDao().insert(
                AlertEntity(
                    id = UUID.randomUUID().toString(),
                    childId = childId,
                    childName = child.name,
                    title = "تم ربط جهاز جديد",
                    body = "تم ربط جهاز ${child.name} (${child.deviceName}) بنجاح عبر رمز QR الآمن.",
                    category = AlertCategory.NEW_MESSAGE.name
                )
            )

            prefs.setSelectedChildId(childId)
            prefs.setActiveChildDeviceId(childId)

            // Notify child device over local Wi-Fi LAN if reachable
            val approvalDto = PairApprovedDto(
                childId = childId,
                childName = child.name,
                relation = child.relation,
                parentName = parentName
            )
            lanSync.sendPacket(
                host = payload.hostIp,
                port = payload.port,
                packet = LanSyncPacket(
                    type = "PAIR_APPROVED",
                    childId = childId,
                    token = payload.token,
                    senderRole = AccountRole.PARENT.name,
                    payloadJson = json.encodeToString(approvalDto),
                    parentHost = lanSync.getLocalIpAddress(),
                    parentPort = lanSync.boundPort
                )
            )
            child
        }
    }

    suspend fun clearPendingPairingApproval() {
        _pendingPairingApproval.value = null
    }

    suspend fun syncCurrentDeviceTelemetry(childId: String) {
        val child = db.childDao().getById(childId) ?: return
        val health = BatteryTelemetryHelper.readSnapshot(context)
        val realApps = UsageStatsHelper.queryInstalledAndUsedApps(context)
        val totalUsed = realApps.sumOf { it.dailyMinutes }

        var lat = child.lastLatitude
        var lng = child.lastLongitude
        var addr = child.lastAddress
        if (child.locationSharingEnabled && LocationHelper.hasLocationPermission(context)) {
            val coords = LocationHelper.fetchCurrentLocation(context)
            if (coords != null) {
                lat = coords.latitude
                lng = coords.longitude
                addr = coords.addressLabel
                db.locationHistoryDao().insert(
                    LocationHistoryEntity(
                        id = UUID.randomUUID().toString(),
                        childId = childId,
                        latitude = lat,
                        longitude = lng,
                        address = addr,
                        eventType = "تحديث موقع"
                    )
                )
            }
        }

        val updated = child.copy(
            batteryLevel = health.batteryPercent,
            isCharging = health.isCharging,
            isOnline = health.isNetworkConnected,
            lastSeenAt = System.currentTimeMillis(),
            usedTodayMinutes = if (totalUsed > 0) totalUsed else child.usedTodayMinutes,
            lastLatitude = lat,
            lastLongitude = lng,
            lastAddress = addr,
            locationUpdatedAt = System.currentTimeMillis()
        )
        db.childDao().update(updated)

        if (realApps.isNotEmpty()) {
            val existingRules = db.appRuleDao().getByChild(childId).associateBy { it.packageName }
            val merged = realApps.take(25).map { app ->
                val prev = existingRules[app.packageName]
                AppRuleEntity(
                    id = prev?.id ?: "${childId}_${app.packageName}",
                    childId = childId,
                    packageName = app.packageName,
                    appName = app.appName,
                    category = app.category,
                    isAllowed = prev?.isAllowed ?: true,
                    isRestricted = prev?.isRestricted ?: false,
                    dailyLimitMinutes = prev?.dailyLimitMinutes ?: 60,
                    usedTodayMinutes = app.dailyMinutes,
                    notifyOnAttempt = prev?.notifyOnAttempt ?: true
                )
            }
            db.appRuleDao().upsertAll(merged)
        }

        checkSafeZonesAndAlerts(updated, lat, lng, health.batteryPercent, health.isCharging)

        if (updated.peerHost.isNotBlank()) {
            val dto = TelemetrySyncDto(
                batteryLevel = updated.batteryLevel,
                isCharging = updated.isCharging,
                usedTodayMinutes = updated.usedTodayMinutes,
                latitude = lat,
                longitude = lng,
                address = addr,
                locationSharingEnabled = updated.locationSharingEnabled,
                apps = realApps.take(15).map {
                    AppSummaryDto(it.packageName, it.appName, it.category, it.dailyMinutes)
                }
            )
            lanSync.sendPacket(
                updated.peerHost,
                updated.peerPort,
                LanSyncPacket(
                    type = "TELEMETRY",
                    childId = childId,
                    senderRole = AccountRole.CHILD.name,
                    payloadJson = json.encodeToString(dto)
                )
            )
        }
    }

    private suspend fun checkSafeZonesAndAlerts(
        child: ChildProfileEntity,
        lat: Double,
        lng: Double,
        battery: Int,
        isCharging: Boolean
    ) {
        if (battery <= 15 && !isCharging) {
            db.alertDao().insert(
                AlertEntity(
                    id = "bat_${child.id}_${System.currentTimeMillis() / 3_600_000}",
                    childId = child.id,
                    childName = child.name,
                    title = "انخفاض البطارية",
                    body = "مستوى بطارية جهاز ${child.name} وصل إلى $battery%",
                    category = AlertCategory.BATTERY_LOW.name
                )
            )
        }
        val zones = db.safeZoneDao().observeByChild(child.id).first()
        zones.filter { it.isActive }.forEach { zone ->
            val dist = LocationHelper.distanceMeters(lat, lng, zone.latitude, zone.longitude)
            if (dist > zone.radiusMeters && zone.notifyOnExit) {
                db.alertDao().insert(
                    AlertEntity(
                        id = "geo_exit_${zone.id}_${System.currentTimeMillis() / 600_000}",
                        childId = child.id,
                        childName = child.name,
                        title = "مغادرة المنطقة الآمنة",
                        body = "غادر ${child.name} نطاق (${zone.name}) — المسافة ${dist.toInt()}م",
                        category = AlertCategory.LOCATION_EVENT.name
                    )
                )
            }
        }
    }

    suspend fun toggleRemoteDeviceLock(child: ChildProfileEntity): Boolean {
        val nextLocked = !child.isDeviceLocked
        val updated = child.copy(isDeviceLocked = nextLocked)
        db.childDao().update(updated)
        if (nextLocked) {
            DevicePolicyHelper.lockScreenIfAuthorized(context)
            if (child.peerHost.isNotBlank()) {
                lanSync.sendPacket(
                    child.peerHost,
                    child.peerPort,
                    LanSyncPacket(
                        type = "LOCK_COMMAND",
                        childId = child.id,
                        senderRole = AccountRole.PARENT.name
                    )
                )
            }
            db.alertDao().insert(
                AlertEntity(
                    id = UUID.randomUUID().toString(),
                    childId = child.id,
                    childName = child.name,
                    title = "قفل الجهاز عن بُعد",
                    body = "تم تفعيل وضع قفل الاستخدام لجهاز ${child.name}",
                    category = AlertCategory.TIME_EXPIRED.name
                )
            )
        }
        return nextLocked
    }

    suspend fun updateScreenTimePolicy(
        child: ChildProfileEntity,
        dailyLimitMinutes: Int,
        studyStart: String,
        studyEnd: String,
        bedtimeStart: String,
        bedtimeEnd: String
    ) {
        val updated = child.copy(
            dailyLimitMinutes = dailyLimitMinutes,
            studyStart = studyStart,
            studyEnd = studyEnd,
            bedtimeStart = bedtimeStart,
            bedtimeEnd = bedtimeEnd
        )
        db.childDao().update(updated)
        if (child.peerHost.isNotBlank()) {
            lanSync.sendPacket(
                child.peerHost,
                child.peerPort,
                LanSyncPacket(
                    type = "POLICY_UPDATE",
                    childId = child.id,
                    senderRole = AccountRole.PARENT.name,
                    payloadJson = json.encodeToString(updated)
                )
            )
        }
    }

    suspend fun grantExtraTime(child: ChildProfileEntity, extraMinutes: Int) {
        val updated = child.copy(
            bonusMinutesToday = child.bonusMinutesToday + extraMinutes,
            isDeviceLocked = false
        )
        db.childDao().update(updated)
        sendMessage(
            childId = child.id,
            senderRole = AccountRole.PARENT,
            content = "تمت إضافة $extraMinutes دقيقة إضافية لوقتك اليوم!",
            isPreset = false
        )
    }

    suspend fun updateAppRule(rule: AppRuleEntity) {
        db.appRuleDao().upsert(rule)
        if (rule.isRestricted && rule.notifyOnAttempt) {
            val child = db.childDao().getById(rule.childId)
            db.alertDao().insert(
                AlertEntity(
                    id = UUID.randomUUID().toString(),
                    childId = rule.childId,
                    childName = child?.name ?: "",
                    title = "تحديث تقييد تطبيق",
                    body = "تم تقييد تطبيق ${rule.appName} وتفعيل تنبيه محاولات الفتح.",
                    category = AlertCategory.RESTRICTED_APP.name
                )
            )
        }
    }

    suspend fun simulateRestrictedAppAttempt(childId: String, appName: String) {
        val child = db.childDao().getById(childId) ?: return
        val body = "تنبيه: محاولة فتح تطبيق مقيد ($appName) على جهاز ${child.name}"
        db.alertDao().insert(
            AlertEntity(
                id = UUID.randomUUID().toString(),
                childId = childId,
                childName = child.name,
                title = "محاولة استخدام تطبيق مقيد",
                body = body,
                category = AlertCategory.RESTRICTED_APP.name
            )
        )
        NotificationHelper.showWahjNotification(
            context,
            NotificationHelper.CHANNEL_ALERTS,
            appName.hashCode(),
            "تطبيق مقيد",
            body
        )
    }

    suspend fun upsertSchedule(schedule: ScheduleEntity) {
        db.scheduleDao().upsert(schedule)
    }

    suspend fun deleteSchedule(id: String) {
        db.scheduleDao().deleteById(id)
    }

    suspend fun sendMessage(
        childId: String,
        senderRole: AccountRole,
        content: String,
        isPreset: Boolean,
        scheduleDelayMinutes: Int = 0
    ) {
        val clean = content.trim()
        if (clean.isBlank()) return
        val id = UUID.randomUUID().toString()
        val isScheduled = scheduleDelayMinutes > 0
        val scheduledAt = if (isScheduled) System.currentTimeMillis() + scheduleDelayMinutes * 60_000L else null

        val msg = MessageEntity(
            id = id,
            childId = childId,
            senderRole = senderRole.name,
            content = clean,
            isPreset = isPreset,
            status = if (isScheduled) MessageStatus.SCHEDULED.name else MessageStatus.SENT.name,
            scheduledAt = scheduledAt
        )
        db.messageDao().upsert(msg)

        if (isScheduled) {
            ScheduledMessageWorker.enqueue(context, id, childId, scheduleDelayMinutes * 60_000L)
        } else {
            val child = db.childDao().getById(childId)
            val sent = if (child != null && child.peerHost.isNotBlank()) {
                lanSync.sendPacket(
                    child.peerHost,
                    child.peerPort,
                    LanSyncPacket(
                        type = "MESSAGE",
                        childId = childId,
                        senderRole = senderRole.name,
                        payloadJson = clean
                    )
                )
            } else {
                true
            }
            if (sent) {
                db.messageDao().upsert(msg.copy(status = MessageStatus.DELIVERED.name))
            }
            NotificationHelper.showWahjNotification(
                context,
                NotificationHelper.CHANNEL_MESSAGES,
                id.hashCode(),
                if (senderRole == AccountRole.PARENT) "رسالة من ولي الأمر" else "رسالة من ${child?.name ?: "الطفل"}",
                clean
            )
        }
    }

    suspend fun markMessagesRead(childId: String, readerRole: AccountRole) {
        val otherSender = if (readerRole == AccountRole.CHILD) AccountRole.PARENT.name else AccountRole.CHILD.name
        db.messageDao().markAllStatusForRole(childId, otherSender, MessageStatus.READ.name)
    }

    suspend fun requestExtraTime(childId: String, minutes: Int, reason: String) {
        val child = db.childDao().getById(childId) ?: return
        val reqId = UUID.randomUUID().toString()
        db.timeRequestDao().upsert(
            TimeRequestEntity(
                id = reqId,
                childId = childId,
                requestedMinutes = minutes,
                reason = reason.ifBlank { "احتياج لوقت إضافي" },
                status = RequestStatus.PENDING.name
            )
        )
        val body = "طلب ${child.name} $minutes دقيقة إضافية: ${reason.ifBlank { "متابعة الدراسة" }}"
        db.alertDao().insert(
            AlertEntity(
                id = UUID.randomUUID().toString(),
                childId = childId,
                childName = child.name,
                title = "طلب وقت إضافي",
                body = body,
                category = AlertCategory.EXTRA_TIME_REQUEST.name
            )
        )
        if (child.peerHost.isNotBlank()) {
            lanSync.sendPacket(
                child.peerHost,
                child.peerPort,
                LanSyncPacket(
                    type = "TIME_REQUEST",
                    childId = childId,
                    senderRole = AccountRole.CHILD.name,
                    payloadJson = json.encodeToString(TimeRequestDto(reqId, minutes, reason))
                )
            )
        }
    }

    suspend fun resolveTimeRequest(request: TimeRequestEntity, approve: Boolean) {
        val newStatus = if (approve) RequestStatus.APPROVED.name else RequestStatus.REJECTED.name
        db.timeRequestDao().upsert(request.copy(status = newStatus))
        val child = db.childDao().getById(request.childId) ?: return
        if (approve) {
            grantExtraTime(child, request.requestedMinutes)
        } else {
            sendMessage(
                childId = child.id,
                senderRole = AccountRole.PARENT,
                content = "تم تأجيل طلب الوقت الإضافي (${request.requestedMinutes} دقيقة) لوقت لاحق.",
                isPreset = false
            )
        }
    }

    suspend fun toggleLocationSharing(child: ChildProfileEntity, enabled: Boolean) {
        db.childDao().update(child.copy(locationSharingEnabled = enabled))
    }

    suspend fun addSafeZone(childId: String, name: String, lat: Double, lng: Double, radiusMeters: Int) {
        db.safeZoneDao().upsert(
            SafeZoneEntity(
                id = UUID.randomUUID().toString(),
                childId = childId,
                name = name.trim(),
                latitude = lat,
                longitude = lng,
                radiusMeters = radiusMeters,
                isActive = true
            )
        )
    }

    suspend fun toggleSafeZone(zone: SafeZoneEntity) {
        db.safeZoneDao().upsert(zone.copy(isActive = !zone.isActive))
    }

    suspend fun deleteSafeZone(id: String) {
        db.safeZoneDao().deleteById(id)
    }

    suspend fun markAlertRead(id: String) {
        db.alertDao().markRead(id)
    }

    suspend fun markAllAlertsRead() {
        db.alertDao().markAllRead()
    }

    suspend fun unlinkChild(childId: String) {
        db.childDao().deleteById(childId)
    }

    fun getLocalIpAndPort(): Pair<String, Int> = lanSync.getLocalIpAddress() to lanSync.boundPort
}
