package com.amh.sotto.data

import android.content.Context
import android.content.SharedPreferences

enum class VoiceGender {
    DEFAULT,
    FEMALE,
    MALE;

    companion object {
        fun fromString(value: String?): VoiceGender {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
        }
    }
}

data class VoiceSettings(
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val voiceName: String? = null,
    val voiceGender: VoiceGender = VoiceGender.DEFAULT
)

interface VoiceSettingsRepository {
    fun getVoiceSettings(): VoiceSettings
    fun saveVoiceSettings(settings: VoiceSettings)
}

class SharedPreferencesVoiceSettingsRepository(context: Context) : VoiceSettingsRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("sotto_voice_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_VOICE_NAME = "voice_name"
        private const val KEY_VOICE_GENDER = "voice_gender"
    }

    override fun getVoiceSettings(): VoiceSettings {
        val rate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        val pitch = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        val voiceName = prefs.getString(KEY_VOICE_NAME, null)
        val voiceGender = VoiceGender.fromString(prefs.getString(KEY_VOICE_GENDER, null))
        return VoiceSettings(
            speechRate = rate,
            speechPitch = pitch,
            voiceName = voiceName,
            voiceGender = voiceGender
        )
    }

    override fun saveVoiceSettings(settings: VoiceSettings) {
        val editor = prefs.edit()
        editor.putFloat(KEY_SPEECH_RATE, settings.speechRate)
        editor.putFloat(KEY_SPEECH_PITCH, settings.speechPitch)
        editor.putString(KEY_VOICE_NAME, settings.voiceName)
        editor.putString(KEY_VOICE_GENDER, settings.voiceGender.name)
        editor.apply()
    }
}
