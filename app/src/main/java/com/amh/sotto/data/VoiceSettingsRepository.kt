package com.amh.sotto.data

import android.content.Context
import android.content.SharedPreferences
import com.amh.sotto.util.UsabilityTracker

data class VoiceSettings(
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val voiceName: String? = null,
    val playAttentionChime: Boolean = false,
    val showLanguageSwitcher: Boolean = false,
    val secondaryLanguage: String = "id",
    val shareUsabilityMetrics: Boolean = false,
    val userRole: String = ROLE_UNSET
) {
    companion object {
        const val ROLE_UNSET = "unset"
        const val ROLE_CAREGIVER = "caregiver"
        const val ROLE_SELF = "self"
        const val ROLE_PROFESSIONAL = "professional"
    }
}

interface VoiceSettingsRepository {
    fun getVoiceSettings(): VoiceSettings
    fun saveVoiceSettings(settings: VoiceSettings)
    fun getLastPromptedMetricsVersion(): Int
    fun setLastPromptedMetricsVersion(versionCode: Int)
    fun getInstallId(): String?
    fun getOrGenerateInstallId(): String
    fun clearInstallId()
    fun hasPromptedRole(): Boolean
    fun setPromptedRole(prompted: Boolean)
    fun getUserRole(): String
    fun setUserRole(role: String)
}

class SharedPreferencesVoiceSettingsRepository(private val context: Context) : VoiceSettingsRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("sotto_voice_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_VOICE_NAME = "voice_name"
        private const val KEY_ATTENTION_CHIME = "attention_chime"
        private const val KEY_SHOW_LANGUAGE_SWITCHER = "show_language_switcher"
        private const val KEY_SECONDARY_LANGUAGE = "secondary_language"
        private const val KEY_SHARE_USABILITY_METRICS = "share_usability_metrics"
        private const val KEY_METRICS_PROMPT_VERSION_CODE = "metrics_prompt_version_code"
        private const val KEY_INSTALL_ID = "telemetry_install_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_ROLE_PROMPTED = "role_prompted"
    }

    override fun getVoiceSettings(): VoiceSettings {
        val rate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        val pitch = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        val voiceName = prefs.getString(KEY_VOICE_NAME, null)
        val attentionChime = prefs.getBoolean(KEY_ATTENTION_CHIME, false)
        val showLanguageSwitcher = prefs.getBoolean(KEY_SHOW_LANGUAGE_SWITCHER, false)
        val secondaryLanguage = prefs.getString(KEY_SECONDARY_LANGUAGE, "id") ?: "id"
        val shareMetrics = prefs.getBoolean(KEY_SHARE_USABILITY_METRICS, false)
        val role = prefs.getString(KEY_USER_ROLE, VoiceSettings.ROLE_UNSET) ?: VoiceSettings.ROLE_UNSET
        return VoiceSettings(
            speechRate = rate,
            speechPitch = pitch,
            voiceName = voiceName,
            playAttentionChime = attentionChime,
            showLanguageSwitcher = showLanguageSwitcher,
            secondaryLanguage = secondaryLanguage,
            shareUsabilityMetrics = shareMetrics,
            userRole = role
        )
    }

    override fun saveVoiceSettings(settings: VoiceSettings) {
        val wasMetricsEnabled = prefs.getBoolean(KEY_SHARE_USABILITY_METRICS, false)
        val editor = prefs.edit()
        editor.putFloat(KEY_SPEECH_RATE, settings.speechRate)
        editor.putFloat(KEY_SPEECH_PITCH, settings.speechPitch)
        editor.putString(KEY_VOICE_NAME, settings.voiceName)
        editor.putBoolean(KEY_ATTENTION_CHIME, settings.playAttentionChime)
        editor.putBoolean(KEY_SHOW_LANGUAGE_SWITCHER, settings.showLanguageSwitcher)
        editor.putString(KEY_SECONDARY_LANGUAGE, settings.secondaryLanguage)
        editor.putBoolean(KEY_SHARE_USABILITY_METRICS, settings.shareUsabilityMetrics)
        editor.putString(KEY_USER_ROLE, settings.userRole)
        editor.apply()

        if (settings.shareUsabilityMetrics) {
            getOrGenerateInstallId()
        } else if (wasMetricsEnabled) {
            clearInstallId()
            UsabilityTracker.clearBuffer(context)
        }
    }

    override fun getLastPromptedMetricsVersion(): Int {
        return prefs.getInt(KEY_METRICS_PROMPT_VERSION_CODE, 0)
    }

    override fun setLastPromptedMetricsVersion(versionCode: Int) {
        prefs.edit().putInt(KEY_METRICS_PROMPT_VERSION_CODE, versionCode).apply()
    }

    override fun getInstallId(): String? {
        return prefs.getString(KEY_INSTALL_ID, null)
    }

    override fun getOrGenerateInstallId(): String {
        val existing = prefs.getString(KEY_INSTALL_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val newId = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALL_ID, newId).apply()
        return newId
    }

    override fun clearInstallId() {
        prefs.edit().remove(KEY_INSTALL_ID).apply()
    }

    override fun hasPromptedRole(): Boolean {
        return prefs.getBoolean(KEY_ROLE_PROMPTED, false)
    }

    override fun setPromptedRole(prompted: Boolean) {
        prefs.edit().putBoolean(KEY_ROLE_PROMPTED, prompted).apply()
    }

    override fun getUserRole(): String {
        return prefs.getString(KEY_USER_ROLE, VoiceSettings.ROLE_UNSET) ?: VoiceSettings.ROLE_UNSET
    }

    override fun setUserRole(role: String) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply()
    }
}
