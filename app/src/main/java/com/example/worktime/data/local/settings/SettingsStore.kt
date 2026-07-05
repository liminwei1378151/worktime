package com.example.worktime.data.local.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "worktime_settings")

class SettingsStore(
    private val context: Context
) {
    val defaultRemindMinutes: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_REMIND_MINUTES] ?: DEFAULT_REMIND_MINUTES
    }

    suspend fun setDefaultRemindMinutes(value: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_DEFAULT_REMIND_MINUTES] = value
        }
    }

    companion object {
        const val DEFAULT_REMIND_MINUTES = 30
        private val KEY_DEFAULT_REMIND_MINUTES = intPreferencesKey("default_remind_minutes")
    }
}
