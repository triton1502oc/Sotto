package com.amh.sotto.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VoiceSettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var repository: SharedPreferencesVoiceSettingsRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("sotto_voice_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor

        repository = SharedPreferencesVoiceSettingsRepository(context)
    }

    @Test
    fun `getVoiceSettings returns defaults when preferences empty`() {
        every { sharedPreferences.getFloat("speech_rate", 1.0f) } returns 1.0f
        every { sharedPreferences.getFloat("speech_pitch", 1.0f) } returns 1.0f
        every { sharedPreferences.getString("voice_name", null) } returns null
        every { sharedPreferences.getBoolean("attention_chime", false) } returns false
        every { sharedPreferences.getBoolean("show_language_switcher", false) } returns false
        every { sharedPreferences.getString("secondary_language", "id") } returns "id"
        every { sharedPreferences.getBoolean("share_usability_metrics", false) } returns false
        every { sharedPreferences.getString("app_theme", VoiceSettings.THEME_CHARCOAL) } returns VoiceSettings.THEME_CHARCOAL
        every { sharedPreferences.getBoolean("show_safety_cards", true) } returns true

        val settings = repository.getVoiceSettings()

        assertEquals(1.0f, settings.speechRate)
        assertEquals(1.0f, settings.speechPitch)
        assertEquals(null, settings.voiceName)
        assertEquals(false, settings.playAttentionChime)
        assertEquals(false, settings.showLanguageSwitcher)
        assertEquals("id", settings.secondaryLanguage)
        assertEquals(false, settings.shareUsabilityMetrics)
        assertEquals(VoiceSettings.THEME_CHARCOAL, settings.appTheme)
        assertEquals(true, settings.showSafetyCards)
    }

    @Test
    fun `getVoiceSettings returns saved values`() {
        every { sharedPreferences.getFloat("speech_rate", 1.0f) } returns 0.8f
        every { sharedPreferences.getFloat("speech_pitch", 1.0f) } returns 1.2f
        every { sharedPreferences.getString("voice_name", null) } returns "en-us-x-sfg#male_1"
        every { sharedPreferences.getBoolean("attention_chime", false) } returns true
        every { sharedPreferences.getBoolean("show_language_switcher", false) } returns true
        every { sharedPreferences.getString("secondary_language", "id") } returns "id"
        every { sharedPreferences.getBoolean("share_usability_metrics", false) } returns true
        every { sharedPreferences.getString("app_theme", VoiceSettings.THEME_CHARCOAL) } returns VoiceSettings.THEME_WARM_AMBER
        every { sharedPreferences.getBoolean("show_safety_cards", true) } returns false

        val settings = repository.getVoiceSettings()

        assertEquals(0.8f, settings.speechRate)
        assertEquals(1.2f, settings.speechPitch)
        assertEquals("en-us-x-sfg#male_1", settings.voiceName)
        assertEquals(true, settings.playAttentionChime)
        assertEquals(true, settings.showLanguageSwitcher)
        assertEquals("id", settings.secondaryLanguage)
        assertEquals(true, settings.shareUsabilityMetrics)
        assertEquals(VoiceSettings.THEME_WARM_AMBER, settings.appTheme)
        assertEquals(false, settings.showSafetyCards)
    }

    @Test
    fun `saveVoiceSettings commits values to editor`() {
        val newSettings = VoiceSettings(
            speechRate = 1.25f,
            speechPitch = 0.9f,
            voiceName = "en-gb-x-rjs#female_1",
            playAttentionChime = true,
            showLanguageSwitcher = true,
            secondaryLanguage = "id",
            shareUsabilityMetrics = true,
            appTheme = VoiceSettings.THEME_SLATE_NAVY,
            showSafetyCards = false
        )

        repository.saveVoiceSettings(newSettings)

        verify {
            editor.putFloat("speech_rate", 1.25f)
            editor.putFloat("speech_pitch", 0.9f)
            editor.putString("voice_name", "en-gb-x-rjs#female_1")
            editor.putBoolean("attention_chime", true)
            editor.putBoolean("show_language_switcher", true)
            editor.putString("secondary_language", "id")
            editor.putBoolean("share_usability_metrics", true)
            editor.putString("app_theme", VoiceSettings.THEME_SLATE_NAVY)
            editor.putBoolean("show_safety_cards", false)
            editor.apply()
        }
    }

    @Test
    fun `getLastPromptedMetricsVersion returns default 0`() {
        every { sharedPreferences.getInt("metrics_prompt_version_code", 0) } returns 0

        val version = repository.getLastPromptedMetricsVersion()
        assertEquals(0, version)
    }

    @Test
    fun `setLastPromptedMetricsVersion saves version code`() {
        repository.setLastPromptedMetricsVersion(19)

        verify {
            editor.putInt("metrics_prompt_version_code", 19)
            editor.apply()
        }
    }

    @Test
    fun `installId generated on opt-in and cleared on opt-out`() {
        every { sharedPreferences.getString("telemetry_install_id", null) } returns null
        every { editor.remove("telemetry_install_id") } returns editor

        val id = repository.getOrGenerateInstallId()
        org.junit.Assert.assertNotNull(id)
        verify {
            editor.putString("telemetry_install_id", any())
            editor.apply()
        }

        repository.clearInstallId()
        verify {
            editor.remove("telemetry_install_id")
            editor.apply()
        }
    }

    @Test
    fun `userRole getter and setter works`() {
        every { sharedPreferences.getString("user_role", VoiceSettings.ROLE_UNSET) } returns VoiceSettings.ROLE_CAREGIVER

        assertEquals(VoiceSettings.ROLE_CAREGIVER, repository.getUserRole())

        repository.setUserRole(VoiceSettings.ROLE_SELF)
        verify {
            editor.putString("user_role", VoiceSettings.ROLE_SELF)
            editor.apply()
        }
    }

    @Test
    fun `firstInstalledAt returns saved or sets current time`() {
        every { sharedPreferences.getLong("first_installed_at", 0L) } returns 0L
        every { editor.putLong(any(), any()) } returns editor

        val time = repository.getFirstInstalledAt()
        org.junit.Assert.assertTrue(time > 0L)
        verify {
            editor.putLong("first_installed_at", any())
            editor.apply()
        }
    }

    @Test
    fun `recordActiveDay adds date and getActiveDayCount returns size`() {
        val dateSet = mutableSetOf("2026-03-01", "2026-03-02")
        every { sharedPreferences.getStringSet("active_days", emptySet()) } returns dateSet
        every { editor.putStringSet(any(), any()) } returns editor

        repository.recordActiveDay("2026-03-03")
        verify {
            editor.putStringSet("active_days", match { it.contains("2026-03-03") })
            editor.apply()
        }

        assertEquals(2, repository.getActiveDayCount())
    }

    @Test
    fun `shouldPromptPmfSurvey requires opt-in, 14 days, 5 active days, unprompted`() {
        every { sharedPreferences.getBoolean("share_usability_metrics", false) } returns true
        every { sharedPreferences.getBoolean("pmf_survey_prompted", false) } returns false
        every { sharedPreferences.getLong("first_installed_at", 0L) } returns (System.currentTimeMillis() - 15L * 86_400_000L)
        every { sharedPreferences.getStringSet("active_days", emptySet()) } returns setOf("d1", "d2", "d3", "d4", "d5")

        assertEquals(true, repository.shouldPromptPmfSurvey())

        // If not opted in to metrics
        every { sharedPreferences.getBoolean("share_usability_metrics", false) } returns false
        assertEquals(false, repository.shouldPromptPmfSurvey())

        // If already prompted
        every { sharedPreferences.getBoolean("share_usability_metrics", false) } returns true
        every { sharedPreferences.getBoolean("pmf_survey_prompted", false) } returns true
        assertEquals(false, repository.shouldPromptPmfSurvey())
    }
}

