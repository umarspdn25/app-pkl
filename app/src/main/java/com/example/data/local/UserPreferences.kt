package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "pkl_tsm_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_ACTIVE_NISN = stringPreferencesKey("active_nisn")
        val KEY_SESSION_TOKEN = stringPreferencesKey("session_token")
        val KEY_ADMIN_TOKEN = stringPreferencesKey("admin_token")
        val KEY_GAS_WEBAPP_URL = stringPreferencesKey("gas_webapp_url")
        val KEY_CONFIG_VERSION = stringPreferencesKey("config_version")
        val KEY_LAST_SYNC = longPreferencesKey("last_sync_time")

        // Schedule settings cache
        val KEY_JAM_MASUK = stringPreferencesKey("jam_masuk")
        val KEY_BATAS_TOLERANSI = stringPreferencesKey("batas_toleransi")
        val KEY_JAM_PULANG = stringPreferencesKey("jam_pulang")
        val KEY_HARI_OPERASIONAL = stringPreferencesKey("hari_operasional")
        val KEY_LIBUR_NASIONAL_AKTIF = booleanPreferencesKey("libur_nasional_aktif")
        val KEY_HOLIDAY_OVERRIDE = booleanPreferencesKey("holiday_override")

        // Default Google Apps Script URL placeholder
        const val DEFAULT_GAS_URL = "https://script.google.com/macros/s/AKfycbz_SAMPLE_DEPLOYMENT_URL/exec"
    }

    val activeNisn: Flow<String?> = context.dataStore.data.map { it[KEY_ACTIVE_NISN] }
    val sessionToken: Flow<String?> = context.dataStore.data.map { it[KEY_SESSION_TOKEN] }
    val adminToken: Flow<String?> = context.dataStore.data.map { it[KEY_ADMIN_TOKEN] }
    val gasWebappUrl: Flow<String> = context.dataStore.data.map { it[KEY_GAS_WEBAPP_URL] ?: "" }
    val lastSyncTime: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_SYNC] ?: 0L }

    val jamMasuk: Flow<String> = context.dataStore.data.map { it[KEY_JAM_MASUK] ?: "07:00" }
    val batasToleransi: Flow<String> = context.dataStore.data.map { it[KEY_BATAS_TOLERANSI] ?: "09:00" }
    val jamPulang: Flow<String> = context.dataStore.data.map { it[KEY_JAM_PULANG] ?: "15:00" }
    val hariOperasional: Flow<String> = context.dataStore.data.map { it[KEY_HARI_OPERASIONAL] ?: "1,2,3,4,5,6" }
    val liburNasionalAktif: Flow<Boolean> = context.dataStore.data.map { it[KEY_LIBUR_NASIONAL_AKTIF] ?: true }
    val holidayOverride: Flow<Boolean> = context.dataStore.data.map { it[KEY_HOLIDAY_OVERRIDE] ?: false }

    suspend fun saveStudentSession(nisn: String, token: String) {
        context.dataStore.edit {
            it[KEY_ACTIVE_NISN] = nisn
            it[KEY_SESSION_TOKEN] = token
        }
    }

    suspend fun clearStudentSession() {
        context.dataStore.edit {
            it.remove(KEY_ACTIVE_NISN)
            it.remove(KEY_SESSION_TOKEN)
        }
    }

    suspend fun saveAdminSession(adminToken: String) {
        context.dataStore.edit {
            it[KEY_ADMIN_TOKEN] = adminToken
        }
    }

    suspend fun clearAdminSession() {
        context.dataStore.edit {
            it.remove(KEY_ADMIN_TOKEN)
        }
    }

    suspend fun saveGasWebappUrl(url: String) {
        context.dataStore.edit {
            it[KEY_GAS_WEBAPP_URL] = url
        }
    }

    suspend fun saveScheduleConfig(
        masuk: String,
        toleransi: String,
        pulang: String,
        hari: String,
        liburAktif: Boolean,
        version: String
    ) {
        context.dataStore.edit {
            it[KEY_JAM_MASUK] = masuk
            it[KEY_BATAS_TOLERANSI] = toleransi
            it[KEY_JAM_PULANG] = pulang
            it[KEY_HARI_OPERASIONAL] = hari
            it[KEY_LIBUR_NASIONAL_AKTIF] = liburAktif
            it[KEY_CONFIG_VERSION] = version
            it[KEY_LAST_SYNC] = System.currentTimeMillis()
        }
    }

    suspend fun setHolidayOverride(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_HOLIDAY_OVERRIDE] = enabled
        }
    }
}
