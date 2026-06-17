package com.animesh.timetable.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** App-level configuration: active module, section, theme, and per-module setup state. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val ACTIVE_MODULE = longPreferencesKey("active_module")
        val SECTION = stringPreferencesKey("section")
        val THEME = stringPreferencesKey("theme_mode")
        val CONFIGURED = stringSetPreferencesKey("configured_modules")
    }

    val activeModuleId: Flow<Long> =
        context.dataStore.data.map { it[Keys.ACTIVE_MODULE] ?: -1L }

    val section: Flow<String> =
        context.dataStore.data.map { it[Keys.SECTION] ?: "F" }

    val themeMode: Flow<ThemeMode> =
        context.dataStore.data.map { runCatching { ThemeMode.valueOf(it[Keys.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM) }

    val configuredModules: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.CONFIGURED] ?: emptySet() }

    suspend fun setActiveModule(id: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_MODULE] = id }
    }

    suspend fun setSection(section: String) {
        context.dataStore.edit { it[Keys.SECTION] = section }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun markConfigured(id: Long) {
        context.dataStore.edit {
            it[Keys.CONFIGURED] = (it[Keys.CONFIGURED] ?: emptySet()) + id.toString()
        }
    }

    suspend fun unmarkConfigured(id: Long) {
        context.dataStore.edit {
            it[Keys.CONFIGURED] = (it[Keys.CONFIGURED] ?: emptySet()) - id.toString()
        }
    }
}
