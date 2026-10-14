package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val KEY_USER_PIN = stringPreferencesKey("user_pin")
        val KEY_LOGIN_ENABLED = booleanPreferencesKey("login_enabled")
        val KEY_LOGO_URI = stringPreferencesKey("custom_logo_uri")
        val KEY_API_ENDPOINT = stringPreferencesKey("api_endpoint")
        val KEY_API_TOKEN = stringPreferencesKey("api_token")
        val KEY_NOTIF_PRINT = booleanPreferencesKey("notif_print")
        val KEY_NOTIF_CUTOFF = booleanPreferencesKey("notif_cutoff")
        val KEY_DEFAULT_APPROVER_AREA = stringPreferencesKey("default_approver_area")
        val KEY_DEFAULT_APPROVER_PUSAT = stringPreferencesKey("default_approver_pusat")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val userPin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_PIN] ?: "1234"
    }

    val isLoginEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOGIN_ENABLED] ?: true
    }

    val customLogoUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOGO_URI]
    }

    val apiEndpoint: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_ENDPOINT] ?: "https://api.perusahaan.co.id/v1/hr/spkl-sync"
    }

    val apiToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_TOKEN] ?: ""
    }

    val notifPrint: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIF_PRINT] ?: true
    }

    val notifCutoff: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIF_CUTOFF] ?: true
    }

    val defaultApproverArea: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_APPROVER_AREA] ?: "Bpk. Hendra S. (Pim. Area)"
    }

    val defaultApproverPusat: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_APPROVER_PUSAT] ?: "Ibu Rina K. (Pim. Pusat)"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setUserPin(pin: String) {
        context.dataStore.edit { it[KEY_USER_PIN] = pin }
    }

    suspend fun setLoginEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LOGIN_ENABLED] = enabled }
    }

    suspend fun setCustomLogoUri(uri: String?) {
        context.dataStore.edit {
            if (uri != null) it[KEY_LOGO_URI] = uri else it.remove(KEY_LOGO_URI)
        }
    }

    suspend fun setApiConfig(endpoint: String, token: String) {
        context.dataStore.edit {
            it[KEY_API_ENDPOINT] = endpoint
            it[KEY_API_TOKEN] = token
        }
    }

    suspend fun setNotificationPrefs(print: Boolean, cutoff: Boolean) {
        context.dataStore.edit {
            it[KEY_NOTIF_PRINT] = print
            it[KEY_NOTIF_CUTOFF] = cutoff
        }
    }
}
