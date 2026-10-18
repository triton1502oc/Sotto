package com.amh.sotto.ui.main

import android.speech.tts.Voice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amh.sotto.R
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.theme.SottoColors
import com.amh.sotto.util.UsabilityTracker
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsDialog(
    currentSettings: VoiceSettings,
    currentLanguage: String,
    availableLanguages: List<Pair<String, String>>,
    availableVoices: List<Voice>,
    isTtsLanguageInstalled: (String) -> Boolean = { true },
    onInstallTtsVoice: () -> Unit = {},
    onLanguageChanged: (String) -> Unit,
    onSettingsChanged: (VoiceSettings) -> Unit,
    onTestVoice: (VoiceSettings) -> Unit,
    onExportPhrases: () -> Unit = {},
    onImportPhrases: () -> Unit = {},
    onOpenQRScanner: () -> Unit = {},
    onOpenWhyLog: () -> Unit = {},
    onOpenWhyTreeEditor: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var settings by remember { mutableStateOf(currentSettings) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    fun updateSettings(newSettings: VoiceSettings) {
        val rounded = newSettings.copy(
            speechRate = ((newSettings.speechRate * 10).roundToInt() / 10f),
            speechPitch = ((newSettings.speechPitch * 10).roundToInt() / 10f)
        )
        settings = rounded
        onSettingsChanged(rounded)
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
                                color = SottoColors.ActiveGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // 3 Categorized Settings Tabs
                    val tabTitles = listOf(
                        stringResource(R.string.tab_settings_voice),
                        stringResource(R.string.tab_settings_appearance),
                        stringResource(R.string.tab_settings_data)
                    )

                    PrimaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)) }
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = {
                                    if (selectedTabIndex != index) {
                                        val outcome = when (index) {
                                            0 -> "voice"
                                            1 -> "appearance"
                                            else -> "data"
                                        }
                                        UsabilityTracker.startIntent("settings_tab_view")
                                        UsabilityTracker.endIntent(context, "settings_tab_view", outcome = outcome)
                                        selectedTabIndex = index
                                    }
                                },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }

                    // Scrollable Settings Content for Selected Tab
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (selectedTabIndex) {
                            0 -> {
                                VoiceSettingsVoiceTab(
                                    settings = settings,
                                    onUpdateSettings = { updateSettings(it) },
                                    currentLanguage = currentLanguage,
                                    availableLanguages = availableLanguages,
                                    availableVoices = availableVoices,
                                    isTtsLanguageInstalled = isTtsLanguageInstalled,
                                    onInstallTtsVoice = onInstallTtsVoice,
                                    onLanguageChanged = onLanguageChanged,
                                    onTestVoice = onTestVoice
                                )
                            }
                            1 -> {
                                VoiceSettingsAppearanceTab(
                                    settings = settings,
                                    onUpdateSettings = { updateSettings(it) },
                                    onEnterPip = onEnterPip
                                )
                            }
                            2 -> {
                                VoiceSettingsDataTab(
                                    settings = settings,
                                    onUpdateSettings = { updateSettings(it) },
                                    onExportPhrases = onExportPhrases,
                                    onImportPhrases = onImportPhrases,
                                    onOpenQRScanner = onOpenQRScanner,
                                    onOpenWhyLog = onOpenWhyLog,
                                    onOpenWhyTreeEditor = onOpenWhyTreeEditor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
