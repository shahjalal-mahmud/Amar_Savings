package com.appriyo.amarsavings.data.db

import android.content.Context
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.prefs.Preferences

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "amar_savings_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val SAVINGS_GOAL = longPreferencesKey("savings_goal")
    }

    val savingsGoal: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[SAVINGS_GOAL] ?: 0L
    }

    suspend fun setSavingsGoal(amount: Long) {
        context.dataStore.edit { prefs ->
            prefs[SAVINGS_GOAL] = amount
        }
    }
}