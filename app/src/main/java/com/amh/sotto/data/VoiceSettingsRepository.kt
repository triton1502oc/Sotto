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
    val shareUsabilityMetrics: Boolean = false
)

interface VoiceSettingsRepository {
    fun getVoiceSettings(): VoiceSettings
    fun saveVoiceSettings(settings: VoiceSettings)
    fun getLastPromptedMetricsVersion(): Int
    fun setLastPromptedMetricsVersion(versionCode: Int)
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
    }

    override fun getVoiceSettings(): VoiceSettings {
        val rate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        val pitch = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        val voiceName = prefs.getString(KEY_VOICE_NAME, null)
        val attentionChime = prefs.getBoolean(KEY_ATTENTION_CHIME, false)
        val showLanguageSwitcher = prefs.getBoolean(KEY_SHOW_LANGUAGE_SWITCHER, false)
        val secondaryLanguage = prefs.getString(KEY_SECONDARY_LANGUAGE, "id") ?: "id"
        val shareMetrics = prefs.getBoolean(KEY_SHARE_USABILITY_METRICS, false)
        return VoiceSettings(
            speechRate = rate,
            speechPitch = pitch,
            voiceName = voiceName,
            playAttentionChime = attentionChime,
            showLanguageSwitcher = showLanguageSwitcher,
            secondaryLanguage = secondaryLanguage,
            shareUsabilityMetrics = shareMetrics
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
        editor.apply()

        // If metrics were enabled and are now toggled OFF, immediately purge the buffer
        if (wasMetricsEnabled && !settings.shareUsabilityMetrics) {
            UsabilityTracker.clearBuffer(context)
        }
    }

    override fun getLastPromptedMetricsVersion(): Int {
        return prefs.getInt(KEY_METRICS_PROMPT_VERSION_CODE, 0)
    }

    override fun setLastPromptedMetricsVersion(versionCode: Int) {
        prefs.edit().putInt(KEY_METRICS_PROMPT_VERSION_CODE, versionCode).apply()
    }
}
