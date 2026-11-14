package com.example.system

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

class WahjDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "تم تفعيل صلاحية إدارة جهاز وهج بنجاح", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "تم إيقاف صلاحية إدارة جهاز وهج", Toast.LENGTH_SHORT).show()
    }
}

object DevicePolicyHelper {
    fun componentName(context: Context): ComponentName {
        return ComponentName(context, WahjDeviceAdminReceiver::class.java)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        return dpm.isAdminActive(componentName(context))
    }

    fun buildActivateAdminIntent(context: Context): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "تسمح هذه الصلاحية الرسمية لولي الأمر بقفل شاشة جهاز الطفل عن بُعد عند انتهاء الوقت أو وقت النوم. لا يمكن للتطبيق فتح قفل النظام عن بُعد."
            )
        }
    }

    fun lockScreenIfAuthorized(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        val admin = componentName(context)
        return if (dpm.isAdminActive(admin)) {
            dpm.lockNow()
            true
        } else {
            false
        }
    }
}
