package com.amh.sotto.ui.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amh.sotto.BuildConfig
import com.amh.sotto.R
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.util.ChimePlayer
import com.amh.sotto.util.LocaleHelper
import com.amh.sotto.util.TranslationHelper
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
    onTestVoice: (VoiceSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var rate by remember { mutableFloatStateOf(currentSettings.speechRate) }
    var pitch by remember { mutableFloatStateOf(currentSettings.speechPitch) }
    var attentionChime by remember { mutableStateOf(currentSettings.playAttentionChime) }
    var showLangSwitcher by remember { mutableStateOf(currentSettings.showLanguageSwitcher) }
    var secondaryLang by remember { mutableStateOf(currentSettings.secondaryLanguage) }
    var secondaryLangDropdownExpanded by remember { mutableStateOf(false) }
    var langDropdownExpanded by remember { mutableStateOf(false) }
    var langSearchQuery by remember { mutableStateOf("") }

    val systemDefaultLabel = stringResource(R.string.system_default)

    fun updateSettings(
        newRate: Float = rate,
        newPitch: Float = pitch,
        newChime: Boolean = attentionChime,
        newShowLangSwitcher: Boolean = showLangSwitcher,
        newSecondaryLang: String = secondaryLang
    ) {
        rate = newRate
        pitch = newPitch
        attentionChime = newChime
        showLangSwitcher = newShowLangSwitcher
        secondaryLang = newSecondaryLang
        onSettingsChanged(
            VoiceSettings(
                speechRate = ((newRate * 10).roundToInt() / 10f),
                speechPitch = ((newPitch * 10).roundToInt() / 10f),
                voiceName = null,
                playAttentionChime = newChime,
                showLanguageSwitcher = newShowLangSwitcher,
                secondaryLanguage = newSecondaryLang
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

                // Attention Chime Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val nextState = !attentionChime
                            updateSettings(newChime = nextState)
                            if (nextState) {
                                ChimePlayer.play(context)
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_attention_chime),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.label_attention_chime_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = attentionChime,
                        onCheckedChange = {
                            updateSettings(newChime = it)
                            if (it) {
                                ChimePlayer.play(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                // Dual-Language Speech Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_dual_language_section),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val nextState = !showLangSwitcher
                                updateSettings(newShowLangSwitcher = nextState)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.label_show_lang_switcher),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.label_show_lang_switcher_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = showLangSwitcher,
                            onCheckedChange = {
                                updateSettings(newShowLangSwitcher = it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }

                    if (showLangSwitcher) {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                text = stringResource(R.string.label_secondary_language),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val secondaryLanguagesList = remember(availableLanguages) {
                                availableLanguages.filter { it.first != LocaleHelper.LANG_SYSTEM }
                            }
                            val secondaryLanguageLabel = remember(secondaryLang, secondaryLanguagesList) {
                                secondaryLanguagesList.firstOrNull { it.first == secondaryLang }?.second
                                    ?: LocaleHelper.getLanguageDisplayName(secondaryLang)
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { secondaryLangDropdownExpanded = true },
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = secondaryLanguageLabel,
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
                                    expanded = secondaryLangDropdownExpanded,
                                    onDismissRequest = { secondaryLangDropdownExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .heightIn(max = 280.dp)
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    secondaryLanguagesList.forEach { (code, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                updateSettings(newSecondaryLang = code)
                                                secondaryLangDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Language Model Download Status & Action
                            var isModelDownloaded by remember(secondaryLang) { mutableStateOf<Boolean?>(null) }
                            var isDownloadingModel by remember { mutableStateOf(false) }
                            var downloadError by remember { mutableStateOf<String?>(null) }

                            LaunchedEffect(secondaryLang) {
                                TranslationHelper.isModelDownloaded(secondaryLang) { downloaded ->
                                    isModelDownloaded = downloaded
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.label_language_model, LocaleHelper.getLanguageDisplayName(secondaryLang)),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isModelDownloaded == true) {
                                                stringResource(R.string.label_model_downloaded)
                                            } else if (isDownloadingModel) {
                                                stringResource(R.string.status_downloading_model)
                                            } else {
                                                stringResource(R.string.label_model_not_downloaded)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isModelDownloaded == true) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        if (downloadError != null) {
                                            Text(
                                                text = downloadError!!,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFEF5350)
                                            )
                                        }
                                    }
                                    if (isModelDownloaded == false) {
                                        OutlinedButton(
                                            onClick = {
                                                if (!isDownloadingModel) {
                                                    downloadError = null
                                                    isDownloadingModel = true
                                                    TranslationHelper.downloadModel(
                                                        langCode = secondaryLang,
                                                        onProgress = { isDownloadingModel = it },
                                                        onSuccess = {
                                                            isDownloadingModel = false
                                                            isModelDownloaded = true
                                                        },
                                                        onError = {
                                                            isDownloadingModel = false
                                                            downloadError = context.getString(R.string.error_model_download_network)
                                                        }
                                                    )
                                                }
                                            },
                                            enabled = !isDownloadingModel,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            if (isDownloadingModel) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            } else {
                                                Text(
                                                    text = stringResource(R.string.action_download_model),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Test Voice Button
                OutlinedButton(
                    onClick = {
                        onTestVoice(
                            VoiceSettings(
                                speechRate = ((rate * 10).roundToInt() / 10f),
                                speechPitch = ((pitch * 10).roundToInt() / 10f),
                                voiceName = null,
                                playAttentionChime = attentionChime,
                                showLanguageSwitcher = showLangSwitcher,
                                secondaryLanguage = secondaryLang
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.action_test_voice))
                }

                // About footer
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "💬  ${stringResource(R.string.action_feedback)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/triton1502oc/Sotto/issues"))
                                    context.startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    // Ignore if no browser or activity can handle the URL
                                } catch (e: Exception) {
                                    // Protect against any unexpected security or runtime exception
                                }
                            }
                            .padding(vertical = 4.dp)
                    )
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
