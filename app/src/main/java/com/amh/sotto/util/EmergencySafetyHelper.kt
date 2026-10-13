package com.amh.sotto.util

import android.content.Context
import com.amh.sotto.R
import java.util.Locale

object EmergencySafetyHelper {

    /**
     * Checks whether an emergency safety phrase still has unresolved placeholder brackets
     * such as [Area/Neighborhood] or [phone number].
     */
    fun hasUnresolvedPlaceholders(text: String): Boolean {
        return text.contains('[') && text.contains(']')
    }

    /**
     * Cleans bracketed placeholders so Android TTS never reads literal brackets aloud.
     */
    fun cleanPlaceholdersForSpeech(text: String): String {
        if (!hasUnresolvedPlaceholders(text)) return text
        return text.replace(Regex("\\[.*?\\]"), "")
            .replace(":", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Formats configured safety details into a display text and spoken text with natural digit spacing.
     */
    fun formatSafetyPhrase(
        context: Context,
        area: String,
        contactName: String,
        phone: String,
        locale: Locale = Locale.getDefault()
    ): Pair<String, String> {
        val namePart = if (contactName.isNotBlank()) " ($contactName)" else ""
        val displayText = context.getString(
            R.string.emergency_contact_phrase_format,
            area.trim(),
            namePart,
            phone.trim()
        )
        val spokenText = LocaleHelper.formatDigitsForSpeech(displayText, locale)
        return Pair(displayText, spokenText)
    }
}
