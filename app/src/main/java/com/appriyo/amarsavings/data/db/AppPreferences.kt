package com.appriyo.amarsavings.data.db

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "amar_savings_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        // ── Existing keys ─────────────────────────────────────────────────────
        private val SAVINGS_GOAL = longPreferencesKey("savings_goal")
        private val THEME_MODE = stringPreferencesKey("theme_mode")

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        // ── Auth keys ─────────────────────────────────────────────────────────
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        private val USER_PHOTO_URL = stringPreferencesKey("user_photo_url")

        // ── Backup keys ───────────────────────────────────────────────────────
        private val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        private val DRIVE_BACKUP_FILE_ID = stringPreferencesKey("drive_backup_file_id")
        private val LOCAL_STATE_HASH = stringPreferencesKey("local_state_hash")
    }

    // ── Existing flows ──────────────────────────────────────────────────────

    val savingsGoal: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[SAVINGS_GOAL] ?: 0L
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: THEME_LIGHT
    }

    // ── Auth flows ──────────────────────────────────────────────────────────

    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL] }
    val userDisplayName: Flow<String?> = context.dataStore.data.map { it[USER_DISPLAY_NAME] }
    val userPhotoUrl: Flow<String?> = context.dataStore.data.map { it[USER_PHOTO_URL] }

    // ── Backup flows ────────────────────────────────────────────────────────

    val lastBackupAt: Flow<Long> = context.dataStore.data.map { it[LAST_BACKUP_AT] ?: 0L }

    val driveBackupFileId: Flow<String?> = context.dataStore.data.map { it[DRIVE_BACKUP_FILE_ID] }

    val localStateHash: Flow<String?> = context.dataStore.data.map { it[LOCAL_STATE_HASH] }

    // ── Suspend setters ─────────────────────────────────────────────────────

    suspend fun setSavingsGoal(amount: Long) {
        context.dataStore.edit { prefs -> prefs[SAVINGS_GOAL] = amount }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }

    suspend fun setUserProfile(email: String, displayName: String?, photoUrl: String?) {
        context.dataStore.edit { prefs ->
            prefs[USER_EMAIL] = email
            if (displayName != null) prefs[USER_DISPLAY_NAME] = displayName
            if (photoUrl != null) prefs[USER_PHOTO_URL] = photoUrl
        }
    }

    suspend fun setLastBackupAt(timestamp: Long) {
        context.dataStore.edit { prefs -> prefs[LAST_BACKUP_AT] = timestamp }
    }

    suspend fun setDriveBackupFileId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id == null) prefs.remove(DRIVE_BACKUP_FILE_ID)
            else prefs[DRIVE_BACKUP_FILE_ID] = id
        }
    }

    suspend fun setLocalStateHash(hash: String?) {
        context.dataStore.edit { prefs ->
            if (hash == null) prefs.remove(LOCAL_STATE_HASH)
            else prefs[LOCAL_STATE_HASH] = hash
        }
    }

    /** Removes auth + backup-related prefs but keeps theme + savings goal. */
    suspend fun clearAuthAndBackup() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_EMAIL)
            prefs.remove(USER_DISPLAY_NAME)
            prefs.remove(USER_PHOTO_URL)
            prefs.remove(LAST_BACKUP_AT)
            prefs.remove(DRIVE_BACKUP_FILE_ID)
            prefs.remove(LOCAL_STATE_HASH)
        }
    }

    /** Synchronous-style snapshot helpers for use from non-Composable callers (e.g. backup). */
    suspend fun getDriveBackupFileIdNow(): String? = driveBackupFileId.first()
    suspend fun getLocalStateHashNow(): String? = localStateHash.first()
}