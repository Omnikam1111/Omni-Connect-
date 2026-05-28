package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("crm_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock"
        private const val KEY_REAL_PIN = "real_pin"
        private const val KEY_DECOY_PIN = "decoy_pin"
        private const val KEY_HAS_PIN = "has_pin"
    }

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, value).apply()

    var realPin: String
        get() = prefs.getString(KEY_REAL_PIN, "1234") ?: "1234"
        set(value) = prefs.edit().putString(KEY_REAL_PIN, value).apply()

    var decoyPin: String
        get() = prefs.getString(KEY_DECOY_PIN, "5555") ?: "5555"
        set(value) = prefs.edit().putString(KEY_DECOY_PIN, value).apply()

    var hasPinConfigured: Boolean
        get() = prefs.getBoolean(KEY_HAS_PIN, true) // Default to true so they enter the default 1234 on start
        set(value) = prefs.edit().putBoolean(KEY_HAS_PIN, value).apply()

    var isCustomThemeEnabled: Boolean
        get() = prefs.getBoolean("is_cust_theme", false)
        set(value) = prefs.edit().putBoolean("is_cust_theme", value).apply()

    var isCustomThemeUnlocked: Boolean
        get() = prefs.getBoolean("is_cust_theme_unlocked", false)
        set(value) = prefs.edit().putBoolean("is_cust_theme_unlocked", value).apply()

    var selectedThemePreset: String
        get() = prefs.getString("selected_theme_preset", "classic") ?: "classic"
        set(value) = prefs.edit().putString("selected_theme_preset", value).apply()

    var customPrimaryColor: Int
        get() = prefs.getInt("cust_prime", 0xFFD0BCFF.toInt())
        set(value) = prefs.edit().putInt("cust_prime", value).apply()

    var customSecondaryColor: Int
        get() = prefs.getInt("cust_sec", 0xFFCCC2DC.toInt())
        set(value) = prefs.edit().putInt("cust_sec", value).apply()

    var customBackgroundColor: Int
        get() = prefs.getInt("cust_bg", 0xFF141218.toInt())
        set(value) = prefs.edit().putInt("cust_bg", value).apply()

    var customSurfaceColor: Int
        get() = prefs.getInt("cust_surf", 0xFF1D1B20.toInt())
        set(value) = prefs.edit().putInt("cust_surf", value).apply()

    var customDeleteColor: Int
        get() = prefs.getInt("cust_del", 0xFFFF5252.toInt())
        set(value) = prefs.edit().putInt("cust_del", value).apply()

    // SMTP Configuration Keys for Live Automated Emails
    var smtpHost: String
        get() = prefs.getString("smtp_host", "smtp.gmail.com") ?: "smtp.gmail.com"
        set(value) = prefs.edit().putString("smtp_host", value).apply()

    var smtpPort: String
        get() = prefs.getString("smtp_port", "587") ?: "587"
        set(value) = prefs.edit().putString("smtp_port", value).apply()

    var smtpUsername: String
        get() = prefs.getString("smtp_username", "") ?: ""
        set(value) = prefs.edit().putString("smtp_username", value).apply()

    var smtpPassword: String
        get() = prefs.getString("smtp_password", "") ?: ""
        set(value) = prefs.edit().putString("smtp_password", value).apply()

    var smtpSender: String
        get() = prefs.getString("smtp_sender", "") ?: ""
        set(value) = prefs.edit().putString("smtp_sender", value).apply()

    var smtpUseSsl: Boolean
        get() = prefs.getBoolean("smtp_ssl", false)
        set(value) = prefs.edit().putBoolean("smtp_ssl", value).apply()

    var smtpUseTls: Boolean
        get() = prefs.getBoolean("smtp_tls", true)
        set(value) = prefs.edit().putBoolean("smtp_tls", value).apply()

    // Google Sign-In and Gmail API preferences
    var googleAccountEmail: String
        get() = prefs.getString("google_acct_email", "") ?: ""
        set(value) = prefs.edit().putString("google_acct_email", value).apply()

    var googleAccountName: String
        get() = prefs.getString("google_acct_name", "") ?: ""
        set(value) = prefs.edit().putString("google_acct_name", value).apply()

    var googleOAuthClientId: String
        get() = prefs.getString("google_oauth_client_id", "") ?: ""
        set(value) = prefs.edit().putString("google_oauth_client_id", value).apply()

    var googleOAuthClientSecret: String
        get() = prefs.getString("google_oauth_client_secret", "") ?: ""
        set(value) = prefs.edit().putString("google_oauth_client_secret", value).apply()

    var googleRefreshToken: String
        get() = prefs.getString("google_refresh_token", "") ?: ""
        set(value) = prefs.edit().putString("google_refresh_token", value).apply()

    var googleAccessToken: String
        get() = prefs.getString("google_access_token", "") ?: ""
        set(value) = prefs.edit().putString("google_access_token", value).apply()

    var googleTokenExpiry: Long
        get() = prefs.getLong("google_token_expiry", 0L)
        set(value) = prefs.edit().putLong("google_token_expiry", value).apply()

    // GLSL Background Shader settings
    var isShaderBackgroundEnabled: Boolean
        get() = prefs.getBoolean("shader_bg_enabled", false)
        set(value) = prefs.edit().putBoolean("shader_bg_enabled", value).apply()

    var selectedShaderPreset: String
        get() = prefs.getString("selected_shader_preset", "stars") ?: "stars"
        set(value) = prefs.edit().putString("selected_shader_preset", value).apply()

    var customShaderCode: String
        get() = prefs.getString("custom_shader_code", "") ?: ""
        set(value) = prefs.edit().putString("custom_shader_code", value).apply()

    var shaderSeed: Float
        get() = prefs.getFloat("shader_seed", 1.0f)
        set(value) = prefs.edit().putFloat("shader_seed", value).apply()

    var contactCardOpacity: Float
        get() = prefs.getFloat("contact_card_opacity", 1.0f)
        set(value) = prefs.edit().putFloat("contact_card_opacity", value).apply()

    var savedSortOrder: String
        get() = prefs.getString("saved_sort_order", "FIRST_NAME_ASC") ?: "FIRST_NAME_ASC"
        set(value) = prefs.edit().putString("saved_sort_order", value).apply()

    var appLanguage: String
        get() = prefs.getString("app_language", "en") ?: "en"
        set(value) = prefs.edit().putString("app_language", value).apply()

    fun getSavedShaderNames(): Set<String> {
        return prefs.getStringSet("saved_shader_names", emptySet()) ?: emptySet()
    }

    fun saveCustomShader(name: String, code: String) {
        val updatedSet = getSavedShaderNames().toMutableSet()
        updatedSet.add(name)
        prefs.edit()
            .putStringSet("saved_shader_names", updatedSet)
            .putString("saved_shader_code_$name", code)
            .apply()
    }

    fun deleteSavedShader(name: String) {
        val updatedSet = getSavedShaderNames().toMutableSet()
        if (updatedSet.remove(name)) {
            prefs.edit()
                .putStringSet("saved_shader_names", updatedSet)
                .remove("saved_shader_code_$name")
                .apply()
        }
    }

    fun getSavedShaderCode(name: String): String {
        return prefs.getString("saved_shader_code_$name", "") ?: ""
    }

    fun saveCustomShaders(shaders: List<Pair<String, String>>) {
        val updatedSet = getSavedShaderNames().toMutableSet()
        val editor = prefs.edit()
        for (shader in shaders) {
            updatedSet.add(shader.first)
            editor.putString("saved_shader_code_${shader.first}", shader.second)
        }
        editor.putStringSet("saved_shader_names", updatedSet)
        editor.apply()
    }

    fun importPreferences(
        isBiometricEnabled: Boolean,
        realPin: String,
        decoyPin: String,
        hasPinConfigured: Boolean,
        isCustomThemeEnabled: Boolean,
        selectedThemePreset: String,
        customPrimaryColor: Int,
        customSecondaryColor: Int,
        customBackgroundColor: Int,
        customSurfaceColor: Int,
        customDeleteColor: Int,
        smtpHost: String,
        smtpPort: String,
        smtpUsername: String,
        smtpPassword: String,
        smtpSender: String,
        smtpUseSsl: Boolean,
        smtpUseTls: Boolean,
        googleAccountEmail: String,
        googleAccountName: String,
        googleOAuthClientId: String,
        googleOAuthClientSecret: String,
        googleRefreshToken: String,
        isShaderBackgroundEnabled: Boolean,
        selectedShaderPreset: String,
        customShaderCode: String,
        shaderSeed: Float,
        contactCardOpacity: Float
    ) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_BIOMETRIC_LOCK, isBiometricEnabled)
        editor.putString(KEY_REAL_PIN, realPin)
        editor.putString(KEY_DECOY_PIN, decoyPin)
        editor.putBoolean(KEY_HAS_PIN, hasPinConfigured)
        editor.putBoolean("is_cust_theme", isCustomThemeEnabled)
        editor.putString("selected_theme_preset", selectedThemePreset)
        editor.putInt("cust_prime", customPrimaryColor)
        editor.putInt("cust_sec", customSecondaryColor)
        editor.putInt("cust_bg", customBackgroundColor)
        editor.putInt("cust_surf", customSurfaceColor)
        editor.putInt("cust_del", customDeleteColor)
        editor.putString("smtp_host", smtpHost)
        editor.putString("smtp_port", smtpPort)
        editor.putString("smtp_username", smtpUsername)
        editor.putString("smtp_password", smtpPassword)
        editor.putString("smtp_sender", smtpSender)
        editor.putBoolean("smtp_ssl", smtpUseSsl)
        editor.putBoolean("smtp_tls", smtpUseTls)
        editor.putString("google_acct_email", googleAccountEmail)
        editor.putString("google_acct_name", googleAccountName)
        editor.putString("google_oauth_client_id", googleOAuthClientId)
        editor.putString("google_oauth_client_secret", googleOAuthClientSecret)
        editor.putString("google_refresh_token", googleRefreshToken)
        editor.putBoolean("shader_bg_enabled", isShaderBackgroundEnabled)
        editor.putString("selected_shader_preset", selectedShaderPreset)
        editor.putString("custom_shader_code", customShaderCode)
        editor.putFloat("shader_seed", shaderSeed)
        editor.putFloat("contact_card_opacity", contactCardOpacity)
        editor.apply()
    }
}
