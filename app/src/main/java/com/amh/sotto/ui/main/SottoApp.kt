package com.amh.sotto.ui.main

import android.Manifest
import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.amh.sotto.service.EmergencyTileService
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.Voice
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.amh.sotto.R
import com.amh.sotto.data.Phrase
import com.amh.sotto.data.SharedPreferencesWhyFinderRepository
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.data.WhyFinderRepository
import com.amh.sotto.theme.SottoColors
import com.amh.sotto.ui.category.ManageCategoriesDialog
import com.amh.sotto.ui.category.getCategoryDisplayName
import com.amh.sotto.ui.conversation.CaregiverTab
import com.amh.sotto.ui.conversation.TwoWayConversationDialog
import com.amh.sotto.ui.main.ExportBackupDialog
import com.amh.sotto.ui.main.ImportBackupDialog
import com.amh.sotto.ui.main.PmfSurveyDialog
import com.amh.sotto.ui.main.VoiceSettingsDialog
import com.amh.sotto.ui.whyfinder.WhyLogDialog
import com.amh.sotto.ui.whyfinder.WhyTreeEditorDialog
import com.amh.sotto.util.BackupData
import com.amh.sotto.util.ChimePlayer
import com.amh.sotto.util.ImportResult
import com.amh.sotto.util.LocaleHelper
import com.amh.sotto.util.PhraseBackupHelper
import com.amh.sotto.util.SpeechRecognitionHelper
import com.amh.sotto.util.TranslationHelper
import com.amh.sotto.util.UsabilityTracker
import sh.calvin.reorderable.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SottoApp(
    phrases: List<Phrase>,
    ttsReady: Boolean,
    voiceSettings: VoiceSettings,
    availableVoices: List<Voice>,
    availableTtsLanguages: Set<Locale> = emptySet(),
    currentLanguage: String,
    onLanguageChanged: (String) -> Unit,
    onSpeak: (Phrase) -> Unit,
    onSpeakText: (String, String?) -> Unit = { _, _ -> },
    onAddPhrase: (Phrase) -> Unit,
    onEditPhrase: (Phrase, Phrase) -> Unit,
    onDeletePhrase: (Phrase) -> Unit,
    onMovePhrase: (Phrase, Phrase) -> Unit,
    onUpdateVoiceSettings: (VoiceSettings) -> Unit,
    onTestVoice: (VoiceSettings) -> Unit,
    isTtsLanguageInstalled: (String) -> Boolean = { true },
    onInstallTtsVoice: () -> Unit = {},
    categories: List<String> = emptyList(),
    onAddCategory: (String) -> Boolean = { false },
    onRenameCategory: (String, String) -> Boolean = { _, _ -> false },
    onDeleteCategory: (String) -> Boolean = { false },
    onReorderCategories: (Int, Int) -> Unit = { _, _ -> },
    customCategories: List<String> = emptyList(),
    onAddCustomCategory: (String) -> Boolean = { false },
    onRenameCustomCategory: (String, String) -> Boolean = { _, _ -> false },
    onDeleteCustomCategory: (String) -> Boolean = { false },
    onExportBackup: (Boolean, Boolean, Boolean, WhyFinderRepository?) -> String = { _, _, _, _ -> "" },
    onImportBackup: (BackupData, Boolean, WhyFinderRepository?) -> ImportResult = { _, _, _ -> ImportResult(0, 0) },
    shouldPromptMetrics: Boolean = false,
    onMetricsPromptAnswered: (Boolean) -> Unit = {},
    shouldPromptRole: Boolean = false,
    onRolePromptAnswered: (String) -> Unit = {},
    shouldPromptPmfSurvey: Boolean = false,
    onPmfSurveyAnswered: (String, String, String) -> Unit = { _, _, _ -> },
    onPmfSurveyDismissed: () -> Unit = {},
    initialOpenPartnerMode: Boolean = false,
    onPartnerModeOpened: () -> Unit = {},
    onEnterPip: () -> Unit = {}
) {
    val context = LocalContext.current
    val effectiveCategories = if (categories.isNotEmpty()) categories else customCategories
    var expandedPhrase by remember { mutableStateOf<Phrase?>(null) }
    var showMetricsOptInDialog by rememberSaveable { mutableStateOf(shouldPromptMetrics) }
    var showRolePromptDialog by rememberSaveable { mutableStateOf(shouldPromptRole) }
    var showPmfSurveyDialog by rememberSaveable { mutableStateOf(shouldPromptPmfSurvey) }
    var showAddDialog by remember { mutableStateOf(false) }
    var initialAddText by rememberSaveable { mutableStateOf("") }
    var showVoiceDialog by rememberSaveable { mutableStateOf(false) }
    var phraseToEdit by remember { mutableStateOf<Phrase?>(null) }
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var activeSpeechTarget by rememberSaveable { mutableStateOf("primary") }
    var quickText by rememberSaveable { mutableStateOf("") }
    var showManageCategoriesDialog by rememberSaveable { mutableStateOf(false) }
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var categoryToManage by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryToDeleteConfirm by rememberSaveable { mutableStateOf<String?>(null) }

    var showTwoWayDialog by rememberSaveable { mutableStateOf(false) }
    var isListeningTwoWay by remember { mutableStateOf(false) }
    var liveSpokenTextTwoWay by remember { mutableStateOf("") }
    var showAudioRationaleDialog by rememberSaveable { mutableStateOf(false) }
    var showMissingOfflineSpeechDialog by rememberSaveable { mutableStateOf(false) }
    var missingOfflineLanguageName by rememberSaveable { mutableStateOf("") }
    val whyFinderRepository = remember { SharedPreferencesWhyFinderRepository(context) }
    var showWhyLogDialog by rememberSaveable { mutableStateOf(false) }
    var showWhyTreeEditorDialog by rememberSaveable { mutableStateOf(false) }
    var activeQRShareString by remember { mutableStateOf<String?>(null) }
    var showQRScannerDialog by rememberSaveable { mutableStateOf(false) }
    var partnerModeInitialTab by remember { mutableStateOf<CaregiverTab?>(null) }
    var showQsTileDiscoveryDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialOpenPartnerMode) {
        if (initialOpenPartnerMode) {
            UsabilityTracker.startIntent("TwoWay", contextTag = "tile")
            partnerModeInitialTab = CaregiverTab.WHY_FINDER
            showTwoWayDialog = true
            onPartnerModeOpened()
        }
    }

    val speechHelper = remember { SpeechRecognitionHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.destroy()
        }
    }

    val twoWaySpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                liveSpokenTextTwoWay = matches[0]
            }
        }
        isListeningTwoWay = false
    }

    val listenModePrompt = stringResource(R.string.listen_mode_hint)
    fun launchSystemSpeechFallback() {
        val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
        val langTag = LocaleHelper.getLocaleForLanguage(effectiveLang).toLanguageTag()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, listenModePrompt)
        }
        try {
            twoWaySpeechLauncher.launch(intent)
        } catch (_: Exception) {}
    }

    fun startListeningWithPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (!speechHelper.isAvailable()) {
                launchSystemSpeechFallback()
                return
            }
            val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
            val locale = LocaleHelper.getLocaleForLanguage(effectiveLang)
            isListeningTwoWay = true
            liveSpokenTextTwoWay = ""
            speechHelper.startListening(
                locale = locale,
                onPartialResult = { partial ->
                    liveSpokenTextTwoWay = partial
                },
                onFinalResult = { final ->
                    liveSpokenTextTwoWay = final
                    isListeningTwoWay = false
                },
                onError = { errorCode ->
                    isListeningTwoWay = false
                    if (SpeechRecognitionHelper.isMissingOfflineModelError(errorCode)) {
                        speechHelper.triggerModelDownload(locale)
                        missingOfflineLanguageName = LocaleHelper.getLanguageDisplayName(effectiveLang)
                        showMissingOfflineSpeechDialog = true
                        UsabilityTracker.recordOfflineSpeechModelMissing(context, effectiveLang)
                    } else if (errorCode != SpeechRecognizer.ERROR_NO_MATCH && errorCode != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        launchSystemSpeechFallback()
                    }
                }
            )
        } else {
            showAudioRationaleDialog = true
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningWithPermission()
        } else {
            isListeningTwoWay = false
        }
    }

    var pendingBackupData by remember { mutableStateOf<BackupData?>(null) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var pendingExportOptions by remember { mutableStateOf(Triple(true, true, false)) }

    val exportSuccessTemplate = stringResource(R.string.msg_export_success)
    val exportErrorTemplate = stringResource(R.string.error_export_failed)
    val importSuccessTemplate = stringResource(R.string.msg_import_success)
    val importTreeSuccessMsg = stringResource(R.string.msg_import_tree_success)
    val importErrorMsg = stringResource(R.string.error_import_failed)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = onExportBackup(
                    pendingExportOptions.first,
                    pendingExportOptions.second,
                    pendingExportOptions.third,
                    whyFinderRepository
                )
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(json.toByteArray(Charsets.UTF_8))
                }
                val count = if (pendingExportOptions.first) phrases.size else 0
                Toast.makeText(
                    context,
                    String.format(Locale.getDefault(), exportSuccessTemplate, count),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    String.format(Locale.getDefault(), exportErrorTemplate, e.localizedMessage ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!json.isNullOrBlank()) {
                    pendingBackupData = PhraseBackupHelper.parseBackupJson(json)
                } else {
                    Toast.makeText(context, importErrorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, importErrorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun stopTwoWayListening() {
        isListeningTwoWay = false
        speechHelper.stopListening()
    }

    val allCategoryKey = "ALL"
    var selectedCategory by rememberSaveable { mutableStateOf(allCategoryKey) }

    val systemCategories = remember {
        listOf(
            allCategoryKey to R.string.category_all,
            Phrase.CATEGORY_EMERGENCY to R.string.category_emergency,
            Phrase.CATEGORY_NEEDS to R.string.category_needs,
            Phrase.CATEGORY_SOCIAL to R.string.category_social,
            Phrase.CATEGORY_CARE to R.string.category_care,
            Phrase.CATEGORY_GENERAL to R.string.category_general
        )
    }

    val filteredPhrases = remember(phrases, selectedCategory, voiceSettings.showSafetyCards) {
        if (selectedCategory == allCategoryKey) {
            if (voiceSettings.showSafetyCards) phrases else phrases.filter { !it.isEmergency }
        } else if (selectedCategory == Phrase.CATEGORY_EMERGENCY) {
            phrases.filter { it.isEmergency }
        } else {
            phrases.filter { !it.isEmergency && it.category == selectedCategory }
        }
    }

    val lazyGridState = rememberLazyGridState()
    val state = sh.calvin.reorderable.rememberReorderableLazyGridState(
        lazyGridState = lazyGridState,
        onMove = { from, to ->
            val fromPhrase = filteredPhrases.getOrNull(from.index)
            val toPhrase = filteredPhrases.getOrNull(to.index)
            if (fromPhrase != null && toPhrase != null) {
                onMovePhrase(fromPhrase, toPhrase)
            }
        }
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        if (voiceSettings.showLanguageSwitcher) {
                            val context = LocalContext.current
                            val primarySelected = activeSpeechTarget == "primary"
                            val primaryLabel = LocaleHelper.getLocaleForLanguage(
                                LocaleHelper.getEffectiveLanguage(context)
                            ).language.uppercase(Locale.ROOT)
                            val secondaryLabel = voiceSettings.secondaryLanguage.uppercase(Locale.ROOT)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(modifier = Modifier.padding(2.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (primarySelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        modifier = Modifier.clickable { activeSpeechTarget = "primary" }
                                    ) {
                                        Text(
                                            text = primaryLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (primarySelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (primarySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (!primarySelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        modifier = Modifier.clickable { activeSpeechTarget = "secondary" }
                                    ) {
                                        Text(
                                            text = secondaryLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (!primarySelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (!primarySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        val listenModeCd = stringResource(R.string.cd_listen_mode)
                        // Partner Mode CTA
                        IconButton(
                            onClick = {
                                UsabilityTracker.startIntent("TwoWay", contextTag = "topbar")
                                showTwoWayDialog = true
                            },
                            modifier = Modifier.semantics { contentDescription = listenModeCd }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_partner_mode),
                                        contentDescription = listenModeCd,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Picture-in-Picture Trigger (optional top bar shortcut #64)
                        if (voiceSettings.enablePip && voiceSettings.showPipInTopBar) {
                            val pipCd = stringResource(R.string.cd_enter_pip)
                            IconButton(
                                onClick = onEnterPip,
                                modifier = Modifier.semantics { contentDescription = pipCd }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_pip),
                                    contentDescription = pipCd,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Voice & App Settings (Always visible)
                        IconButton(onClick = { showVoiceDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = stringResource(R.string.action_settings),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Edit List / Done Pill Button
                        Surface(
                            onClick = {
                                val entering = !isEditMode
                                isEditMode = entering
                                UsabilityTracker.startIntent("edit_mode_toggle")
                                UsabilityTracker.endIntent(context, "edit_mode_toggle", outcome = if (entering) "enter" else "exit")
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isEditMode) SottoColors.SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isEditMode) SottoColors.ActiveGreen else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .padding(start = 4.dp, end = 8.dp)
                        ) {
                            AnimatedContent(
                                targetState = isEditMode,
                                label = "editPillTransition"
                            ) { editModeActive ->
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(if (editModeActive) R.drawable.ic_check else R.drawable.ic_edit),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (editModeActive) SottoColors.ActiveGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (editModeActive) stringResource(R.string.action_done) else stringResource(R.string.action_edit_list),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (editModeActive) SottoColors.ActiveGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (!ttsReady) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = SottoColors.WarningAmber,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.status_initializing),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Black
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )

                // Hero Partner Mode Entry Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.section_my_board),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            UsabilityTracker.startIntent("TwoWay", contextTag = "hero")
                            partnerModeInitialTab = null
                            showTwoWayDialog = true
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_partner_mode),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.title_partner_mode),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Category Filter Chips
                val mainBarCategories = effectiveCategories

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        val isSelected = selectedCategory == allCategoryKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = allCategoryKey },
                            modifier = Modifier.sizeIn(minHeight = 48.dp),
                            label = {
                                Text(
                                    text = stringResource(R.string.category_all),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                    }

                    items(mainBarCategories) { catName ->
                        val isSelected = selectedCategory == catName
                        val isEmergency = catName.equals(Phrase.CATEGORY_EMERGENCY, ignoreCase = true)
                        val isProtectedFromDelete = Phrase.isUndeletableCategory(catName)
                        val isProtectedFromRename = Phrase.isUnrenamableCategory(catName)

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isEditMode && !isProtectedFromRename) {
                                    categoryToManage = catName
                                } else {
                                    selectedCategory = catName
                                }
                            },
                            modifier = Modifier.sizeIn(minHeight = 48.dp),
                            leadingIcon = if (isEmergency) {
                                {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_shield),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) SottoColors.EmergencyAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = getCategoryDisplayName(catName),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isEmergency) SottoColors.EmergencyChipBackground else MaterialTheme.colorScheme.surfaceVariant,
                                selectedLabelColor = if (isEmergency) SottoColors.EmergencyTextSecondary else MaterialTheme.colorScheme.onSurface,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ),
                            border = if (isEmergency && isSelected) {
                                BorderStroke(1.dp, SottoColors.EmergencyAccent)
                            } else null
                        )
                    }

                    if (isEditMode) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    UsabilityTracker.startIntent("manage_categories_open")
                                    UsabilityTracker.endIntent(context, "manage_categories_open", outcome = "opened")
                                    showManageCategoriesDialog = true
                                },
                                modifier = Modifier.sizeIn(minHeight = 48.dp),
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_tune),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.action_manage_categories),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { showAddCategoryDialog = true },
                                modifier = Modifier.sizeIn(minHeight = 48.dp),
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_add),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.action_add_category),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                var isQuickTranslating by remember { mutableStateOf(false) }
                var isQuickListening by remember { mutableStateOf(false) }
                var quickBaseText by remember { mutableStateOf("") }
                val voiceInputPrompt = stringResource(R.string.cd_voice_input)

                val quickSpeechLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        if (!matches.isNullOrEmpty()) {
                            quickText = if (quickText.isBlank()) matches[0] else "$quickText ${matches[0]}"
                        }
                    }
                }

                fun stopQuickListening() {
                    if (isQuickListening) {
                        speechHelper.stopListening()
                        isQuickListening = false
                    }
                }

                val quickAudioPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                        val locale = LocaleHelper.getLocaleForLanguage(effectiveLang)
                        quickBaseText = quickText.trim()
                        isQuickListening = true
                        speechHelper.startListening(
                            locale = locale,
                            onPartialResult = { partial ->
                                quickText = if (quickBaseText.isEmpty()) partial else "$quickBaseText $partial"
                            },
                            onFinalResult = { final ->
                                quickText = if (quickBaseText.isEmpty()) final else "$quickBaseText $final"
                                isQuickListening = false
                            },
                            onError = { errorCode ->
                                isQuickListening = false
                                if (SpeechRecognitionHelper.isMissingOfflineModelError(errorCode)) {
                                    speechHelper.triggerModelDownload(locale)
                                    missingOfflineLanguageName = LocaleHelper.getLanguageDisplayName(effectiveLang)
                                    showMissingOfflineSpeechDialog = true
                                    UsabilityTracker.recordOfflineSpeechModelMissing(context, effectiveLang)
                                }
                            }
                        )
                    } else {
                        isQuickListening = false
                    }
                }

                fun toggleQuickListening() {
                    if (isQuickListening) {
                        stopQuickListening()
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            if (!speechHelper.isAvailable()) {
                                val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                                val langTag = LocaleHelper.getLocaleForLanguage(effectiveLang).toLanguageTag()
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, voiceInputPrompt)
                                }
                                try {
                                    quickSpeechLauncher.launch(intent)
                                } catch (_: Exception) {}
                                return
                            }
                            val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                            val locale = LocaleHelper.getLocaleForLanguage(effectiveLang)
                            quickBaseText = quickText.trim()
                            isQuickListening = true
                            speechHelper.startListening(
                                locale = locale,
                                onPartialResult = { partial ->
                                    quickText = if (quickBaseText.isEmpty()) partial else "$quickBaseText $partial"
                                },
                                onFinalResult = { final ->
                                    quickText = if (quickBaseText.isEmpty()) final else "$quickBaseText $final"
                                    isQuickListening = false
                                },
                                onError = { errorCode ->
                                    isQuickListening = false
                                    if (SpeechRecognitionHelper.isMissingOfflineModelError(errorCode)) {
                                        speechHelper.triggerModelDownload(locale)
                                        missingOfflineLanguageName = LocaleHelper.getLanguageDisplayName(effectiveLang)
                                        showMissingOfflineSpeechDialog = true
                                        UsabilityTracker.recordOfflineSpeechModelMissing(context, effectiveLang)
                                    }
                                }
                            )
                        } else {
                            quickAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }

                fun handleQuickSpeak(text: String) {
                    stopQuickListening()
                    if (text.isBlank()) return
                    UsabilityTracker.endIntent(context, "QuickSpeak", outcome = "completed", contextTag = "dock_v2 len_${text.length}")
                    if (voiceSettings.showLanguageSwitcher && activeSpeechTarget == "secondary") {
                        isQuickTranslating = true
                        val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                        val sourceLang = LocaleHelper.resolvePhraseLocale(LocaleHelper.LANG_AUTO, text, effectiveLang).language
                        val targetLang = voiceSettings.secondaryLanguage
                        TranslationHelper.translate(
                            text = text,
                            sourceLangCode = sourceLang,
                            targetLangCode = targetLang,
                            onProgress = { isQuickTranslating = it },
                            onSuccess = { translated ->
                                isQuickTranslating = false
                                onSpeakText(translated, targetLang)
                            },
                            onError = {
                                isQuickTranslating = false
                                onSpeakText(text, targetLang)
                            }
                        )
                    } else {
                        onSpeakText(text, LocaleHelper.LANG_AUTO)
                    }
                }

                fun handleQuickFullscreen(text: String) {
                    if (text.isBlank()) return
                    UsabilityTracker.endIntent(context, "QuickSpeak", outcome = "shown_fullscreen", contextTag = "dock_v2 len_${text.length}")
                    expandedPhrase = Phrase(text = text, language = LocaleHelper.LANG_AUTO)
                    if (voiceSettings.showLanguageSwitcher) {
                        val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                        val sourceLang = LocaleHelper.resolvePhraseLocale(LocaleHelper.LANG_AUTO, text, effectiveLang).language
                        val targetLang = voiceSettings.secondaryLanguage
                        TranslationHelper.translate(
                            text = text,
                            sourceLangCode = sourceLang,
                            targetLangCode = targetLang,
                            onProgress = {},
                            onSuccess = { translated ->
                                if (expandedPhrase?.text == text) {
                                    expandedPhrase = expandedPhrase?.copy(spokenText = translated, spokenLanguage = targetLang)
                                }
                            },
                            onError = {}
                        )
                    }
                }

                val saveCardCd = stringResource(R.string.cd_save_as_card)
                val showFullscreenCd = stringResource(R.string.cd_show_fullscreen)
                val quickSpeakCd = stringResource(R.string.cd_quick_speak)
                val stopVoiceInputCd = stringResource(R.string.cd_stop_voice_input)
                val clearTextCd = stringResource(R.string.cd_clear_text)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp, max = 56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    stopQuickListening()
                                    if (quickText.isNotBlank()) {
                                        handleQuickFullscreen(quickText.trim())
                                    }
                                },
                                enabled = quickText.isNotBlank(),
                                modifier = Modifier
                                    .size(40.dp)
                                    .semantics { contentDescription = showFullscreenCd }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_fullscreen),
                                    contentDescription = showFullscreenCd,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (quickText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }

                            TextField(
                                value = quickText,
                                onValueChange = {
                                    if (quickText.isBlank() && it.isNotBlank()) {
                                        UsabilityTracker.startIntent("QuickSpeak")
                                    }
                                    quickText = it
                                },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.hint_quick_speak),
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        stopQuickListening()
                                        if (quickText.isNotBlank()) {
                                            handleQuickSpeak(quickText.trim())
                                        }
                                    }
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                            )

                            if (isQuickListening) {
                                IconButton(
                                    onClick = { stopQuickListening() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .semantics { contentDescription = stopVoiceInputCd }
                                ) {
                                    Text(
                                        "⏹",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            if (quickText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        stopQuickListening()
                                        val trimmed = quickText.trim()
                                        val alreadyExists = phrases.any { it.text.trim().equals(trimmed, ignoreCase = true) }
                                        if (alreadyExists) {
                                            UsabilityTracker.recordFriction(context, "duplicate_phrase_blocked")
                                            android.widget.Toast.makeText(context, context.getString(R.string.error_phrase_exists), android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        UsabilityTracker.endIntent(context, "QuickSpeak", outcome = if (alreadyExists) "duplicate_blocked" else "saved_as_card", contextTag = "dock_v2 len_${trimmed.length}")
                                        initialAddText = trimmed
                                        showAddDialog = true
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .semantics { contentDescription = saveCardCd }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_add),
                                        contentDescription = saveCardCd,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        stopQuickListening()
                                        UsabilityTracker.endIntent(
                                            context,
                                            "QuickSpeak",
                                            outcome = "aborted",
                                            frictionTag = "cleared_text",
                                            contextTag = "dock_v2 len_${quickText.length}"
                                        )
                                        quickText = ""
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .semantics { contentDescription = clearTextCd }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = clearTextCd,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            } else if (!isQuickListening) {
                                IconButton(
                                    onClick = { toggleQuickListening() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .semantics { contentDescription = voiceInputPrompt }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_mic),
                                        contentDescription = voiceInputPrompt,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    FilledIconButton(
                        onClick = {
                            if (quickText.isNotBlank()) {
                                handleQuickSpeak(quickText.trim())
                            }
                        },
                        enabled = quickText.isNotBlank() && !isQuickTranslating,
                        modifier = Modifier
                            .size(52.dp)
                            .semantics { contentDescription = quickSpeakCd },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isQuickTranslating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_volume_up),
                                contentDescription = quickSpeakCd,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (isEditMode) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.cd_add_phrase)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                state = lazyGridState,
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
            items(
                items = filteredPhrases,
                key = { it.text },
                span = { phrase ->
                    if (phrase.isEmergency) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                }
            ) { phrase ->
                ReorderableItem(state, key = phrase.text) { isDragging ->
                    val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                    var cardModifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (phrase.isEmergency) {
                                Modifier.wrapContentHeight()
                            } else if (voiceSettings.compactCardLayout) {
                                Modifier.aspectRatio(1.25f)
                            } else {
                                Modifier.aspectRatio(1f)
                            }
                        )
                        .clip(RoundedCornerShape(16.dp))
                        
                    val speakActionLabel = stringResource(R.string.action_speak)
                    val fullscreenActionLabel = stringResource(R.string.cd_show_fullscreen)
                    val cardCd = if (phrase.isEmergency) "Safety: ${phrase.text}" else phrase.text

                    fun performSpeak(p: Phrase) {
                        UsabilityTracker.recordCardTap(context, p.category, p.isEmergency, System.identityHashCode(p))
                        if (voiceSettings.showLanguageSwitcher) {
                            if (activeSpeechTarget == "secondary") {
                                val hasValidSpoken = !p.spokenText.isNullOrBlank() && p.spokenText != LocaleHelper.LANG_AUTO
                                if (hasValidSpoken) {
                                    onSpeakText(p.spokenText, p.spokenLanguage ?: voiceSettings.secondaryLanguage)
                                } else {
                                    val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                                    val sourceLang = LocaleHelper.resolvePhraseLocale(p.language, p.text, effectiveLang).language
                                    val targetLang = voiceSettings.secondaryLanguage
                                    TranslationHelper.translate(
                                        text = p.text,
                                        sourceLangCode = sourceLang,
                                        targetLangCode = targetLang,
                                        onProgress = {},
                                        onSuccess = { translated ->
                                            onSpeakText(translated, targetLang)
                                            onEditPhrase(p, p.copy(spokenText = translated, spokenLanguage = targetLang))
                                        },
                                        onError = {
                                            onSpeakText(p.text, targetLang)
                                        }
                                    )
                                }
                            } else {
                                onSpeakText(p.text, p.language)
                            }
                        } else {
                            onSpeak(p)
                        }
                    }

                    cardModifier = cardModifier.semantics {
                        contentDescription = cardCd
                        role = Role.Button
                        if (!isEditMode) {
                            customActions = listOf(
                                CustomAccessibilityAction(label = speakActionLabel) {
                                    performSpeak(phrase)
                                    true
                                },
                                CustomAccessibilityAction(label = fullscreenActionLabel) {
                                    expandedPhrase = phrase
                                    true
                                }
                            )
                        }
                    }

                    cardModifier = if (isEditMode) {
                        cardModifier
                            .longPressDraggableHandle()
                            .clickable { phraseToEdit = phrase }
                    } else {
                        cardModifier.combinedClickable(
                            onClick = { performSpeak(phrase) },
                            onLongClick = { expandedPhrase = phrase }
                        )
                    }

                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(defaultElevation = elevation.value),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEditMode) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else if (phrase.isEmergency) {
                                SottoColors.EmergencyCardBackground
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = if (phrase.isEmergency) BorderStroke(1.5.dp, SottoColors.EmergencyAccent) else null
                    ) {
                        if (phrase.isEmergency) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = SottoColors.EmergencyTranslucent,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_shield),
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = SottoColors.EmergencyAccent
                                            )
                                            Text(
                                                text = stringResource(R.string.emergency_badge),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SottoColors.EmergencyAccent
                                            )
                                        }
                                    }
                                    if (isEditMode) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_drag_indicator),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = SottoColors.EmergencyAccent.copy(alpha = 0.6f)
                                        )
                                    } else {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { expandedPhrase = phrase },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_fullscreen),
                                                    contentDescription = stringResource(R.string.cd_show_fullscreen),
                                                    tint = SottoColors.EmergencyAccent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { performSpeak(phrase) },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_volume_up),
                                                    contentDescription = stringResource(R.string.action_speak),
                                                    modifier = Modifier.size(20.dp),
                                                    tint = SottoColors.EmergencyAccent
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = phrase.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = SottoColors.EmergencyText,
                                    lineHeight = 24.sp
                                )
                                val hasEmergencySpoken = voiceSettings.showLanguageSwitcher &&
                                    !phrase.spokenText.isNullOrBlank() &&
                                    phrase.spokenText != LocaleHelper.LANG_AUTO
                                if (hasEmergencySpoken) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_record_voice_over),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = SottoColors.EmergencyTextSecondary.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = phrase.spokenText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SottoColors.EmergencyTextSecondary.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            val hasCardSpoken = voiceSettings.showLanguageSwitcher &&
                                !phrase.spokenText.isNullOrBlank() &&
                                phrase.spokenText != LocaleHelper.LANG_AUTO
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (isEditMode) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_drag_indicator),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (voiceSettings.compactCardLayout) 8.dp else 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = phrase.text,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = if (hasCardSpoken) 3 else 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (hasCardSpoken) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_record_voice_over),
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = phrase.spokenText,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
    }
}

    val availableLanguages = remember(availableTtsLanguages) {
        LocaleHelper.getAvailableLanguages(availableTtsLanguages)
    }

    // Voice Settings Dialog
    if (showVoiceDialog) {
        VoiceSettingsDialog(
            currentSettings = voiceSettings,
            currentLanguage = currentLanguage,
            availableLanguages = availableLanguages,
            availableVoices = availableVoices,
            isTtsLanguageInstalled = isTtsLanguageInstalled,
            onInstallTtsVoice = onInstallTtsVoice,
            onLanguageChanged = onLanguageChanged,
            onSettingsChanged = onUpdateVoiceSettings,
            onTestVoice = onTestVoice,
            onExportPhrases = { showExportDialog = true },
            onImportPhrases = {
                importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
            onOpenQRScanner = {
                showVoiceDialog = false
                showQRScannerDialog = true
            },
            onOpenWhyLog = { showWhyLogDialog = true },
            onOpenWhyTreeEditor = { showWhyTreeEditorDialog = true },
            onEnterPip = {
                showVoiceDialog = false
                onEnterPip()
            },
            onDismiss = {
                showVoiceDialog = false
                val defaultTexts = try {
                    context.resources.getStringArray(R.array.default_phrases_emergency).toSet() +
                    context.resources.getStringArray(R.array.default_phrases_needs).toSet() +
                    context.resources.getStringArray(R.array.default_phrases_social).toSet() +
                    context.resources.getStringArray(R.array.default_phrases_care).toSet() +
                    context.resources.getStringArray(R.array.default_phrases_general).toSet()
                } catch (e: Exception) {
                    emptySet()
                }
                val customCount = phrases.count { it.text !in defaultTexts }
                UsabilityTracker.recordSettingsState(context, voiceSettings, customCount)
            }
        )
    }

    // Export Backup Configuration Dialog
    if (showExportDialog) {
        val tree = remember { whyFinderRepository.getTree() }
        val logs = remember { whyFinderRepository.getLogs() }
        ExportBackupDialog(
            phraseCount = phrases.size,
            areaCount = tree.areas.size,
            logCount = logs.size,
            onConfirmExport = { expPhrases, expTree, expLog ->
                pendingExportOptions = Triple(expPhrases, expTree, expLog)
                showExportDialog = false
                val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                exportLauncher.launch("sotto_backup_$dateStr.json")
            },
            onShareQR = { expPhrases, _, _ ->
                if (expPhrases) {
                    val compactJson = PhraseBackupHelper.exportCompactPhrasesForQR(phrases, effectiveCategories)
                    showExportDialog = false
                    activeQRShareString = compactJson
                    UsabilityTracker.recordActivation(context, "qr_deck_exported")
                }
            },
            onDismiss = { showExportDialog = false }
        )
    }

    // One-Time Install / Upgrade Usability Metrics Consent Dialog
    if (showMetricsOptInDialog) {
        AlertDialog(
            onDismissRequest = {
                showMetricsOptInDialog = false
                onMetricsPromptAnswered(false)
            },
            title = {
                Text(
                    text = stringResource(R.string.dialog_metrics_optin_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_metrics_optin_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMetricsOptInDialog = false
                        onMetricsPromptAnswered(true)
                    }
                ) {
                    Text(stringResource(R.string.action_help_improve))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMetricsOptInDialog = false
                        onMetricsPromptAnswered(false)
                    }
                ) {
                    Text(stringResource(R.string.action_not_now))
                }
            }
        )
    }

    // One-Time First-Launch Role Selection Dialog
    if (showRolePromptDialog && !showMetricsOptInDialog) {
        AlertDialog(
            onDismissRequest = {
                showRolePromptDialog = false
                onRolePromptAnswered("")
            },
            title = {
                Text(
                    text = stringResource(R.string.dialog_role_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dialog_role_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val roleOptions = listOf(
                        VoiceSettings.ROLE_CAREGIVER to stringResource(R.string.role_caregiver),
                        VoiceSettings.ROLE_SELF to stringResource(R.string.role_self),
                        VoiceSettings.ROLE_PROFESSIONAL to stringResource(R.string.role_professional)
                    )
                    roleOptions.forEach { (roleKey, roleLabel) ->
                        OutlinedButton(
                            onClick = {
                                showRolePromptDialog = false
                                onRolePromptAnswered(roleKey)
                                UsabilityTracker.recordRoleSet(context, roleKey)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(roleLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showRolePromptDialog = false
                        onRolePromptAnswered("")
                    }
                ) {
                    Text(stringResource(R.string.action_skip))
                }
            }
        )
    }

    // 14-Day Product-Market Fit Survey Dialog
    if (showPmfSurveyDialog && !showMetricsOptInDialog && !showRolePromptDialog && !showTwoWayDialog && pendingBackupData == null) {
        PmfSurveyDialog(
            initialRole = voiceSettings.userRole,
            onDismiss = {
                showPmfSurveyDialog = false
                onPmfSurveyDismissed()
            },
            onSubmit = { score, benefit, role ->
                showPmfSurveyDialog = false
                onPmfSurveyAnswered(score, benefit, role)
            }
        )
    }

    // Import Backup Confirmation Dialog
    pendingBackupData?.let { backup ->
        ImportBackupDialog(
            backupData = backup,
            onMerge = {
                val result = onImportBackup(backup, false, whyFinderRepository)
                pendingBackupData = null
                val message = if (result.phraseCount > 0) {
                    String.format(Locale.getDefault(), importSuccessTemplate, result.phraseCount)
                } else if (result.treeImported) {
                    importTreeSuccessMsg
                } else {
                    String.format(Locale.getDefault(), importSuccessTemplate, 0)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            onReplace = {
                val result = onImportBackup(backup, true, whyFinderRepository)
                pendingBackupData = null
                val message = if (result.phraseCount > 0) {
                    String.format(Locale.getDefault(), importSuccessTemplate, result.phraseCount)
                } else if (result.treeImported) {
                    importTreeSuccessMsg
                } else {
                    String.format(Locale.getDefault(), importSuccessTemplate, 0)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { pendingBackupData = null }
        )
    }

    // QR Code Scanner Dialog
    if (showQRScannerDialog) {
        QRScannerDialog(
            onScan = { scannedData ->
                showQRScannerDialog = false
                try {
                    val backup = PhraseBackupHelper.parseBackupJson(scannedData)
                    pendingBackupData = backup
                    UsabilityTracker.recordActivation(context, "qr_deck_imported")
                } catch (_: Exception) {
                    Toast.makeText(context, R.string.error_import_failed, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showQRScannerDialog = false }
        )
    }

    // Offline QR Code Display Dialog
    if (activeQRShareString != null) {
        QRCodeDialog(
            dataString = activeQRShareString!!,
            onDismiss = { activeQRShareString = null }
        )
    }

    // Audio Permission Rationale Dialog
    if (showAudioRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showAudioRationaleDialog = false },
            title = { Text(stringResource(R.string.permission_audio_title)) },
            text = { Text(stringResource(R.string.permission_audio_desc)) },
            confirmButton = {
                Button(onClick = {
                    showAudioRationaleDialog = false
                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text(stringResource(R.string.action_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioRationaleDialog = false }) {
                    Text(stringResource(R.string.action_not_now))
                }
            }
        )
    }

    // Missing Offline Speech Model Dialog
    if (showMissingOfflineSpeechDialog) {
        val targetLangName = missingOfflineLanguageName.ifBlank {
            LocaleHelper.getLanguageDisplayName(LocaleHelper.getEffectiveLanguage(context))
        }
        AlertDialog(
            onDismissRequest = { showMissingOfflineSpeechDialog = false },
            title = { Text(stringResource(R.string.dialog_title_offline_speech_missing)) },
            text = { Text(stringResource(R.string.dialog_desc_offline_speech_missing, targetLangName)) },
            confirmButton = {
                Button(onClick = {
                    showMissingOfflineSpeechDialog = false
                    SpeechRecognitionHelper.openOfflineSpeechSettings(context)
                }) {
                    Text(stringResource(R.string.action_open_voice_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMissingOfflineSpeechDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Two-Way Caregiver & Receptive Communication Mode Dialog
    if (showTwoWayDialog) {
        TwoWayConversationDialog(
            allPhrases = phrases,
            isListening = isListeningTwoWay,
            liveSpokenText = liveSpokenTextTwoWay,
            onStartListening = { startListeningWithPermission() },
            onStopListening = { stopTwoWayListening() },
            onClearText = { liveSpokenTextTwoWay = "" },
            onSpeakResponse = { ttsText ->
                UsabilityTracker.endIntent(context, "TwoWay", outcome = "completed")
                onSpeakText(ttsText, LocaleHelper.LANG_AUTO)
            },
            categories = effectiveCategories,
            customCategories = effectiveCategories,
            whyFinderRepository = whyFinderRepository,
            initialTab = partnerModeInitialTab,
            onDismiss = {
                UsabilityTracker.endIntent(context, "TwoWay", outcome = "aborted", frictionTag = "dismissed")
                stopTwoWayListening()
                liveSpokenTextTwoWay = ""
                showTwoWayDialog = false
                partnerModeInitialTab = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val prefs = context.getSharedPreferences("sotto_partner_prefs", Context.MODE_PRIVATE)
                    val promptShown = prefs.getBoolean("qs_tile_prompt_shown", false)
                    val tileAdded = prefs.getBoolean("qs_tile_added", false)
                    if (!promptShown && !tileAdded) {
                        showQsTileDiscoveryDialog = true
                    }
                }
            }
        )
    }

    // Quick Settings Tile Discovery Dialog (API 33+)
    if (showQsTileDiscoveryDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AlertDialog(
            onDismissRequest = {
                val prefs = context.getSharedPreferences("sotto_partner_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("qs_tile_prompt_shown", true).apply()
                showQsTileDiscoveryDialog = false
            },
            title = {
                Text(
                    text = stringResource(R.string.prompt_add_qs_tile_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.prompt_add_qs_tile_msg),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prefs = context.getSharedPreferences("sotto_partner_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("qs_tile_prompt_shown", true).apply()
                        showQsTileDiscoveryDialog = false
                        try {
                            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                            val component = ComponentName(context, EmergencyTileService::class.java)
                            statusBarManager?.requestAddTileService(
                                component,
                                context.getString(R.string.tile_partner_mode),
                                android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_qs_sotto_partner),
                                context.mainExecutor
                            ) { statusCode ->
                                if (statusCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                    prefs.edit().putBoolean("qs_tile_added", true).apply()
                                    UsabilityTracker.recordActivation(context, "qs_tile_added")
                                }
                            }
                        } catch (_: Exception) { }
                    }
                ) {
                    Text(stringResource(R.string.action_add_tile))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val prefs = context.getSharedPreferences("sotto_partner_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("qs_tile_prompt_shown", true).apply()
                        showQsTileDiscoveryDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_not_now))
                }
            }
        )
    }

    // Private Why Log Dialog
    if (showWhyLogDialog) {
        WhyLogDialog(
            repository = whyFinderRepository,
            onDismiss = { showWhyLogDialog = false }
        )
    }

    // Why Tree Editor Dialog
    if (showWhyTreeEditorDialog) {
        WhyTreeEditorDialog(
            repository = whyFinderRepository,
            onDismiss = { showWhyTreeEditorDialog = false }
        )
    }

    // Modal Dialog for viewing/speaking giant text
    expandedPhrase?.let { phrase ->
        val isEmergency = phrase.isEmergency
        val hasSpokenText = voiceSettings.showLanguageSwitcher &&
            !phrase.spokenText.isNullOrBlank() &&
            phrase.spokenText != LocaleHelper.LANG_AUTO

        LaunchedEffect(phrase.text, voiceSettings.showLanguageSwitcher, voiceSettings.secondaryLanguage) {
            if (voiceSettings.showLanguageSwitcher && (phrase.spokenText.isNullOrBlank() || phrase.spokenText == LocaleHelper.LANG_AUTO)) {
                val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                val sourceLang = LocaleHelper.resolvePhraseLocale(phrase.language, phrase.text, effectiveLang).language
                val targetLang = voiceSettings.secondaryLanguage
                TranslationHelper.translate(
                    text = phrase.text,
                    sourceLangCode = sourceLang,
                    targetLangCode = targetLang,
                    onProgress = {},
                    onSuccess = { translated ->
                        if (expandedPhrase?.text == phrase.text) {
                            expandedPhrase = expandedPhrase?.copy(spokenText = translated, spokenLanguage = targetLang)
                        }
                    },
                    onError = {}
                )
            }
        }

        Dialog(
            onDismissRequest = { expandedPhrase = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isEmergency) SottoColors.EmergencyContainer else MaterialTheme.colorScheme.background,
                border = if (isEmergency) BorderStroke(2.dp, SottoColors.EmergencyAccent) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isEmergency) {
                            Surface(
                                color = SottoColors.EmergencyTranslucent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Text(
                                    text = "🛡️ " + stringResource(R.string.emergency_badge),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SottoColors.EmergencyAccent,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { expandedPhrase = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEmergency) SottoColors.EmergencyAccent else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight(Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = phrase.text,
                            fontSize = if (isEmergency && phrase.text.length > 60) 36.sp else 48.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isEmergency && phrase.text.length > 60) 44.sp else 56.sp,
                            color = if (isEmergency) SottoColors.EmergencyText else MaterialTheme.colorScheme.onBackground
                        )
                        if (hasSpokenText) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🗣️", fontSize = 20.sp)
                                    Text(
                                        text = phrase.spokenText,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    if (hasSpokenText) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onSpeakText(phrase.text, phrase.language) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = if (isEmergency) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = SottoColors.EmergencyAccent,
                                        contentColor = SottoColors.EmergencyContainer
                                    )
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                val targetLocale = LocaleHelper.resolvePhraseLocale(phrase.language, phrase.text, LocaleHelper.getEffectiveLanguage(context))
                                val langLabel = targetLocale.getDisplayLanguage(targetLocale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(targetLocale) else it.toString() }
                                Text(
                                    text = stringResource(R.string.action_speak_primary, langLabel),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { onSpeakText(phrase.spokenText, phrase.spokenLanguage ?: voiceSettings.secondaryLanguage) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                val spokenLang = phrase.spokenLanguage?.takeIf { it != LocaleHelper.LANG_AUTO } ?: voiceSettings.secondaryLanguage
                                val targetLocale = LocaleHelper.resolvePhraseLocale(spokenLang, phrase.spokenText, LocaleHelper.getEffectiveLanguage(context))
                                val langLabel = targetLocale.getDisplayLanguage(targetLocale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(targetLocale) else it.toString() }
                                Text(
                                    text = stringResource(R.string.action_speak_secondary, langLabel),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { onSpeak(phrase) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            colors = if (isEmergency) {
                                ButtonDefaults.buttonColors(
                                    containerColor = SottoColors.EmergencyAccent,
                                    contentColor = SottoColors.EmergencyContainer
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Text(
                                stringResource(R.string.action_speak_aloud),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Dialog with Voice Input and Category selection
    if (showAddDialog || phraseToEdit != null) {
        val context = LocalContext.current
        val voiceInputPrompt = stringResource(R.string.cd_voice_input)
        var textValue by remember(phraseToEdit, showAddDialog) { mutableStateOf(phraseToEdit?.text ?: initialAddText) }
        var spokenTextValue by remember(phraseToEdit, showAddDialog) { mutableStateOf(phraseToEdit?.spokenText ?: "") }
        var spokenLangValue by remember(phraseToEdit, showAddDialog) { mutableStateOf(phraseToEdit?.spokenLanguage ?: LocaleHelper.LANG_AUTO) }
        var isSpokenTextExpanded by remember(phraseToEdit, showAddDialog) { mutableStateOf(!phraseToEdit?.spokenText.isNullOrBlank()) }
        var isTranslating by remember { mutableStateOf(false) }
        var isModelDownloading by remember { mutableStateOf(false) }
        var showModelDownloadConfirmDialog by remember { mutableStateOf(false) }
        var translationErrorResId by remember { mutableStateOf<Int?>(null) }
        var selectedCat by remember(phraseToEdit, showAddDialog) {
            mutableStateOf(
                if (phraseToEdit?.isEmergency == true) Phrase.CATEGORY_EMERGENCY
                else (phraseToEdit?.category ?: if (selectedCategory != allCategoryKey) selectedCategory else Phrase.CATEGORY_GENERAL)
            )
        }
        val isEditModeDialog = phraseToEdit != null
        val isDuplicateText = remember(textValue, phraseToEdit, phrases) {
            val trimmed = textValue.trim()
            trimmed.isNotBlank() && phrases.any { p ->
                p.text.trim().equals(trimmed, ignoreCase = true) &&
                    (phraseToEdit == null || !p.text.trim().equals(phraseToEdit?.text?.trim(), ignoreCase = true))
            }
        }

        fun executeTranslation(targetLang: String, sourceLang: String) {
            translationErrorResId = null
            TranslationHelper.translate(
                text = textValue.trim(),
                sourceLangCode = sourceLang,
                targetLangCode = targetLang,
                onProgress = { isTranslating = it },
                onSuccess = { translated ->
                    spokenTextValue = translated
                    spokenLangValue = targetLang
                    isModelDownloading = false
                },
                onError = {
                    isModelDownloading = false
                    translationErrorResId = R.string.error_translation_failed
                }
            )
        }

        if (showModelDownloadConfirmDialog) {
            val targetLangName = LocaleHelper.getLanguageDisplayName(voiceSettings.secondaryLanguage)
            AlertDialog(
                onDismissRequest = { showModelDownloadConfirmDialog = false },
                title = { Text(stringResource(R.string.dialog_download_model_title)) },
                text = {
                    Text(stringResource(R.string.dialog_download_model_message, targetLangName))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showModelDownloadConfirmDialog = false
                            isModelDownloading = true
                            val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                            val sourceLang = LocaleHelper.resolvePhraseLocale(LocaleHelper.LANG_AUTO, textValue, effectiveLang).language
                            val targetLang = voiceSettings.secondaryLanguage
                            executeTranslation(targetLang, sourceLang)
                        }
                    ) {
                        Text(stringResource(R.string.action_download_and_translate))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showModelDownloadConfirmDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        val speechLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!matches.isNullOrEmpty()) {
                    textValue = if (textValue.isBlank()) matches[0] else "$textValue ${matches[0]}"
                }
            }
        }

        val spokenSpeechLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!matches.isNullOrEmpty()) {
                    spokenTextValue = if (spokenTextValue.isBlank()) matches[0] else "$spokenTextValue ${matches[0]}"
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                phraseToEdit = null
                initialAddText = ""
            },
            title = {
                Text(
                    if (isEditModeDialog) {
                        stringResource(R.string.dialog_edit_phrase_title)
                    } else {
                        stringResource(R.string.dialog_add_phrase_title)
                    }
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Phrase Text Field with Voice Input (Microphone button)
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = { Text(stringResource(R.string.label_phrase_text)) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                                    val langTag = LocaleHelper.getLocaleForLanguage(effectiveLang).toLanguageTag()
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, voiceInputPrompt)
                                    }
                                    try {
                                        speechLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        // Ignore if speech recognizer not present
                                    }
                                }
                            ) {
                                Text("🎤", fontSize = 20.sp)
                            }
                        },
                        isError = isDuplicateText,
                        supportingText = if (isDuplicateText) {
                            { Text(stringResource(R.string.error_phrase_exists), color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2
                    )

                    if (voiceSettings.showLanguageSwitcher) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Expandable Alternate Spoken Text
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isSpokenTextExpanded = !isSpokenTextExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🗣️", fontSize = 14.sp)
                                Text(
                                    text = stringResource(R.string.label_spoken_text),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = if (isSpokenTextExpanded) "▲" else "▼",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (isSpokenTextExpanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.label_spoken_language) + ": ${LocaleHelper.getLanguageDisplayName(voiceSettings.secondaryLanguage)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (textValue.isNotBlank() && !isTranslating && !isModelDownloading) {
                                            translationErrorResId = null
                                            val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                                            val sourceLang = LocaleHelper.resolvePhraseLocale(LocaleHelper.LANG_AUTO, textValue, effectiveLang).language
                                            val targetLang = voiceSettings.secondaryLanguage
                                            TranslationHelper.isModelDownloaded(targetLang) { isDownloaded ->
                                                if (isDownloaded) {
                                                    executeTranslation(targetLang, sourceLang)
                                                } else {
                                                    showModelDownloadConfirmDialog = true
                                                }
                                            }
                                        }
                                    },
                                    enabled = textValue.isNotBlank() && !isTranslating && !isModelDownloading,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    if (isTranslating || isModelDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isModelDownloading) stringResource(R.string.status_downloading_model) else stringResource(R.string.status_translating),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    } else {
                                        Text(
                                            text = "🌐 " + stringResource(R.string.action_auto_translate),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }

                            if (translationErrorResId != null) {
                                Text(
                                    text = stringResource(translationErrorResId!!),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SottoColors.DestructiveRed,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            OutlinedTextField(
                                value = spokenTextValue,
                                onValueChange = { spokenTextValue = it },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.hint_spoken_text),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val targetLang = if (spokenLangValue != LocaleHelper.LANG_AUTO) spokenLangValue else voiceSettings.secondaryLanguage
                                            val langTag = LocaleHelper.getLocaleForLanguage(targetLang).toLanguageTag()
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, voiceInputPrompt)
                                            }
                                            try {
                                                spokenSpeechLauncher.launch(intent)
                                            } catch (e: Exception) {
                                                // Ignore if speech recognizer not present
                                            }
                                        }
                                    ) {
                                        Text("🎤", fontSize = 20.sp)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false,
                                minLines = 2
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Selection
                    Text(
                        text = stringResource(R.string.label_category),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        effectiveCategories.forEach { catName ->
                            val isSelected = selectedCat == catName
                            val isEmergencyCat = catName.equals(Phrase.CATEGORY_EMERGENCY, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCat = catName },
                                leadingIcon = if (isEmergencyCat) {
                                    {
                                        Text(
                                            text = "🛡️",
                                            fontSize = 12.sp
                                        )
                                    }
                                } else null,
                                label = {
                                    Text(
                                        text = getCategoryDisplayName(catName),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isEmergencyCat) SottoColors.EmergencyChipBackground else MaterialTheme.colorScheme.surfaceVariant,
                                    selectedLabelColor = if (isEmergencyCat) SottoColors.EmergencyTextSecondary else MaterialTheme.colorScheme.onSurface
                                ),
                                border = if (isEmergencyCat && isSelected) {
                                    BorderStroke(1.dp, SottoColors.EmergencyAccent)
                                } else null
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isDuplicateText) {
                            UsabilityTracker.recordFriction(context, "duplicate_phrase_blocked")
                            return@Button
                        }
                        if (textValue.isNotBlank()) {
                            val isEmergency = selectedCat == Phrase.CATEGORY_EMERGENCY
                            val newPhrase = Phrase(
                                text = textValue.trim(),
                                spokenText = if (voiceSettings.showLanguageSwitcher) spokenTextValue.trim().takeIf { it.isNotBlank() } else phraseToEdit?.spokenText,
                                spokenLanguage = if (voiceSettings.showLanguageSwitcher) spokenLangValue.takeIf { spokenTextValue.isNotBlank() } else phraseToEdit?.spokenLanguage,
                                language = LocaleHelper.LANG_AUTO,
                                isEmergency = isEmergency,
                                category = selectedCat
                            )
                            if (isEditModeDialog) {
                                onEditPhrase(phraseToEdit!!, newPhrase)
                            } else {
                                onAddPhrase(newPhrase)
                                if (initialAddText.isNotBlank()) {
                                    quickText = ""
                                }
                            }
                            if (newPhrase.isEmergency) {
                                UsabilityTracker.recordSafetyCardSaved(context)
                            }
                        }
                        showAddDialog = false
                        phraseToEdit = null
                        initialAddText = ""
                    },
                    enabled = textValue.isNotBlank() && !isDuplicateText
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isEditModeDialog) {
                        TextButton(onClick = {
                            onDeletePhrase(phraseToEdit!!)
                            showAddDialog = false
                            phraseToEdit = null
                            initialAddText = ""
                        }) {
                            Text(stringResource(R.string.action_delete), color = SottoColors.DestructiveRed)
                        }
                    }
                    TextButton(onClick = {
                        showAddDialog = false
                        phraseToEdit = null
                        initialAddText = ""
                    }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        var catInput by rememberSaveable { mutableStateOf("") }
        var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
        val reservedError = stringResource(R.string.error_category_reserved)
        val existsError = stringResource(R.string.error_category_exists)

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text(stringResource(R.string.dialog_title_add_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = catInput,
                        onValueChange = {
                            catInput = it
                            errorMessage = null
                        },
                        placeholder = { Text(stringResource(R.string.hint_category_name)) },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = if (errorMessage != null) {
                            { Text(errorMessage!!, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = catInput.trim()
                        if (trimmed.isBlank()) return@Button
                        if (trimmed.equals(allCategoryKey, ignoreCase = true) || Phrase.isUnrenamableCategory(trimmed)) {
                            errorMessage = reservedError
                            return@Button
                        }
                        if (effectiveCategories.any { it.equals(trimmed, ignoreCase = true) }) {
                            errorMessage = existsError
                            return@Button
                        }
                        if (onAddCategory(trimmed) || onAddCustomCategory(trimmed)) {
                            selectedCategory = trimmed
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Edit/Manage Category Dialog
    if (categoryToManage != null) {
        val oldName = categoryToManage!!
        var catInput by rememberSaveable(oldName) { mutableStateOf(oldName) }
        var errorMessage by rememberSaveable(oldName) { mutableStateOf<String?>(null) }
        val reservedError = stringResource(R.string.error_category_reserved)
        val existsError = stringResource(R.string.error_category_exists)

        AlertDialog(
            onDismissRequest = { categoryToManage = null },
            title = { Text(stringResource(R.string.dialog_title_edit_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = catInput,
                        onValueChange = {
                            catInput = it
                            errorMessage = null
                        },
                        placeholder = { Text(stringResource(R.string.hint_category_name)) },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = if (errorMessage != null) {
                            { Text(errorMessage!!, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = catInput.trim()
                        if (trimmed.isBlank()) return@Button
                        if (trimmed.equals(oldName, ignoreCase = true)) {
                            categoryToManage = null
                            return@Button
                        }
                        if (trimmed.equals(allCategoryKey, ignoreCase = true) || Phrase.isUnrenamableCategory(trimmed)) {
                            errorMessage = reservedError
                            return@Button
                        }
                        if (effectiveCategories.any { it.equals(trimmed, ignoreCase = true) && !it.equals(oldName, ignoreCase = true) }) {
                            errorMessage = existsError
                            return@Button
                        }
                        if (onRenameCategory(oldName, trimmed) || onRenameCustomCategory(oldName, trimmed)) {
                            if (selectedCategory == oldName) {
                                selectedCategory = trimmed
                            }
                            categoryToManage = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_rename_category))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!Phrase.isUndeletableCategory(oldName)) {
                        TextButton(
                            onClick = {
                                val toDelete = oldName
                                categoryToManage = null
                                categoryToDeleteConfirm = toDelete
                            }
                        ) {
                            Text(stringResource(R.string.action_delete_category), color = SottoColors.DestructiveRed)
                        }
                    }
                    TextButton(onClick = { categoryToManage = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
        )
    }

    // Delete Category Confirmation Dialog
    if (categoryToDeleteConfirm != null) {
        val targetCat = categoryToDeleteConfirm!!
        val displayName = getCategoryDisplayName(targetCat)
        AlertDialog(
            onDismissRequest = { categoryToDeleteConfirm = null },
            title = { Text(stringResource(R.string.dialog_title_delete_category)) },
            text = {
                Text(stringResource(R.string.dialog_message_delete_category, displayName))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(targetCat)
                        onDeleteCustomCategory(targetCat)
                        if (selectedCategory == targetCat) {
                            selectedCategory = allCategoryKey
                        }
                        categoryToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.action_delete_category))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDeleteConfirm = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Manage Categories Dialog (Option B)
    if (showManageCategoriesDialog) {
        ManageCategoriesDialog(
            categories = effectiveCategories,
            onDismiss = { showManageCategoriesDialog = false },
            onReorderCategories = onReorderCategories,
            onAddCategory = { showAddCategoryDialog = true },
            onRenameCategory = { catName -> categoryToManage = catName },
            onDeleteCategory = { catName -> categoryToDeleteConfirm = catName }
        )
    }
}
