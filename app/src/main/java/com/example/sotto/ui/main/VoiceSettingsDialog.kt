package com.example.sotto.ui.main

import android.speech.tts.Voice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sotto.data.VoiceSettings
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsDialog(
    currentSettings: VoiceSettings,
    availableVoices: List<Voice>,
    onSettingsChanged: (VoiceSettings) -> Unit,
    onTestVoice: () -> Unit,
    onDismiss: () -> Unit
) {
    var rate by remember { mutableFloatStateOf(currentSettings.speechRate) }
    var pitch by remember { mutableFloatStateOf(currentSettings.speechPitch) }
    var selectedVoiceName by remember { mutableStateOf(currentSettings.voiceName) }
    var voiceDropdownExpanded by remember { mutableStateOf(false) }

    fun updateSettings(newRate: Float = rate, newPitch: Float = pitch, newVoice: String? = selectedVoiceName) {
        rate = newRate
        pitch = newPitch
        selectedVoiceName = newVoice
        onSettingsChanged(
            VoiceSettings(
                speechRate = ((newRate * 10).roundToInt() / 10f),
                speechPitch = ((newPitch * 10).roundToInt() / 10f),
                voiceName = newVoice
            )
        )
    }

    val selectedVoiceLabel = remember(selectedVoiceName, availableVoices) {
        if (selectedVoiceName == null) {
            "System Default"
        } else {
            val v = availableVoices.firstOrNull { it.name == selectedVoiceName }
            if (v != null) formatVoiceLabel(v) else "System Default"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Voice Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Speech Rate (Speed)
                Column {
                    val rateText = when {
                        rate <= 0.7f -> "Slower"
                        rate in 0.9f..1.1f -> "Normal"
                        rate >= 1.3f -> "Faster"
                        else -> ""
                    }
                    val speedDisplay = String.format(Locale.US, "%.1fx", rate)
                    Text(
                        text = "Speed: $speedDisplay ${if (rateText.isNotEmpty()) "($rateText)" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = rate,
                        onValueChange = { updateSettings(newRate = it) },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Speech Pitch
                Column {
                    val pitchText = when {
                        pitch <= 0.8f -> "Lower"
                        pitch in 0.95f..1.05f -> "Normal"
                        pitch >= 1.2f -> "Higher"
                        else -> ""
                    }
                    val pitchDisplay = String.format(Locale.US, "%.1fx", pitch)
                    Text(
                        text = "Pitch: $pitchDisplay ${if (pitchText.isNotEmpty()) "($pitchText)" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = pitch,
                        onValueChange = { updateSettings(newPitch = it) },
                        valueRange = 0.7f..1.3f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Speaker / Voice Selection
                Column {
                    Text(
                        text = "Speaker Voice",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { voiceDropdownExpanded = true },
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedVoiceLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "▼",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = voiceDropdownExpanded,
                            onDismissRequest = { voiceDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .heightIn(max = 280.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("System Default", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    updateSettings(newVoice = null)
                                    voiceDropdownExpanded = false
                                }
                            )
                            availableVoices.forEach { voice ->
                                DropdownMenuItem(
                                    text = { Text(formatVoiceLabel(voice), color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        updateSettings(newVoice = voice.name)
                                        voiceDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Test Voice Button
                OutlinedButton(
                    onClick = onTestVoice,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Test Voice")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

private fun formatVoiceLabel(voice: Voice): String {
    val locale = voice.locale
    val country = locale.displayCountry.ifBlank { locale.country }
    val nameSimplified = voice.name
        .substringAfterLast("/")
        .substringAfterLast(":")
        .removePrefix("en-us-x-")
        .removePrefix("en-gb-x-")
        .removeSuffix("-local")
    return if (country.isNotEmpty()) {
        "$country - $nameSimplified"
    } else {
        voice.name
    }
}
