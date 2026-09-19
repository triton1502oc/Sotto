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
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("sotto_voice_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor

        repository = SharedPreferencesVoiceSettingsRepository(context)
    }

    @Test
    fun `getVoiceSettings returns defaults when preferences empty`() {
        every { sharedPreferences.getFloat("speech_rate", 1.0f) } returns 1.0f
        every { sharedPreferences.getFloat("speech_pitch", 1.0f) } returns 1.0f
        every { sharedPreferences.getString("voice_name", null) } returns null
        every { sharedPreferences.getBoolean("attention_chime", false) } returns false

        val settings = repository.getVoiceSettings()

        assertEquals(1.0f, settings.speechRate)
        assertEquals(1.0f, settings.speechPitch)
        assertEquals(null, settings.voiceName)
        assertEquals(false, settings.playAttentionChime)
    }

    @Test
    fun `getVoiceSettings returns saved values`() {
        every { sharedPreferences.getFloat("speech_rate", 1.0f) } returns 0.8f
        every { sharedPreferences.getFloat("speech_pitch", 1.0f) } returns 1.2f
        every { sharedPreferences.getString("voice_name", null) } returns "en-us-x-sfg#male_1"
        every { sharedPreferences.getBoolean("attention_chime", false) } returns true

        val settings = repository.getVoiceSettings()

        assertEquals(0.8f, settings.speechRate)
        assertEquals(1.2f, settings.speechPitch)
        assertEquals("en-us-x-sfg#male_1", settings.voiceName)
        assertEquals(true, settings.playAttentionChime)
    }

    @Test
    fun `saveVoiceSettings commits values to editor`() {
        val newSettings = VoiceSettings(
            speechRate = 1.25f,
            speechPitch = 0.9f,
            voiceName = "en-gb-x-rjs#female_1",
            playAttentionChime = true
        )

        repository.saveVoiceSettings(newSettings)

        verify {
            editor.putFloat("speech_rate", 1.25f)
            editor.putFloat("speech_pitch", 0.9f)
            editor.putString("voice_name", "en-gb-x-rjs#female_1")
            editor.putBoolean("attention_chime", true)
            editor.apply()
        }
    }
}
