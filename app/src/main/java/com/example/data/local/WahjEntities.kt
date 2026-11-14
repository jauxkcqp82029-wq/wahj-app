package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountRole {
    PARENT,
    CHILD
}

enum class MessageStatus {
    SCHEDULED,
    SENT,
    DELIVERED,
    READ
}

enum class ScheduleType {
    STUDY,
    BEDTIME,
    BREAK
}

enum class AlertCategory {
    OFFLINE,
    BATTERY_LOW,
    TIME_EXPIRED,
    EXTRA_TIME_REQUEST,
    NEW_MESSAGE,
    LOCATION_EVENT,
    RESTRICTED_APP
}

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val role: String, // PARENT or CHILD
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "children")
data class ChildProfileEntity(
    @PrimaryKey val id: String,
    val parentId: String,
    val name: String,
    val relation: String,
    val deviceName: String,
    val deviceType: String, // "هاتف" or "تابلت"
    val osVersion: String,
    val isOnline: Boolean,
    val batteryLevel: Int,
    val isCharging: Boolean = false,
    val lastSeenAt: Long,
    val lastLatitude: Double,
    val lastLongitude: Double,
    val lastAddress: String,
    val locationUpdatedAt: Long,
    val locationSharingEnabled: Boolean = true,
    val dailyLimitMinutes: Int = 180,
    val usedTodayMinutes: Int = 0,
    val bonusMinutesToday: Int = 0,
    val studyStart: String = "16:00",
    val studyEnd: String = "18:00",
    val bedtimeStart: String = "21:30",
    val bedtimeEnd: String = "06:30",
    val isDeviceLocked: Boolean = false,
    val pairingToken: String = "",
    val peerHost: String = "",
    val peerPort: Int = 0,
    val pairedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val packageName: String,
    val appName: String,
    val category: String,
    val isAllowed: Boolean = true,
    val isRestricted: Boolean = false,
    val dailyLimitMinutes: Int = 60,
    val usedTodayMinutes: Int = 0,
    val notifyOnAttempt: Boolean = true
)

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val title: String,
    val type: String, // STUDY, BEDTIME, BREAK
    val startTime: String,
    val endTime: String,
    val daysSummary: String,
    val isEnabled: Boolean = true
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val senderRole: String, // PARENT or CHILD
    val content: String,
    val isPreset: Boolean = false,
    val status: String = MessageStatus.SENT.name,
    val scheduledAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "time_requests")
data class TimeRequestEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val requestedMinutes: Int,
    val reason: String,
    val status: String = RequestStatus.PENDING.name,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "safe_zones")
data class SafeZoneEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 250,
    val isActive: Boolean = true,
    val notifyOnEnter: Boolean = true,
    val notifyOnExit: Boolean = true
)

@Entity(tableName = "location_history")
data class LocationHistoryEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val eventType: String,
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val childName: String,
    val title: String,
    val body: String,
    val category: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "used_nonces")
data class UsedNonceEntity(
    @PrimaryKey val nonce: String,
    val usedAt: Long = System.currentTimeMillis()
)
