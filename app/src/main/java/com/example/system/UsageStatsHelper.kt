package com.example.system

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Calendar

data class RealAppUsageStat(
    val packageName: String,
    val appName: String,
    val dailyMinutes: Int,
    val weeklyMinutes: Int,
    val category: String
)

object UsageStatsHelper {
    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun buildUsageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun queryInstalledAndUsedApps(context: Context): List<RealAppUsageStat> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val installedInfos = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        val appNamesByPkg = installedInfos.associate { info ->
            val pkg = info.activityInfo.packageName
            val label = info.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg
            pkg to label
        }

        val dailyMap = mutableMapOf<String, Int>()
        val weeklyMap = mutableMapOf<String, Int>()

        if (hasUsageAccessPermission(context)) {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usm != null) {
                val now = System.currentTimeMillis()
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val startOfWeek = now - 7L * 24L * 60L * 60L * 1000L

                val dailyStats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
                dailyStats?.forEach { stat ->
                    val mins = (stat.totalTimeInForeground / 60_000L).toInt()
                    if (mins > 0) {
                        dailyMap[stat.packageName] = (dailyMap[stat.packageName] ?: 0) + mins
                    }
                }

                val weeklyStats = usm.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, startOfWeek, now)
                weeklyStats?.forEach { stat ->
                    val mins = (stat.totalTimeInForeground / 60_000L).toInt()
                    if (mins > 0) {
                        weeklyMap[stat.packageName] = (weeklyMap[stat.packageName] ?: 0) + mins
                    }
                }
            }
        }

        val allPkgs = (appNamesByPkg.keys + dailyMap.keys)
            .filter { it != context.packageName }
            .distinct()

        return allPkgs.map { pkg ->
            val name = appNamesByPkg[pkg] ?: pkg.substringAfterLast('.')
            val daily = dailyMap[pkg] ?: 0
            val weekly = maxOf(weeklyMap[pkg] ?: 0, daily)
            RealAppUsageStat(
                packageName = pkg,
                appName = name,
                dailyMinutes = daily,
                weeklyMinutes = weekly,
                category = inferCategory(pkg, name)
            )
        }.sortedWith(compareByDescending<RealAppUsageStat> { it.dailyMinutes }.thenBy { it.appName })
    }

    private fun inferCategory(pkg: String, name: String): String {
        val lower = "$pkg $name".lowercase()
        return when {
            lower.contains("youtube") || lower.contains("video") || lower.contains("netflix") -> "فيديو"
            lower.contains("game") || lower.contains("roblox") || lower.contains("minecraft") -> "ألعاب"
            lower.contains("classroom") || lower.contains("khan") || lower.contains("edu") || lower.contains("book") -> "تعليم"
            lower.contains("whatsapp") || lower.contains("telegram") || lower.contains("chat") -> "تواصل"
            lower.contains("chrome") || lower.contains("browser") -> "تصفح"
            else -> "عام"
        }
    }
}
