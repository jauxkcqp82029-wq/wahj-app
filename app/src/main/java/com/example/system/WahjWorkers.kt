package com.example.system

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AlertCategory
import com.example.data.local.AlertEntity
import com.example.data.local.MessageStatus
import com.example.data.local.WahjDatabase
import com.example.data.local.WahjPreferences
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.TimeUnit

class ScheduledMessageWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val messageId = inputData.getString(KEY_MESSAGE_ID) ?: return Result.failure()
        val childId = inputData.getString(KEY_CHILD_ID) ?: return Result.failure()
        val db = WahjDatabase.getInstance(applicationContext)
        val msg = db.messageDao().getById(messageId) ?: return Result.success()
        val child = db.childDao().getById(childId)

        db.messageDao().upsert(msg.copy(status = MessageStatus.DELIVERED.name))
        db.alertDao().insert(
            AlertEntity(
                id = UUID.randomUUID().toString(),
                childId = childId,
                childName = child?.name ?: "الطفل",
                title = "رسالة مجدولة من وهج",
                body = msg.content,
                category = AlertCategory.NEW_MESSAGE.name
            )
        )
        NotificationHelper.showWahjNotification(
            context = applicationContext,
            channelId = NotificationHelper.CHANNEL_MESSAGES,
            notificationId = messageId.hashCode(),
            title = "رسالة جديدة",
            body = msg.content
        )
        return Result.success()
    }

    companion object {
        const val KEY_MESSAGE_ID = "message_id"
        const val KEY_CHILD_ID = "child_id"

        fun enqueue(context: Context, messageId: String, childId: String, delayMs: Long) {
            val data = Data.Builder()
                .putString(KEY_MESSAGE_ID, messageId)
                .putString(KEY_CHILD_ID, childId)
                .build()
            val request = OneTimeWorkRequestBuilder<ScheduledMessageWorker>()
                .setInitialDelay(delayMs.coerceAtLeast(5_000L), TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}

class MonitoringSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = WahjDatabase.getInstance(applicationContext)
        val prefs = WahjPreferences(applicationContext).prefsFlow.first()
        val childId = prefs.activeChildDeviceId ?: return Result.success()
        val child = db.childDao().getById(childId) ?: return Result.success()

        val health = BatteryTelemetryHelper.readSnapshot(applicationContext)
        val usageStats = if (UsageStatsHelper.hasUsageAccessPermission(applicationContext)) {
            UsageStatsHelper.queryInstalledAndUsedApps(applicationContext)
        } else {
            emptyList()
        }
        val totalUsedMinutes = usageStats.sumOf { it.dailyMinutes }
            .takeIf { it > 0 } ?: child.usedTodayMinutes

        db.childDao().update(
            child.copy(
                batteryLevel = health.batteryPercent,
                isCharging = health.isCharging,
                isOnline = health.isNetworkConnected,
                lastSeenAt = System.currentTimeMillis(),
                usedTodayMinutes = totalUsedMinutes
            )
        )

        if (health.batteryPercent <= 15 && !health.isCharging) {
            val body = "بطارية جهاز ${child.name} منخفضة (${health.batteryPercent}%)"
            db.alertDao().insert(
                AlertEntity(
                    id = UUID.randomUUID().toString(),
                    childId = child.id,
                    childName = child.name,
                    title = "انخفاض البطارية",
                    body = body,
                    category = AlertCategory.BATTERY_LOW.name
                )
            )
            NotificationHelper.showWahjNotification(
                applicationContext,
                NotificationHelper.CHANNEL_ALERTS,
                child.id.hashCode() + 10,
                "انخفاض البطارية",
                body
            )
        }

        val allowedTotal = child.dailyLimitMinutes + child.bonusMinutesToday
        if (totalUsedMinutes >= allowedTotal && allowedTotal > 0) {
            val body = "انتهى وقت الاستخدام اليومي المسموح لجهاز ${child.name}"
            db.alertDao().insert(
                AlertEntity(
                    id = UUID.randomUUID().toString(),
                    childId = child.id,
                    childName = child.name,
                    title = "انتهاء وقت الاستخدام",
                    body = body,
                    category = AlertCategory.TIME_EXPIRED.name
                )
            )
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "wahj_periodic_monitoring"

        fun schedulePeriodic(context: Context) {
            runCatching {
                val request = PeriodicWorkRequestBuilder<MonitoringSyncWorker>(15, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }
    }
}
