package com.animesh.timetable.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Persists the student's configuration: selected subjects, batch, and setup state. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val SUBJECTS = stringSetPreferencesKey("selected_subjects")
        val BATCH = stringPreferencesKey("batch")
        val SETUP_DONE = booleanPreferencesKey("setup_done")
    }

    val selectedSubjects: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.SUBJECTS] ?: emptySet() }

    val batch: Flow<String> =
        context.dataStore.data.map { it[Keys.BATCH] ?: "A" }

    val setupDone: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SETUP_DONE] ?: false }

    suspend fun setSubjects(subjects: Set<String>) {
        context.dataStore.edit { it[Keys.SUBJECTS] = subjects }
    }

    suspend fun setBatch(batch: String) {
        context.dataStore.edit { it[Keys.BATCH] = batch }
    }

    suspend fun setSetupDone(done: Boolean) {
        context.dataStore.edit { it[Keys.SETUP_DONE] = done }
    }
}
