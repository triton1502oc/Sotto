package com.amh.sotto.ui.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amh.sotto.BuildConfig
import com.amh.sotto.R
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.util.ChimePlayer
import com.amh.sotto.util.LocaleHelper
import com.amh.sotto.util.TranslationHelper
import com.amh.sotto.util.UsabilityTracker
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsDialog(
    currentSettings: VoiceSettings,
    currentLanguage: String,
    availableLanguages: List<Pair<String, String>> = remember { LocaleHelper.getAvailableLanguages() },
    isTtsLanguageInstalled: (String) -> Boolean = { true },
    onInstallTtsVoice: () -> Unit = {},
    onLanguageChanged: (String) -> Unit,
    onSettingsChanged: (VoiceSettings) -> Unit,
    onTestVoice: (VoiceSettings) -> Unit,
    onExportPhrases: () -> Unit = {},
    onImportPhrases: () -> Unit = {},
    onOpenWhyLog: () -> Unit = {},
    onOpenWhyTreeEditor: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var rate by remember { mutableFloatStateOf(currentSettings.speechRate) }
    var pitch by remember { mutableFloatStateOf(currentSettings.speechPitch) }
    var attentionChime by remember { mutableStateOf(currentSettings.playAttentionChime) }
    var showLangSwitcher by remember { mutableStateOf(currentSettings.showLanguageSwitcher) }
    var secondaryLang by remember { mutableStateOf(currentSettings.secondaryLanguage) }
    var shareMetrics by remember { mutableStateOf(currentSettings.shareUsabilityMetrics) }
    var userRole by remember { mutableStateOf(currentSettings.userRole) }
    var secondaryLangDropdownExpanded by remember { mutableStateOf(false) }
    var langDropdownExpanded by remember { mutableStateOf(false) }
    var langSearchQuery by remember { mutableStateOf("") }

    val systemDefaultLabel = stringResource(R.string.system_default)

    fun updateSettings(
        newRate: Float = rate,
        newPitch: Float = pitch,
        newChime: Boolean = attentionChime,
        newShowLangSwitcher: Boolean = showLangSwitcher,
        newSecondaryLang: String = secondaryLang,
        newShareMetrics: Boolean = shareMetrics,
        newUserRole: String = userRole
    ) {
        rate = newRate
        pitch = newPitch
        attentionChime = newChime
        showLangSwitcher = newShowLangSwitcher
        secondaryLang = newSecondaryLang
        shareMetrics = newShareMetrics
        userRole = newUserRole
        onSettingsChanged(
            VoiceSettings(
                speechRate = ((newRate * 10).roundToInt() / 10f),
                speechPitch = ((newPitch * 10).roundToInt() / 10f),
                voiceName = null,
                playAttentionChime = newChime,
                showLanguageSwitcher = newShowLangSwitcher,
                secondaryLanguage = newSecondaryLang,
                shareUsabilityMetrics = newShareMetrics,
                userRole = newUserRole
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // Top App Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.dialog_voice_settings_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.action_done),
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Scrollable Settings Sections
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section 1: Speech & Voice
                        SettingsCard {
                            SettingsSectionHeader(
                                icon = "🗣️",
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
                                            Text(
                                                text = "▼",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            color = Color(0xFFFFA726),
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

                            // Speech Rate (Speed)
                            Column {
                                val rateText = when {
                                    rate <= 0.7f -> stringResource(R.string.speed_slower)
                                    rate in 0.9f..1.1f -> stringResource(R.string.speed_normal)
                                    rate >= 1.3f -> stringResource(R.string.speed_faster)
                                    else -> ""
                                }
                                val speedDisplay = String.format(Locale.US, "%.1fx", rate)
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

                            // Immediate Test Voice Action
                            OutlinedButton(
                                onClick = {
                                    onTestVoice(
                                        VoiceSettings(
                                            speechRate = ((rate * 10).roundToInt() / 10f),
                                            speechPitch = ((pitch * 10).roundToInt() / 10f),
                                            voiceName = null,
                                            playAttentionChime = attentionChime,
                                            showLanguageSwitcher = showLangSwitcher,
                                            secondaryLanguage = secondaryLang,
                                            shareUsabilityMetrics = shareMetrics,
                                            userRole = userRole
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("🔊  " + stringResource(R.string.action_test_voice))
                            }
                        }

                        // Section 2: Dual-Language
                        SettingsCard {
                            SettingsSectionHeader(
                                icon = "🌐",
                                title = stringResource(R.string.label_dual_language_section)
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
                                    val secondaryLanguageLabel = remember(secondaryLang, secondaryLanguagesList) {
                                        secondaryLanguagesList.firstOrNull { it.first == secondaryLang }?.second
                                            ?: LocaleHelper.getLanguageDisplayName(secondaryLang)
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
                                                Text(
                                                    text = "▼",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                                    // Translation model status & download
                                    var isModelDownloaded by remember(secondaryLang) { mutableStateOf<Boolean?>(null) }
                                    var isDownloadingModel by remember { mutableStateOf(false) }
                                    var downloadErrorResId by remember { mutableStateOf<Int?>(null) }

                                    LaunchedEffect(secondaryLang) {
                                        TranslationHelper.isModelDownloaded(secondaryLang) { downloaded ->
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
                                                    color = if (isModelDownloaded == true) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (downloadErrorResId != null) {
                                                    Text(
                                                        text = stringResource(downloadErrorResId!!),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFFEF5350)
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
                                                                langCode = secondaryLang,
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
                                    val isVoicePackInstalled = isTtsLanguageInstalled(secondaryLang)
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
                                                    text = stringResource(R.string.label_tts_voice_pack, LocaleHelper.getLanguageDisplayName(secondaryLang)),
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
                                                    color = if (isVoicePackInstalled) Color(0xFF81C784) else Color(0xFFFFA726)
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

                        // Section 3: Guided Tools & Backup
                        SettingsCard {
                            SettingsSectionHeader(
                                icon = "🧭",
                                title = stringResource(R.string.why_finder_title)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenWhyLog,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Text("📋 ", fontSize = 14.sp)
                                    Text(
                                        text = stringResource(R.string.action_view_why_log),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                OutlinedButton(
                                    onClick = onOpenWhyTreeEditor,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Text("🌳 ", fontSize = 14.sp)
                                    Text(
                                        text = stringResource(R.string.action_edit_why_tree),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            SettingsSectionHeader(
                                icon = "💾",
                                title = stringResource(R.string.label_backup_section),
                                subtitle = stringResource(R.string.label_backup_desc)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onExportPhrases,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Text("↑ ", fontSize = 14.sp)
                                    Text(
                                        text = stringResource(R.string.action_export_phrases),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                OutlinedButton(
                                    onClick = onImportPhrases,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Text("↓ ", fontSize = 14.sp)
                                    Text(
                                        text = stringResource(R.string.action_import_phrases),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        // Section 4: Analytics & Privacy
                        SettingsCard {
                            SettingsSectionHeader(
                                icon = "📊",
                                title = stringResource(R.string.label_metrics_section)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val nextState = !shareMetrics
                                        updateSettings(newShareMetrics = nextState)
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
                                    checked = shareMetrics,
                                    onCheckedChange = {
                                        updateSettings(newShareMetrics = it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                )
                            }

                            if (shareMetrics) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = stringResource(R.string.label_user_role),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Equal-width chips that fit cleanly on screen
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
                                            val isSelected = userRole == rKey
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    updateSettings(newUserRole = rKey)
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

                                    var showSharedDataDialog by remember { mutableStateOf(false) }
                                    TextButton(
                                        onClick = { showSharedDataDialog = true },
                                        modifier = Modifier.align(Alignment.End),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "🔍 " + stringResource(R.string.action_view_shared_data),
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (showSharedDataDialog) {
                                        val previewText = remember { UsabilityTracker.generatePreviewPayload(context) }
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
                            }
                        }

                        // Section 5: About & Feedback
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
                                        } catch (e: ActivityNotFoundException) {
                                            // Ignore if no browser
                                        } catch (e: Exception) {
                                            // Protect against any unexpected exception
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "💬 " + stringResource(R.string.action_feedback),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
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
private fun SettingsSectionHeader(
    icon: String,
    title: String,
    subtitle: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(icon, fontSize = 16.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
