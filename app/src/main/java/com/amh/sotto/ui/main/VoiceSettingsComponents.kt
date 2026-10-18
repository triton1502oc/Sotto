package com.amh.sotto.ui.main

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.Voice
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amh.sotto.BuildConfig
import com.amh.sotto.R
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.theme.SottoColors
import com.amh.sotto.util.ChimePlayer
import com.amh.sotto.util.LocaleHelper
import com.amh.sotto.util.TranslationHelper
import com.amh.sotto.util.UsabilityTracker
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsVoiceTab(
    settings: VoiceSettings,
    onUpdateSettings: (VoiceSettings) -> Unit,
    currentLanguage: String,
    availableLanguages: List<Pair<String, String>>,
    availableVoices: List<Voice>,
    isTtsLanguageInstalled: (String) -> Boolean,
    onInstallTtsVoice: () -> Unit,
    onLanguageChanged: (String) -> Unit,
    onTestVoice: (VoiceSettings) -> Unit
) {
    val context = LocalContext.current
    var secondaryLangDropdownExpanded by remember { mutableStateOf(false) }
    var langDropdownExpanded by remember { mutableStateOf(false) }
    var voiceDropdownExpanded by remember { mutableStateOf(false) }
    var langSearchQuery by remember { mutableStateOf("") }
    val systemDefaultLabel = stringResource(R.string.system_default)

    val selectedLanguageLabel = remember(currentLanguage, availableLanguages, systemDefaultLabel) {
        if (currentLanguage == LocaleHelper.LANG_SYSTEM) {
            systemDefaultLabel
        } else {
            availableLanguages.firstOrNull { it.first == currentLanguage }?.second
                ?: LocaleHelper.getLanguageDisplayName(currentLanguage)
        }
    }

    val filteredLanguages = remember(langSearchQuery, availableLanguages) {
        if (langSearchQuery.isBlank()) {
            availableLanguages
        } else {
            availableLanguages.filter { (code, label) ->
                code == LocaleHelper.LANG_SYSTEM || label.contains(langSearchQuery, ignoreCase = true)
            }
        }
    }

    val defaultVoiceLabel = stringResource(R.string.voice_system_default)

    val effectiveLocale = remember(currentLanguage) {
        LocaleHelper.getLocaleForLanguage(currentLanguage)
    }

    val matchingVoices = remember(availableVoices, effectiveLocale) {
        availableVoices.filter { voice ->
            !voice.isNetworkConnectionRequired &&
            (voice.locale.language.equals(effectiveLocale.language, ignoreCase = true) ||
             (effectiveLocale.language == "id" && voice.locale.language.equals("in", ignoreCase = true)) ||
             (effectiveLocale.language == "in" && voice.locale.language.equals("id", ignoreCase = true)))
        }.sortedWith(
            compareByDescending<Voice> { it.quality }
                .thenBy { it.locale.displayCountry }
                .thenBy { it.name }
        )
    }

    fun formatVoiceName(voice: Voice): String {
        val naturalTag = when (voice.quality) {
            Voice.QUALITY_VERY_HIGH, Voice.QUALITY_HIGH -> " (Natural)"
            else -> ""
        }
        val country = if (voice.locale.displayCountry.isNotBlank()) " - " + voice.locale.displayCountry else ""
        val cleanName = voice.name
            .replace(Regex("^[a-z]{2,3}[-_][A-Za-z]{2,4}[-_]x-"), "")
            .replace(Regex("[-_]local$"), "")
            .replace(Regex("[-_]network$"), "")
            .replace("#", " ")
            .replace("-", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        return cleanName + country + naturalTag
    }

    val selectedVoiceLabel = remember(settings.voiceName, matchingVoices, defaultVoiceLabel) {
        if (settings.voiceName == null) {
            defaultVoiceLabel
        } else {
            matchingVoices.firstOrNull { it.name == settings.voiceName }?.let { formatVoiceName(it) }
                ?: settings.voiceName
        }
    }

    fun updateSettings(newSettings: VoiceSettings) { onUpdateSettings(newSettings) }

                                // Tab 0: Voice & Speech
                                SettingsCard {
                                    SettingsSectionHeader(
                                        iconRes = R.drawable.ic_record_voice_over,
                                        title = stringResource(R.string.label_speech_section)
                                    )

                                    // App Language Dropdown
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = stringResource(R.string.label_language),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { langDropdownExpanded = true },
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = selectedLanguageLabel,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_arrow_down),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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

                                        if (currentLanguage != LocaleHelper.LANG_SYSTEM && !isTtsLanguageInstalled(currentLanguage)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.warn_voice_pack_missing),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SottoColors.WarningOrange,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                TextButton(
                                                    onClick = onInstallTtsVoice,
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.action_install_voice),
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Voice Selection Dropdown (if matching voices available)
                                    if (matchingVoices.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = stringResource(R.string.label_voice_model),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                    .clickable { voiceDropdownExpanded = true },
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = selectedVoiceLabel,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            painter = painterResource(R.drawable.ic_arrow_down),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                DropdownMenu(
                                                    expanded = voiceDropdownExpanded,
                                                    onDismissRequest = { voiceDropdownExpanded = false },
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .heightIn(max = 300.dp)
                                                        .background(MaterialTheme.colorScheme.surface)
                                                ) {
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                defaultVoiceLabel,
                                                                fontWeight = if (settings.voiceName == null) FontWeight.Bold else FontWeight.Normal,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        },
                                                        onClick = {
                                                            updateSettings(settings.copy(voiceName = null))
                                                            voiceDropdownExpanded = false
                                                        }
                                                    )
                                                    matchingVoices.forEach { voice ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    formatVoiceName(voice),
                                                                    fontWeight = if (settings.voiceName == voice.name) FontWeight.Bold else FontWeight.Normal,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            },
                                                            onClick = {
                                                                updateSettings(settings.copy(voiceName = voice.name))
                                                                voiceDropdownExpanded = false
                                                            }
                                                        )
                                                    }
                                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                text = stringResource(R.string.action_add_more_voices),
                                                                color = MaterialTheme.colorScheme.primary,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        },
                                                        leadingIcon = {
                                                            Icon(
                                                                painter = painterResource(R.drawable.ic_add),
                                                                contentDescription = null,
                                                                modifier = Modifier.size(16.dp),
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        },
                                                        onClick = {
                                                            voiceDropdownExpanded = false
                                                            try {
                                                                val intent = Intent("com.android.settings.TTS_SETTINGS")
                                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                                context.startActivity(intent)
                                                            } catch (_: Exception) {
                                                                try {
                                                                    val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                                    context.startActivity(intent)
                                                                } catch (_: Exception) {}
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Speech Rate (Speed)
                                    Column {
                                        val rateText = when {
                                            settings.speechRate <= 0.7f -> stringResource(R.string.speed_slower)
                                            settings.speechRate in 0.9f..1.1f -> stringResource(R.string.speed_normal)
                                            settings.speechRate >= 1.3f -> stringResource(R.string.speed_faster)
                                            else -> ""
                                        }
                                        val speedDisplay = String.format(Locale.US, "%.1fx", settings.speechRate)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.label_speed),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$speedDisplay ${if (rateText.isNotEmpty()) "($rateText)" else ""}".trim(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Slider(
                                            value = settings.speechRate,
                                            onValueChange = { updateSettings(settings.copy(speechRate = it)) },
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
                                            settings.speechPitch <= 0.8f -> stringResource(R.string.pitch_lower)
                                            settings.speechPitch in 0.95f..1.05f -> stringResource(R.string.pitch_normal)
                                            settings.speechPitch >= 1.2f -> stringResource(R.string.pitch_higher)
                                            else -> ""
                                        }
                                        val pitchDisplay = String.format(Locale.US, "%.1fx", settings.speechPitch)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.label_pitch),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$pitchDisplay ${if (pitchText.isNotEmpty()) "($pitchText)" else ""}".trim(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Slider(
                                            value = settings.speechPitch,
                                            onValueChange = { updateSettings(settings.copy(speechPitch = it)) },
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
                                                val nextState = !settings.playAttentionChime
                                                updateSettings(settings.copy(playAttentionChime = nextState))
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
                                                .padding(end = 12.dp)
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
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = settings.playAttentionChime,
                                            onCheckedChange = {
                                                updateSettings(settings.copy(playAttentionChime = it))
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

                                    // Immediate Test Voice Action
                                    OutlinedButton(
                                        onClick = { onTestVoice(settings) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_volume_up),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(stringResource(R.string.action_test_voice))
                                        }
                                    }
                                }

                                // Dual-Language Section
                                SettingsCard {
                                    SettingsSectionHeader(
                                        iconRes = R.drawable.ic_language,
                                        title = stringResource(R.string.label_dual_language_section)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val nextState = !settings.showLanguageSwitcher
                                                updateSettings(settings.copy(showLanguageSwitcher = nextState))
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 12.dp)
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
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = settings.showLanguageSwitcher,
                                            onCheckedChange = {
                                                updateSettings(settings.copy(showLanguageSwitcher = it))
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            )
                                        )
                                    }

                                    if (settings.showLanguageSwitcher) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = stringResource(R.string.label_secondary_language),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            val secondaryLanguagesList = remember(availableLanguages) {
                                                availableLanguages.filter { it.first != LocaleHelper.LANG_SYSTEM }
                                            }
                                            val secondaryLanguageLabel = remember(settings.secondaryLanguage, secondaryLanguagesList) {
                                                secondaryLanguagesList.firstOrNull { it.first == settings.secondaryLanguage }?.second
                                                    ?: LocaleHelper.getLanguageDisplayName(settings.secondaryLanguage)
                                            }

                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable { secondaryLangDropdownExpanded = true },
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = secondaryLanguageLabel,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Icon(
                                                            painter = painterResource(R.drawable.ic_arrow_down),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                                updateSettings(settings.copy(secondaryLanguage = code))
                                                                secondaryLangDropdownExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            // Translation model status & download
                                            var isModelDownloaded by remember(settings.secondaryLanguage) { mutableStateOf<Boolean?>(null) }
                                            var isDownloadingModel by remember { mutableStateOf(false) }
                                            var downloadErrorResId by remember { mutableStateOf<Int?>(null) }

                                            LaunchedEffect(settings.secondaryLanguage) {
                                                TranslationHelper.isModelDownloaded(settings.secondaryLanguage) { downloaded ->
                                                    isModelDownloaded = downloaded
                                                }
                                            }

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = stringResource(R.string.label_language_model, LocaleHelper.getLanguageDisplayName(settings.secondaryLanguage)),
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
                                                            color = if (isModelDownloaded == true) SottoColors.DownloadedGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        if (downloadErrorResId != null) {
                                                            Text(
                                                                text = stringResource(downloadErrorResId!!),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = SottoColors.DestructiveRed
                                                            )
                                                        }
                                                    }
                                                    if (isModelDownloaded == false) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                if (!isDownloadingModel) {
                                                                    downloadErrorResId = null
                                                                    isDownloadingModel = true
                                                                    TranslationHelper.downloadModel(
                                                                        langCode = settings.secondaryLanguage,
                                                                        onProgress = { isDownloadingModel = it },
                                                                        onSuccess = {
                                                                            isDownloadingModel = false
                                                                            isModelDownloaded = true
                                                                        },
                                                                        onError = {
                                                                            isDownloadingModel = false
                                                                            downloadErrorResId = R.string.error_model_download_network
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

                                            // TTS voice pack status & download
                                            val isVoicePackInstalled = isTtsLanguageInstalled(settings.secondaryLanguage)
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = stringResource(R.string.label_tts_voice_pack, LocaleHelper.getLanguageDisplayName(settings.secondaryLanguage)),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = if (isVoicePackInstalled) {
                                                                stringResource(R.string.label_voice_pack_installed)
                                                            } else {
                                                                stringResource(R.string.label_voice_pack_missing)
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isVoicePackInstalled) SottoColors.DownloadedGreen else SottoColors.WarningOrange
                                                        )
                                                    }
                                                    if (!isVoicePackInstalled) {
                                                        OutlinedButton(
                                                            onClick = onInstallTtsVoice,
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = stringResource(R.string.action_install_voice),
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

@Composable
fun VoiceSettingsAppearanceTab(
    settings: VoiceSettings,
    onUpdateSettings: (VoiceSettings) -> Unit,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    fun updateSettings(newSettings: VoiceSettings) { onUpdateSettings(newSettings) }

                                // Tab 1: Appearance & Safety
                                SettingsCard {
                                    SettingsSectionHeader(
                                        iconRes = R.drawable.ic_palette,
                                        title = stringResource(R.string.label_appearance_section)
                                    )

                                    val themes = listOf(
                                        VoiceSettings.THEME_CHARCOAL to Pair(R.string.theme_charcoal, R.string.theme_charcoal_desc),
                                        VoiceSettings.THEME_OLED_BLACK to Pair(R.string.theme_oled_black, R.string.theme_oled_black_desc),
                                        VoiceSettings.THEME_WARM_AMBER to Pair(R.string.theme_warm_amber, R.string.theme_warm_amber_desc),
                                        VoiceSettings.THEME_SLATE_NAVY to Pair(R.string.theme_slate_navy, R.string.theme_slate_navy_desc),
                                        VoiceSettings.THEME_SOFT_PARCHMENT to Pair(R.string.theme_soft_parchment, R.string.theme_soft_parchment_desc)
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        themes.forEach { (themeKey, strings) ->
                                            val (nameRes, descRes) = strings
                                            val isSelected = settings.appTheme == themeKey

                                            val (previewBg, previewSurface, previewBorder) = when (themeKey) {
                                                VoiceSettings.THEME_OLED_BLACK -> Triple(Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFF333333))
                                                VoiceSettings.THEME_WARM_AMBER -> Triple(Color(0xFF191512), Color(0xFFD4A373), Color(0xFF483C34))
                                                VoiceSettings.THEME_SLATE_NAVY -> Triple(Color(0xFF0F141C), Color(0xFF8FA8C8), Color(0xFF2B3A50))
                                                VoiceSettings.THEME_SOFT_PARCHMENT -> Triple(Color(0xFFF5F2EB), Color(0xFF1A1A1A), Color(0xFFCCC5B8))
                                                else -> Triple(Color(0xFF121212), Color(0xFFE0E0E0), Color(0xFF424242))
                                            }

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        updateSettings(settings.copy(appTheme = themeKey))
                                                        UsabilityTracker.recordThemeSelected(context, themeKey)
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(previewBg)
                                                            .border(1.dp, previewBorder, RoundedCornerShape(8.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(RoundedCornerShape(3.dp))
                                                                .background(previewSurface)
                                                        )
                                                    }

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = stringResource(nameRes),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = stringResource(descRes),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    if (isSelected) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.ic_check_bold),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Card Proportions Setting (#57)
                                SettingsCard {
                                    SettingsSectionHeader(
                                        iconRes = R.drawable.ic_cards,
                                        title = stringResource(R.string.label_card_layout),
                                        subtitle = stringResource(R.string.label_card_layout_desc)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val options = listOf(
                                            true to stringResource(R.string.card_layout_compact),
                                            false to stringResource(R.string.card_layout_square)
                                        )
                                        options.forEach { (isCompact, label) ->
                                            val isSelected = settings.compactCardLayout == isCompact
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        if (settings.compactCardLayout != isCompact) {
                                                            UsabilityTracker.startIntent("card_layout_changed")
                                                            UsabilityTracker.endIntent(
                                                                context,
                                                                "card_layout_changed",
                                                                outcome = if (isCompact) "compact" else "square"
                                                            )
                                                            updateSettings(settings.copy(compactCardLayout = isCompact))
                                                        }
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (isSelected) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.ic_check_bold),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Picture-in-Picture Setting (#45, #64)
                                SettingsCard {
                                    SettingsSectionHeader(
                                        iconRes = R.drawable.ic_pip,
                                        title = stringResource(R.string.label_pip_setting),
                                        subtitle = stringResource(R.string.label_pip_setting_desc)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val nextState = !settings.enablePip
                                                updateSettings(settings.copy(enablePip = nextState))
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 12.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.label_pip_setting),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.label_pip_setting_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = settings.enablePip,
                                            onCheckedChange = {
                                                updateSettings(settings.copy(enablePip = it))
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            )
                                        )
                                    }

                                    if (settings.enablePip) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val nextState = !settings.showPipInTopBar
                                                    UsabilityTracker.startIntent("setting_pip_top_bar")
                                                    UsabilityTracker.endIntent(context, "setting_pip_top_bar", outcome = if (nextState) "enabled" else "disabled")
                                                    updateSettings(settings.copy(showPipInTopBar = nextState))
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 12.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.label_show_pip_in_top_bar),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = stringResource(R.string.label_show_pip_in_top_bar_desc),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Switch(
                                                checked = settings.showPipInTopBar,
                                                onCheckedChange = {
                                                    UsabilityTracker.startIntent("setting_pip_top_bar")
                                                    UsabilityTracker.endIntent(context, "setting_pip_top_bar", outcome = if (it) "enabled" else "disabled")
                                                    updateSettings(settings.copy(showPipInTopBar = it))
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        OutlinedButton(
                                            onClick = {
                                                UsabilityTracker.startIntent("pip_manual_launch")
                                                UsabilityTracker.endIntent(context, "pip_manual_launch", outcome = "settings")
                                                onEnterPip()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_pip),
                                                    contentDescription = stringResource(R.string.cd_enter_pip_now),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = stringResource(R.string.action_enter_pip_now),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                }

                                // Safety Cards Setting
                                SettingsCard {
                                    SettingsSectionHeader(
                                        iconRes = R.drawable.ic_shield,
                                        title = stringResource(R.string.label_safety_section)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val nextState = !settings.showSafetyCards
                                                updateSettings(settings.copy(showSafetyCards = nextState))
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 12.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.label_show_safety_cards),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.label_show_safety_cards_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = settings.showSafetyCards,
                                            onCheckedChange = {
                                                updateSettings(settings.copy(showSafetyCards = it))
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }

}

@Composable
fun VoiceSettingsDataTab(
    settings: VoiceSettings,
    onUpdateSettings: (VoiceSettings) -> Unit,
    onExportPhrases: () -> Unit,
    onImportPhrases: () -> Unit,
    onOpenQRScanner: () -> Unit,
    onOpenWhyLog: () -> Unit,
    onOpenWhyTreeEditor: () -> Unit
) {
    val context = LocalContext.current
    fun updateSettings(newSettings: VoiceSettings) { onUpdateSettings(newSettings) }

                                // Tab 2: Data & Backup (#58)
                                SettingsCard {
                                    SettingsSectionHeader(
                                        iconRes = R.drawable.ic_explore,
                                        title = stringResource(R.string.why_finder_title)
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SettingsToolTile(
                                            iconRes = R.drawable.ic_list_alt,
                                            title = stringResource(R.string.action_view_why_log),
                                            onClick = {
                                                UsabilityTracker.startIntent("settings_tool_open")
                                                UsabilityTracker.endIntent(context, "settings_tool_open", outcome = "why_log")
                                                onOpenWhyLog()
                                            }
                                        )

                                        SettingsToolTile(
                                            iconRes = R.drawable.ic_account_tree,
                                            title = stringResource(R.string.action_edit_why_tree),
                                            onClick = {
                                                UsabilityTracker.startIntent("settings_tool_open")
                                                UsabilityTracker.endIntent(context, "settings_tool_open", outcome = "tree_editor")
                                                onOpenWhyTreeEditor()
                                            }
                                        )
                                    }
                                }

                                SettingsCard {
                                    SettingsSectionHeader(
                                        iconRes = R.drawable.ic_backup,
                                        title = stringResource(R.string.label_backup_section),
                                        subtitle = stringResource(R.string.label_backup_desc)
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SettingsToolTile(
                                            iconRes = R.drawable.ic_file_upload,
                                            title = stringResource(R.string.action_export_phrases),
                                            onClick = {
                                                UsabilityTracker.startIntent("settings_tool_open")
                                                UsabilityTracker.endIntent(context, "settings_tool_open", outcome = "export")
                                                onExportPhrases()
                                            }
                                        )

                                        SettingsToolTile(
                                            iconRes = R.drawable.ic_file_download,
                                            title = stringResource(R.string.action_import_phrases),
                                            onClick = {
                                                UsabilityTracker.startIntent("settings_tool_open")
                                                UsabilityTracker.endIntent(context, "settings_tool_open", outcome = "import")
                                                onImportPhrases()
                                            }
                                        )

                                        SettingsToolTile(
                                            iconRes = R.drawable.ic_qr_code_scanner,
                                            title = stringResource(R.string.action_scan_qr),
                                            onClick = {
                                                UsabilityTracker.startIntent("settings_tool_open")
                                                UsabilityTracker.endIntent(context, "settings_tool_open", outcome = "scan_qr")
                                                onOpenQRScanner()
                                            }
                                        )
                                    }
                                }

                                // Analytics & Privacy
                                SettingsCard {
                                    SettingsSectionHeader(
                                        iconRes = R.drawable.ic_analytics,
                                        title = stringResource(R.string.label_metrics_section)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val nextState = !settings.shareUsabilityMetrics
                                                updateSettings(settings.copy(shareUsabilityMetrics = nextState))
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 12.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.label_metrics_toggle),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.label_metrics_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = settings.shareUsabilityMetrics,
                                            onCheckedChange = {
                                                updateSettings(settings.copy(shareUsabilityMetrics = it))
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            )
                                        )
                                    }

                                    if (settings.shareUsabilityMetrics) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = stringResource(R.string.label_user_role),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val roles = listOf(
                                                    VoiceSettings.ROLE_CAREGIVER to stringResource(R.string.role_caregiver),
                                                    VoiceSettings.ROLE_SELF to stringResource(R.string.role_self),
                                                    VoiceSettings.ROLE_PROFESSIONAL to stringResource(R.string.role_professional)
                                                )
                                                roles.forEach { (rKey, rLabel) ->
                                                    val isSelected = settings.userRole == rKey
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = {
                                                            updateSettings(settings.copy(userRole = rKey))
                                                            UsabilityTracker.recordRoleSet(context, rKey)
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        label = {
                                                            Text(
                                                                text = rLabel,
                                                                fontSize = 12.sp,
                                                                maxLines = 1,
                                                                textAlign = TextAlign.Center,
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    var showSharedDataDialog by remember { mutableStateOf(false) }
                                    TextButton(
                                        onClick = { showSharedDataDialog = true },
                                        modifier = Modifier.align(Alignment.End),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_explore),
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.action_view_shared_data),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    if (showSharedDataDialog) {
                                        val previewText = remember(settings.shareUsabilityMetrics) { UsabilityTracker.generatePreviewPayload(context) }
                                        AlertDialog(
                                            onDismissRequest = { showSharedDataDialog = false },
                                            title = { Text(stringResource(R.string.dialog_shared_data_title)) },
                                            text = {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 350.dp)
                                                        .verticalScroll(rememberScrollState())
                                                ) {
                                                    androidx.compose.foundation.text.selection.SelectionContainer {
                                                        Text(
                                                            text = previewText,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                TextButton(onClick = { showSharedDataDialog = false }) {
                                                    Text(stringResource(R.string.cd_dismiss))
                                                }
                                            }
                                        )
                                    }
                                }

                                // About & Feedback
                                SettingsCard {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/triton1502oc/Sotto/issues"))
                                                    context.startActivity(intent)
                                                } catch (_: ActivityNotFoundException) {
                                                } catch (_: Exception) {}
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_note_alt),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = stringResource(R.string.action_feedback),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

}

@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
internal fun SettingsSectionHeader(
    iconRes: Int,
    title: String,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun SettingsToolTile(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        ListItem(
            modifier = Modifier.heightIn(min = 56.dp),
            leadingContent = {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            },
            supportingContent = if (subtitle != null) {
                { Text(text = subtitle, style = MaterialTheme.typography.bodySmall) }
            } else null,
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}
