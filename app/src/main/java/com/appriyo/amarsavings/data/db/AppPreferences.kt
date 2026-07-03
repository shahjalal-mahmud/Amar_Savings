package com.appriyo.amarsavings.data.db

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "amar_savings_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val SAVINGS_GOAL = longPreferencesKey("savings_goal")
        private val THEME_MODE = stringPreferencesKey("theme_mode")

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }

    val savingsGoal: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[SAVINGS_GOAL] ?: 0L
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: THEME_LIGHT
    }

    suspend fun setSavingsGoal(amount: Long) {
        context.dataStore.edit { prefs ->
            prefs[SAVINGS_GOAL] = amount
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode
        }
    }
}