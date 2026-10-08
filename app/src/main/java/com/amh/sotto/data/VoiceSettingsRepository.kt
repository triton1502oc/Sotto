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
    val userRole: String = ROLE_UNSET,
    val appTheme: String = THEME_CHARCOAL,
    val showSafetyCards: Boolean = true
) {
    companion object {
        const val ROLE_UNSET = "unset"
        const val ROLE_CAREGIVER = "caregiver"
        const val ROLE_SELF = "self"
        const val ROLE_PROFESSIONAL = "professional"

        const val THEME_CHARCOAL = "charcoal"
        const val THEME_OLED_BLACK = "oled_black"
        const val THEME_WARM_AMBER = "warm_amber"
        const val THEME_SLATE_NAVY = "slate_navy"
        const val THEME_SOFT_PARCHMENT = "soft_parchment"
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
    fun getFirstInstalledAt(): Long
    fun recordActiveDay(todayDate: String)
    fun getActiveDayCount(): Int
    fun hasPromptedPmfSurvey(): Boolean
    fun setPromptedPmfSurvey(prompted: Boolean)
    fun shouldPromptPmfSurvey(): Boolean
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
        private const val KEY_FIRST_INSTALLED_AT = "first_installed_at"
        private const val KEY_ACTIVE_DAYS = "active_days"
        private const val KEY_PMF_SURVEY_PROMPTED = "pmf_survey_prompted"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_SHOW_SAFETY_CARDS = "show_safety_cards"
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
        val rawTheme = prefs.getString(KEY_APP_THEME, VoiceSettings.THEME_CHARCOAL)
        val theme = if (rawTheme.isNullOrBlank()) VoiceSettings.THEME_CHARCOAL else rawTheme
        val safetyCards = prefs.getBoolean(KEY_SHOW_SAFETY_CARDS, true)
        return VoiceSettings(
            speechRate = rate,
            speechPitch = pitch,
            voiceName = voiceName,
            playAttentionChime = attentionChime,
            showLanguageSwitcher = showLanguageSwitcher,
            secondaryLanguage = secondaryLanguage,
            shareUsabilityMetrics = shareMetrics,
            userRole = role,
            appTheme = theme,
            showSafetyCards = safetyCards
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
        editor.putString(KEY_APP_THEME, settings.appTheme)
        editor.putBoolean(KEY_SHOW_SAFETY_CARDS, settings.showSafetyCards)
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

    override fun getFirstInstalledAt(): Long {
        val existing = prefs.getLong(KEY_FIRST_INSTALLED_AT, 0L)
        if (existing > 0L) return existing
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_FIRST_INSTALLED_AT, now).apply()
        return now
    }

    override fun recordActiveDay(todayDate: String) {
        val currentDays = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!currentDays.contains(todayDate)) {
            currentDays.add(todayDate)
            prefs.edit().putStringSet(KEY_ACTIVE_DAYS, currentDays).apply()
        }
    }

    override fun getActiveDayCount(): Int {
        return prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet())?.size ?: 0
    }

    override fun hasPromptedPmfSurvey(): Boolean {
        return prefs.getBoolean(KEY_PMF_SURVEY_PROMPTED, false)
    }

    override fun setPromptedPmfSurvey(prompted: Boolean) {
        prefs.edit().putBoolean(KEY_PMF_SURVEY_PROMPTED, prompted).apply()
    }

    override fun shouldPromptPmfSurvey(): Boolean {
        val settings = getVoiceSettings()
        if (!settings.shareUsabilityMetrics) return false
        if (hasPromptedPmfSurvey()) return false
        val installAge = System.currentTimeMillis() - getFirstInstalledAt()
        val days14Ms = 14L * 24 * 60 * 60 * 1000L
        return installAge >= days14Ms && getActiveDayCount() >= 5
    }
}
