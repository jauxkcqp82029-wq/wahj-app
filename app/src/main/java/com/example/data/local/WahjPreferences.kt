package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.wahjDataStore by preferencesDataStore(name = "wahj_prefs")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class WahjSessionPrefs(
    val activeAccountId: String? = null,
    val activeRole: String? = null, // PARENT or CHILD
    val activeChildDeviceId: String? = null,
    val selectedChildId: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingSeen: Boolean = false,
    val parentPinHash: String? = null,
    val childWelcomeShown: Boolean = false
)

class WahjPreferences(private val context: Context) {
    private object Keys {
        val ACTIVE_ACCOUNT_ID = stringPreferencesKey("active_account_id")
        val ACTIVE_ROLE = stringPreferencesKey("active_role")
        val ACTIVE_CHILD_DEVICE_ID = stringPreferencesKey("active_child_device_id")
        val SELECTED_CHILD_ID = stringPreferencesKey("selected_child_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
        val PARENT_PIN_HASH = stringPreferencesKey("parent_pin_hash")
        val CHILD_WELCOME_SHOWN = booleanPreferencesKey("child_welcome_shown")
    }

    val prefsFlow: Flow<WahjSessionPrefs> = context.wahjDataStore.data.map { prefs ->
        val theme = runCatching {
            ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        WahjSessionPrefs(
            activeAccountId = prefs[Keys.ACTIVE_ACCOUNT_ID],
            activeRole = prefs[Keys.ACTIVE_ROLE],
            activeChildDeviceId = prefs[Keys.ACTIVE_CHILD_DEVICE_ID],
            selectedChildId = prefs[Keys.SELECTED_CHILD_ID],
            themeMode = theme,
            onboardingSeen = prefs[Keys.ONBOARDING_SEEN] ?: false,
            parentPinHash = prefs[Keys.PARENT_PIN_HASH],
            childWelcomeShown = prefs[Keys.CHILD_WELCOME_SHOWN] ?: false
        )
    }

    suspend fun setSession(accountId: String, role: String) {
        context.wahjDataStore.edit {
            it[Keys.ACTIVE_ACCOUNT_ID] = accountId
            it[Keys.ACTIVE_ROLE] = role
            it[Keys.ONBOARDING_SEEN] = true
        }
    }

    suspend fun setSelectedRole(role: String) {
        context.wahjDataStore.edit {
            it[Keys.ACTIVE_ROLE] = role
            it[Keys.ONBOARDING_SEEN] = true
        }
    }

    suspend fun setActiveChildDeviceId(childId: String) {
        context.wahjDataStore.edit {
            it[Keys.ACTIVE_CHILD_DEVICE_ID] = childId
            it[Keys.CHILD_WELCOME_SHOWN] = false
        }
    }

    suspend fun setSelectedChildId(childId: String?) {
        context.wahjDataStore.edit {
            if (childId == null) it.remove(Keys.SELECTED_CHILD_ID)
            else it[Keys.SELECTED_CHILD_ID] = childId
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.wahjDataStore.edit {
            it[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setParentPinHash(hash: String?) {
        context.wahjDataStore.edit {
            if (hash == null) it.remove(Keys.PARENT_PIN_HASH)
            else it[Keys.PARENT_PIN_HASH] = hash
        }
    }

    suspend fun markOnboardingSeen() {
        context.wahjDataStore.edit { it[Keys.ONBOARDING_SEEN] = true }
    }

    suspend fun markChildWelcomeShown() {
        context.wahjDataStore.edit { it[Keys.CHILD_WELCOME_SHOWN] = true }
    }

    suspend fun clearSession() {
        context.wahjDataStore.edit {
            it.remove(Keys.ACTIVE_ACCOUNT_ID)
            it.remove(Keys.ACTIVE_ROLE)
            it.remove(Keys.SELECTED_CHILD_ID)
            it.remove(Keys.ACTIVE_CHILD_DEVICE_ID)
        }
    }
}
