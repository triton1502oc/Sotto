package com.amh.sotto.ui.main

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amh.sotto.R
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.util.LocaleHelper
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsDialog(
    currentSettings: VoiceSettings,
    currentLanguage: String,
    availableLanguages: List<Pair<String, String>> = remember { LocaleHelper.getAvailableLanguages() },
    onLanguageChanged: (String) -> Unit,
    onSettingsChanged: (VoiceSettings) -> Unit,
    onTestVoice: () -> Unit,
    onDismiss: () -> Unit
) {
    var rate by remember { mutableFloatStateOf(currentSettings.speechRate) }
    var pitch by remember { mutableFloatStateOf(currentSettings.speechPitch) }
    var langDropdownExpanded by remember { mutableStateOf(false) }
    var langSearchQuery by remember { mutableStateOf("") }

    val systemDefaultLabel = stringResource(R.string.system_default)

    fun updateSettings(newRate: Float = rate, newPitch: Float = pitch) {
        rate = newRate
        pitch = newPitch
        onSettingsChanged(
            VoiceSettings(
                speechRate = ((newRate * 10).roundToInt() / 10f),
                speechPitch = ((newPitch * 10).roundToInt() / 10f),
                voiceName = null
            )
        )
    }

    val selectedLanguageLabel = remember(currentLanguage, availableLanguages, systemDefaultLabel) {
        if (currentLanguage == LocaleHelper.LANG_SYSTEM) {
            systemDefaultLabel
        } else {
            availableLanguages.firstOrNull { it.first == currentLanguage }?.second
                ?: LocaleHelper.getLanguageDisplayName(currentLanguage)
        }
    }

    val filteredLanguages = remember(availableLanguages, langSearchQuery) {
        if (langSearchQuery.isBlank()) {
            availableLanguages
        } else {
            availableLanguages.filter { (code, label) ->
                code == LocaleHelper.LANG_SYSTEM || label.contains(langSearchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_voice_settings_title),
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
                // App Language Dropdown (Set Once)
                Column {
                    Text(
                        text = stringResource(R.string.label_language),
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
                                .clickable { langDropdownExpanded = true },
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
                                    text = selectedLanguageLabel,
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
                            expanded = langDropdownExpanded,
                            onDismissRequest = {
                                langDropdownExpanded = false
                                langSearchQuery = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .heightIn(max = 360.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            OutlinedTextField(
                                value = langSearchQuery,
                                onValueChange = { langSearchQuery = it },
                                placeholder = { Text(stringResource(R.string.search_language), fontSize = 14.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                singleLine = true
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            filteredLanguages.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        onLanguageChanged(code)
                                        langDropdownExpanded = false
                                        langSearchQuery = ""
                                    }
                                )
                            }
                        }
                    }
                }

                // Speech Rate (Speed)
                Column {
                    val rateText = when {
                        rate <= 0.7f -> stringResource(R.string.speed_slower)
                        rate in 0.9f..1.1f -> stringResource(R.string.speed_normal)
                        rate >= 1.3f -> stringResource(R.string.speed_faster)
                        else -> ""
                    }
                    val speedDisplay = String.format(Locale.US, "%.1fx", rate)
                    Text(
                        text = "${stringResource(R.string.label_speed)}: $speedDisplay ${if (rateText.isNotEmpty()) "($rateText)" else ""}",
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
                        pitch <= 0.8f -> stringResource(R.string.pitch_lower)
                        pitch in 0.95f..1.05f -> stringResource(R.string.pitch_normal)
                        pitch >= 1.2f -> stringResource(R.string.pitch_higher)
                        else -> ""
                    }
                    val pitchDisplay = String.format(Locale.US, "%.1fx", pitch)
                    Text(
                        text = "${stringResource(R.string.label_pitch)}: $pitchDisplay ${if (pitchText.isNotEmpty()) "($pitchText)" else ""}",
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

                // Test Voice Button
                OutlinedButton(
                    onClick = onTestVoice,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.action_test_voice))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_done),
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
