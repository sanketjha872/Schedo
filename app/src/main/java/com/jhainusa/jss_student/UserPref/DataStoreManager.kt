package com.jhainusa.jss_student.UserPref

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore("user_prefs")

object UserSession {
    var name: String? = null
}

object UserPreferences {
    private val NAME_KEY = stringPreferencesKey("user_name")
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    private val INITIAL_ATTENDANCE_DONE_KEY = booleanPreferencesKey("initial_attendance_done")
    private val SEMESTER_START_DATE_KEY = stringPreferencesKey("semester_start_date")
    private val SEMESTER_END_DATE_KEY = stringPreferencesKey("semester_end_date")

    private val BUNK_TOOLTIP_SHOWN_KEY = booleanPreferencesKey("bunk_tooltip_shown")
    private val SWIPE_TOOLTIP_SHOWN_KEY = booleanPreferencesKey("swipe_tooltip_shown")
    private val DESIRED_ATTENDANCE_KEY = stringPreferencesKey("desired_attendance")

    private val DESIRED_ATTENDANCE_DONE = booleanPreferencesKey("desired_attendance_done")
    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")

    suspend fun saveName(context: Context, name: String) {
        context.dataStore.edit { prefs ->
            prefs[NAME_KEY] = name

            val deviceId = getHardwareId(context)
            prefs[USER_ID_KEY] = "${deviceId}"
        }
    }

    fun getName(context: Context): Flow<String?> {
        return context.dataStore.data
            .map { prefs -> prefs[NAME_KEY]}
    }

    fun getNotificationsEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data
            .map { prefs -> prefs[NOTIFICATIONS_ENABLED_KEY] ?: true }
    }

    suspend fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    fun getDarkMode(context: Context): Flow<Boolean> {
        return context.dataStore.data
            .map { prefs -> prefs[DARK_MODE_KEY] ?: false }
    }

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = enabled
        }
    }
    private fun getHardwareId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString()
    }
    suspend fun getOrCreateUserId(context: Context): String {
        val prefs = context.dataStore.data.first()
        val existingId = prefs[USER_ID_KEY]

        return if (existingId != null) {
            existingId
        } else {
            val deviceId = getHardwareId(context)
            val newId = "${deviceId}"
            context.dataStore.edit { settings ->
                settings[USER_ID_KEY] = newId
            }
            newId
        }
    }

    // --- Bunk Tooltip Feature Discovery ---
    fun isBunkTooltipShown(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs -> prefs[BUNK_TOOLTIP_SHOWN_KEY] ?: false }
    }

    suspend fun setBunkTooltipShown(context: Context) {
        context.dataStore.edit { prefs -> prefs[BUNK_TOOLTIP_SHOWN_KEY] = true }
    }

    // --- Swipe Tooltip Feature Discovery ---
    fun isSwipeTooltipShown(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs -> prefs[SWIPE_TOOLTIP_SHOWN_KEY] ?: false }
    }

    suspend fun setSwipeTooltipShown(context: Context) {
        context.dataStore.edit { prefs -> prefs[SWIPE_TOOLTIP_SHOWN_KEY] = true }
    }

    suspend fun saveDesiredAttendance(context: Context, attendance: Float) {
        context.dataStore.edit { prefs ->
            prefs[DESIRED_ATTENDANCE_KEY] = attendance.toString()
        }
    }

    fun getDesiredAttendance(context: Context): Flow<Float> {
        return context.dataStore.data.map { prefs ->
            prefs[DESIRED_ATTENDANCE_KEY]?.toFloatOrNull() ?: 75f
        }
    }
    suspend fun setDesiredAttendanceDone(context: Context, done: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DESIRED_ATTENDANCE_DONE] = done
        }
    }

    fun isDesiredAttendanceDone(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[DESIRED_ATTENDANCE_DONE] ?: false
        }
    }

    suspend fun skipOnboarding(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] = true
            prefs[DESIRED_ATTENDANCE_DONE] = true
            prefs[DESIRED_ATTENDANCE_KEY] = "75.0"
        }
    }

    suspend fun completeDesiredAttendance(context: Context, attendance: Float) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] = true
            prefs[DESIRED_ATTENDANCE_DONE] = true
            prefs[DESIRED_ATTENDANCE_KEY] = attendance.toString()
        }
    }

    suspend fun setOnboardingCompleted(context: Context, completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    fun isOnboardingCompleted(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] ?: false
        }
    }

    suspend fun setInitialAttendanceDone(context: Context, done: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[INITIAL_ATTENDANCE_DONE_KEY] = done
        }
    }

    fun isInitialAttendanceDone(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[INITIAL_ATTENDANCE_DONE_KEY] ?: false
        }
    }

    suspend fun setSemesterStartDate(context: Context, date: String) {
        context.dataStore.edit { prefs ->
            prefs[SEMESTER_START_DATE_KEY] = date
        }
    }

    fun getSemesterStartDate(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[SEMESTER_START_DATE_KEY]
        }
    }

    suspend fun setSemesterEndDate(context: Context, date: String) {
        context.dataStore.edit { prefs ->
            prefs[SEMESTER_END_DATE_KEY] = date
        }
    }

    fun getSemesterEndDate(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[SEMESTER_END_DATE_KEY]
        }
    }
}

