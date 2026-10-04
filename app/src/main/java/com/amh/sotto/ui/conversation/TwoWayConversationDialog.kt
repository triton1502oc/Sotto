package com.amh.sotto.ui.conversation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amh.sotto.R
import com.amh.sotto.data.Phrase
import com.amh.sotto.data.WhyFinderRepository
import com.amh.sotto.data.WhyOutcome
import com.amh.sotto.data.WhyTree
import com.amh.sotto.ui.category.getCategoryDisplayName
import com.amh.sotto.ui.whyfinder.BodyPartSelectionGrid
import com.amh.sotto.ui.whyfinder.IntensityScaleRow
import com.amh.sotto.ui.whyfinder.WhyFinderActionDock
import com.amh.sotto.ui.whyfinder.WhyFinderSession
import com.amh.sotto.ui.whyfinder.WhyFinderState
import com.amh.sotto.ui.whyfinder.WhyLogDialog
import com.amh.sotto.ui.whyfinder.WhyTreeEditorDialog
import com.amh.sotto.ui.whyfinder.getBodyRegionLabel
import com.amh.sotto.util.UsabilityTracker
import kotlinx.coroutines.delay
import java.util.Locale

enum class CaregiverTab {
    WHY_FINDER,
    FREEFORM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoWayConversationDialog(
    allPhrases: List<Phrase>,
    isListening: Boolean,
    liveSpokenText: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClearText: () -> Unit = {},
    onSpeakResponse: (spokenText: String) -> Unit,
    categories: List<String> = emptyList(),
    customCategories: List<String> = emptyList(),
    whyFinderRepository: WhyFinderRepository? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val effectiveCategories = if (categories.isNotEmpty()) categories else customCategories

    var activeTab by rememberSaveable {
        mutableStateOf(if (whyFinderRepository != null) CaregiverTab.WHY_FINDER else CaregiverTab.FREEFORM)
    }

    var displayedText by rememberSaveable { mutableStateOf("") }
    var isFlipped by rememberSaveable { mutableStateOf(false) }
    var lastReply by remember { mutableStateOf<String?>(null) }
    var showQuestionsSheet by rememberSaveable { mutableStateOf(false) }
    var showKeyboardInput by rememberSaveable { mutableStateOf(false) }
    var manualInputText by rememberSaveable { mutableStateOf("") }

    // Why Finder session state
    val tree = remember { whyFinderRepository?.getTree() ?: WhyTree(emptyList()) }
    val whySession = remember { WhyFinderSession(tree) }
    val whyState = whySession.currentState

    var showEditQuestionDialog by rememberSaveable { mutableStateOf(false) }
    var editQuestionDraft by rememberSaveable { mutableStateOf("") }
    var whyLogNote by rememberSaveable { mutableStateOf("") }
    var whySessionSaved by rememberSaveable { mutableStateOf(false) }
    var showWhyLogDialog by rememberSaveable { mutableStateOf(false) }
    var showWhyTreeEditorDialog by rememberSaveable { mutableStateOf(false) }

    val activeFreeformText = displayedText.ifBlank { liveSpokenText }

    // Update displayed text whenever speech recognition outputs new words in Freeform mode
    LaunchedEffect(liveSpokenText) {
        if (liveSpokenText.isNotBlank()) {
            displayedText = liveSpokenText
        }
    }

    // Auto-clear reply indicator after 3 seconds
    LaunchedEffect(lastReply) {
        if (lastReply != null) {
            delay(3000)
        }
        lastReply = null
    }

    // Determine active prompt text for Why Finder mode
    val whyPromptText: String = when (whyState) {
        is WhyFinderState.AskingArea -> whyState.area.question
        is WhyFinderState.AskingQuestion -> whyState.question.text
        is WhyFinderState.SelectingBodyPart -> stringResource(R.string.why_finder_body_part_prompt)
        is WhyFinderState.SelectingIntensity -> stringResource(R.string.why_finder_intensity_prompt)
        is WhyFinderState.ConfirmingCause -> {
            val base = stringResource(R.string.why_finder_confirm_title)
            val cause = whyState.causeText
            val details = if (!whyState.bodyPart.isNullOrBlank()) {
                val partLabel = getBodyRegionLabel(whyState.bodyPart, context)
                val intensityStr = whyState.intensity?.let { " ($it/5)" } ?: ""
                " ($partLabel$intensityStr)"
            } else ""
            "$base\n\n$cause$details"
        }
        is WhyFinderState.Finished -> {
            when (whyState.outcome) {
                WhyOutcome.FOUND -> {
                    val base = stringResource(R.string.why_finder_found_title)
                    val cause = whyState.causeText ?: ""
                    val details = if (!whyState.bodyPart.isNullOrBlank()) {
                        val partLabel = getBodyRegionLabel(whyState.bodyPart, context)
                        val intensityStr = whyState.intensity?.let { " ($it/5)" } ?: ""
                        " ($partLabel$intensityStr)"
                    } else ""
                    "$base\n\n$cause$details"
                }
                WhyOutcome.NOT_FOUND -> stringResource(R.string.why_finder_not_found_title)
                WhyOutcome.STOPPED -> stringResource(R.string.why_finder_stopped_title)
            }
        }
    }

    // Auto-read aloud Why Finder prompt when prompt changes
    LaunchedEffect(whyPromptText, activeTab) {
        if (activeTab == CaregiverTab.WHY_FINDER && whyState !is WhyFinderState.Finished && whyPromptText.isNotBlank()) {
            onSpeakResponse(whyPromptText)
        }
    }

    // Auto-save completed Why Finder session
    fun finishAndSaveWhySession(note: String?) {
        if (whySessionSaved) return
        val entry = whySession.toLogEntry(note)
        if (entry != null) {
            whyFinderRepository?.addLog(entry)
            whySessionSaved = true
            UsabilityTracker.recordWhyFinder(
                context = context,
                outcome = entry.outcome.name.lowercase(Locale.ROOT),
                areaId = entry.areaId,
                depth = entry.steps.size,
                notSureCount = entry.steps.count { it.answer == "not_sure" },
                bodyPart = entry.bodyPart,
                intensity = entry.intensity,
                durationMs = entry.endedAt - entry.startedAt
            )
        }
    }

    LaunchedEffect(whyState) {
        if (whyState is WhyFinderState.Finished) {
            finishAndSaveWhySession(whyLogNote)
        }
    }

    fun vibrateFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(70)
            }
        } catch (_: Exception) {}
    }

    fun handleUserResponse(displayText: String, ttsText: String) {
        vibrateFeedback()
        lastReply = displayText
        onSpeakResponse(ttsText)
    }

    // Reusable response strings
    val strYes = stringResource(R.string.response_yes)
    val strNo = stringResource(R.string.response_no)
    val strRepeat = stringResource(R.string.response_repeat)
    val strWait = stringResource(R.string.response_wait)
    val strNotSure = stringResource(R.string.why_finder_btn_not_sure)
    val strStop = stringResource(R.string.why_finder_btn_stop)
    val ttsRepeat = stringResource(R.string.response_tts_repeat)
    val ttsWait = stringResource(R.string.response_tts_wait)

    Dialog(
        onDismissRequest = {
            onStopListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isFlipped) {
                // ==========================================
                // FLIPPED MODE (Face-to-Face Partner View)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top 80%: Patient area rotated 180 degrees to face the person across the table
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .graphicsLayer { rotationZ = 180f }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Receptive display card
                            CaregiverDisplayCard(
                                activeTab = activeTab,
                                whyState = whyState,
                                whyPromptText = whyPromptText,
                                activeFreeformText = activeFreeformText,
                                lastReply = lastReply,
                                isListening = isListening,
                                whyLogNote = whyLogNote,
                                onWhyLogNoteChange = {
                                    whyLogNote = it
                                    finishAndSaveWhySession(it)
                                },
                                onRestartWhyTree = {
                                    whySession.restart()
                                    whySessionSaved = false
                                    whyLogNote = ""
                                },
                                onViewWhyLog = { showWhyLogDialog = true },
                                onEditQuestion = {
                                    editQuestionDraft = whyPromptText
                                    showEditQuestionDialog = true
                                },
                                onPickCard = { showQuestionsSheet = true },
                                onReadAloudPrompt = { onSpeakResponse(whyPromptText) },
                                canStepBack = whySession.canStepBack(),
                                onStepBack = { whySession.stepBack() },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Response Dock facing partner right-side-up
                            CaregiverResponseDock(
                                activeTab = activeTab,
                                whyState = whyState,
                                onYes = {
                                    handleUserResponse(strYes, strYes)
                                    whySession.answerYes()
                                },
                                onNo = {
                                    handleUserResponse(strNo, strNo)
                                    whySession.answerNo()
                                },
                                onRepeat = {
                                    handleUserResponse(strRepeat, ttsRepeat)
                                    val promptToRepeat = if (activeTab == CaregiverTab.WHY_FINDER) whyPromptText else activeFreeformText
                                    if (promptToRepeat.isNotBlank()) onSpeakResponse(promptToRepeat)
                                },
                                onWait = { handleUserResponse(strWait, ttsWait) },
                                onNotSure = {
                                    handleUserResponse(strNotSure, strNotSure)
                                    whySession.answerNotSure()
                                },
                                onStop = {
                                    handleUserResponse(strStop, strStop)
                                    whySession.stop()
                                },
                                onSelectBodyPart = { partKey ->
                                    val label = getBodyRegionLabel(partKey, context)
                                    handleUserResponse(label, label)
                                    whySession.selectBodyPart(partKey)
                                },
                                onSelectIntensity = { level ->
                                    handleUserResponse("$level", "$level")
                                    whySession.selectIntensity(level)
                                }
                            )
                        }
                    }

                    // Bottom: Caregiver controls facing caregiver
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnimatedVisibility(visible = showKeyboardInput && activeTab == CaregiverTab.FREEFORM) {
                            ManualKeyboardInputRow(
                                text = manualInputText,
                                onTextChange = { manualInputText = it },
                                onSubmit = {
                                    if (manualInputText.isNotBlank()) {
                                        displayedText = manualInputText.trim()
                                        manualInputText = ""
                                        showKeyboardInput = false
                                    }
                                }
                            )
                        }

                        CaregiverToolbar(
                            activeTab = activeTab,
                            onTabChange = { activeTab = it },
                            hasWhyFinder = whyFinderRepository != null,
                            isFlipped = true,
                            onToggleFlip = { isFlipped = false },
                            onOpenLog = { showWhyLogDialog = true },
                            onOpenTreeEditor = { showWhyTreeEditorDialog = true },
                            isListening = isListening,
                            onToggleListening = {
                                if (isListening) {
                                    onStopListening()
                                } else {
                                    displayedText = ""
                                    onClearText()
                                    onStartListening()
                                }
                            },
                            canReadAloud = activeFreeformText.isNotBlank(),
                            onReadAloud = { onSpeakResponse(activeFreeformText) },
                            onToggleKeyboard = { showKeyboardInput = !showKeyboardInput },
                            onDismiss = {
                                onStopListening()
                                onDismiss()
                            }
                        )
                    }
                }
            } else {
                // ==========================================
                // NORMAL MODE (Side-by-Side / Standard View)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Action Bar
                    CaregiverToolbar(
                        activeTab = activeTab,
                        onTabChange = { activeTab = it },
                        hasWhyFinder = whyFinderRepository != null,
                        isFlipped = false,
                        onToggleFlip = { isFlipped = true },
                        onOpenLog = { showWhyLogDialog = true },
                        onOpenTreeEditor = { showWhyTreeEditorDialog = true },
                        isListening = isListening,
                        onToggleListening = {
                            if (isListening) {
                                onStopListening()
                            } else {
                                displayedText = ""
                                onClearText()
                                onStartListening()
                            }
                        },
                        canReadAloud = activeFreeformText.isNotBlank(),
                        onReadAloud = { onSpeakResponse(activeFreeformText) },
                        onToggleKeyboard = { showKeyboardInput = !showKeyboardInput },
                        onDismiss = {
                            onStopListening()
                            onDismiss()
                        }
                    )

                    AnimatedVisibility(visible = showKeyboardInput && activeTab == CaregiverTab.FREEFORM) {
                        ManualKeyboardInputRow(
                            text = manualInputText,
                            onTextChange = { manualInputText = it },
                            onSubmit = {
                                if (manualInputText.isNotBlank()) {
                                    displayedText = manualInputText.trim()
                                    manualInputText = ""
                                    showKeyboardInput = false
                                }
                            }
                        )
                    }

                    // Middle: Receptive Display Card
                    CaregiverDisplayCard(
                        activeTab = activeTab,
                        whyState = whyState,
                        whyPromptText = whyPromptText,
                        activeFreeformText = activeFreeformText,
                        lastReply = lastReply,
                        isListening = isListening,
                        whyLogNote = whyLogNote,
                        onWhyLogNoteChange = {
                            whyLogNote = it
                            finishAndSaveWhySession(it)
                        },
                        onRestartWhyTree = {
                            whySession.restart()
                            whySessionSaved = false
                            whyLogNote = ""
                        },
                        onViewWhyLog = { showWhyLogDialog = true },
                        onEditQuestion = {
                            editQuestionDraft = whyPromptText
                            showEditQuestionDialog = true
                        },
                        onPickCard = { showQuestionsSheet = true },
                        onReadAloudPrompt = { onSpeakResponse(whyPromptText) },
                        canStepBack = whySession.canStepBack(),
                        onStepBack = { whySession.stepBack() },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    )

                    // Bottom: User Response Dock
                    CaregiverResponseDock(
                        activeTab = activeTab,
                        whyState = whyState,
                        onYes = {
                            handleUserResponse(strYes, strYes)
                            whySession.answerYes()
                        },
                        onNo = {
                            handleUserResponse(strNo, strNo)
                            whySession.answerNo()
                        },
                        onRepeat = {
                            handleUserResponse(strRepeat, ttsRepeat)
                            val promptToRepeat = if (activeTab == CaregiverTab.WHY_FINDER) whyPromptText else activeFreeformText
                            if (promptToRepeat.isNotBlank()) onSpeakResponse(promptToRepeat)
                        },
                        onWait = { handleUserResponse(strWait, ttsWait) },
                        onNotSure = {
                            handleUserResponse(strNotSure, strNotSure)
                            whySession.answerNotSure()
                        },
                        onStop = {
                            handleUserResponse(strStop, strStop)
                            whySession.stop()
                        },
                        onSelectBodyPart = { partKey ->
                            val label = getBodyRegionLabel(partKey, context)
                            handleUserResponse(label, label)
                            whySession.selectBodyPart(partKey)
                        },
                        onSelectIntensity = { level ->
                            handleUserResponse("$level", "$level")
                            whySession.selectIntensity(level)
                        }
                    )
                }
            }
        }
    }

    // Inline Edit Question Dialog
    if (showEditQuestionDialog) {
        AlertDialog(
            onDismissRequest = { showEditQuestionDialog = false },
            title = { Text(stringResource(R.string.action_edit_question)) },
            text = {
                OutlinedTextField(
                    value = editQuestionDraft,
                    onValueChange = { editQuestionDraft = it },
                    placeholder = { Text(stringResource(R.string.hint_edit_question)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editQuestionDraft.isNotBlank()) {
                        whySession.overrideCurrentQuestion(editQuestionDraft.trim())
                    }
                    showEditQuestionDialog = false
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditQuestionDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Phrase Card Picker Sheet (for selecting cards in Freeform or injecting into Why Finder)
    if (showQuestionsSheet) {
        var selectedPromptCategory by rememberSaveable { mutableStateOf("ALL") }

        AlertDialog(
            onDismissRequest = { showQuestionsSheet = false },
            title = {
                Text(
                    text = stringResource(R.string.action_caregiver_prompts),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            val isSelected = selectedPromptCategory == "ALL"
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPromptCategory = "ALL" },
                                label = {
                                    Text(
                                        text = stringResource(R.string.category_all),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                        items(effectiveCategories) { catName ->
                            val isSelected = selectedPromptCategory == catName
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPromptCategory = catName },
                                label = {
                                    Text(
                                        text = getCategoryDisplayName(catName),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    val availablePhrases = remember(allPhrases, selectedPromptCategory) {
                        if (selectedPromptCategory == "ALL") {
                            allPhrases
                        } else if (selectedPromptCategory == Phrase.CATEGORY_EMERGENCY) {
                            allPhrases.filter { it.isEmergency }
                        } else {
                            allPhrases.filter { !it.isEmergency && it.category == selectedPromptCategory }
                        }
                    }

                    if (availablePhrases.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.hint_no_phrases),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availablePhrases) { phrase ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (activeTab == CaregiverTab.WHY_FINDER) {
                                                whySession.overrideCurrentQuestion(phrase.text)
                                            } else {
                                                displayedText = phrase.text
                                            }
                                            showQuestionsSheet = false
                                        },
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = phrase.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuestionsSheet = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showWhyLogDialog && whyFinderRepository != null) {
        WhyLogDialog(
            repository = whyFinderRepository,
            onDismiss = { showWhyLogDialog = false }
        )
    }

    if (showWhyTreeEditorDialog && whyFinderRepository != null) {
        WhyTreeEditorDialog(
            repository = whyFinderRepository,
            onDismiss = { showWhyTreeEditorDialog = false }
        )
    }
}

@Composable
private fun CaregiverToolbar(
    activeTab: CaregiverTab,
    onTabChange: (CaregiverTab) -> Unit,
    hasWhyFinder: Boolean,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenTreeEditor: () -> Unit,
    isListening: Boolean,
    onToggleListening: () -> Unit,
    canReadAloud: Boolean,
    onReadAloud: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Switcher Tabs
            if (hasWhyFinder) {
                FilterChip(
                    selected = activeTab == CaregiverTab.WHY_FINDER,
                    onClick = { onTabChange(CaregiverTab.WHY_FINDER) },
                    label = {
                        Text(
                            text = "🧭 " + stringResource(R.string.mode_why_finder),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
                FilterChip(
                    selected = activeTab == CaregiverTab.FREEFORM,
                    onClick = { onTabChange(CaregiverTab.FREEFORM) },
                    label = {
                        Text(
                            text = "💬 " + stringResource(R.string.mode_freeform),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            // 180 Flip Button
            OutlinedButton(
                onClick = onToggleFlip,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = if (isFlipped) "🔄 " + stringResource(R.string.action_flip_back) else "🔄 " + stringResource(R.string.action_flip),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Why Log & Tree Editor shortcuts
            if (hasWhyFinder) {
                IconButton(onClick = onOpenLog, modifier = Modifier.size(36.dp)) {
                    Text("📋", fontSize = 17.sp)
                }
                IconButton(onClick = onOpenTreeEditor, modifier = Modifier.size(36.dp)) {
                    Text("🌳", fontSize = 17.sp)
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeTab == CaregiverTab.FREEFORM) {
                val micColor by animateColorAsState(
                    targetValue = if (isListening) Color(0xFFC62828) else MaterialTheme.colorScheme.primaryContainer,
                    label = "micBg"
                )
                FilledTonalButton(
                    onClick = onToggleListening,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = micColor,
                        contentColor = if (isListening) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(if (isListening) "⏹" else "🎤", fontSize = 15.sp)
                }

                if (canReadAloud) {
                    IconButton(onClick = onReadAloud, modifier = Modifier.size(36.dp)) {
                        Text("🔊", fontSize = 17.sp)
                    }
                }

                IconButton(onClick = onToggleKeyboard, modifier = Modifier.size(36.dp)) {
                    Text("⌨️", fontSize = 17.sp)
                }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun CaregiverDisplayCard(
    activeTab: CaregiverTab,
    whyState: WhyFinderState,
    whyPromptText: String,
    activeFreeformText: String,
    lastReply: String?,
    isListening: Boolean,
    whyLogNote: String,
    onWhyLogNoteChange: (String) -> Unit,
    onRestartWhyTree: () -> Unit,
    onViewWhyLog: () -> Unit,
    onEditQuestion: () -> Unit,
    onPickCard: () -> Unit,
    onReadAloudPrompt: () -> Unit,
    canStepBack: Boolean,
    onStepBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        if (activeTab == CaregiverTab.WHY_FINDER && whyState is WhyFinderState.Finished) {
            // Finished Why Finder State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (whyState.outcome == WhyOutcome.FOUND) "🎯 " + stringResource(R.string.why_finder_found_title)
                        else if (whyState.outcome == WhyOutcome.STOPPED) "⏸ " + stringResource(R.string.why_finder_stopped_title)
                        else "🔍 " + stringResource(R.string.why_finder_not_found_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    if (whyState.outcome == WhyOutcome.FOUND && !whyState.causeText.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = whyState.causeText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (!whyState.bodyPart.isNullOrBlank()) {
                                    val context = LocalContext.current
                                    val partLabel = getBodyRegionLabel(whyState.bodyPart, context)
                                    val intensityStr = whyState.intensity?.let { " • Intensity: $it/5" } ?: ""
                                    Text(
                                        text = "$partLabel$intensityStr",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = whyLogNote,
                        onValueChange = onWhyLogNoteChange,
                        placeholder = { Text(stringResource(R.string.why_finder_note_hint), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onRestartWhyTree,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🔄 " + stringResource(R.string.action_restart))
                    }
                    OutlinedButton(
                        onClick = onViewWhyLog,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📋 " + stringResource(R.string.action_view_why_log))
                    }
                }
            }
        } else {
            // Active Prompt State (Why Finder Question or Freeform Prompt)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusBadge(lastReply = lastReply, isListening = isListening)

                val prompt = if (activeTab == CaregiverTab.WHY_FINDER) whyPromptText else activeFreeformText
                GiantPromptDisplay(
                    activeText = prompt,
                    isListening = if (activeTab == CaregiverTab.FREEFORM) isListening else false
                )

                if (activeTab == CaregiverTab.WHY_FINDER) {
                    // Why Finder prompt helper actions (Edit, Pick card, Read aloud, Back)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onEditQuestion,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text("✏️ " + stringResource(R.string.action_edit_question), fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedButton(
                            onClick = onPickCard,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text("📋 " + stringResource(R.string.action_caregiver_prompts), fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(onClick = onReadAloudPrompt, modifier = Modifier.size(34.dp)) {
                            Text("🔊", fontSize = 16.sp)
                        }

                        if (canStepBack) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = onStepBack, modifier = Modifier.size(34.dp)) {
                                Text("↩️", fontSize = 16.sp)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun CaregiverResponseDock(
    activeTab: CaregiverTab,
    whyState: WhyFinderState,
    onYes: () -> Unit,
    onNo: () -> Unit,
    onRepeat: () -> Unit,
    onWait: () -> Unit,
    onNotSure: () -> Unit,
    onStop: () -> Unit,
    onSelectBodyPart: (String) -> Unit,
    onSelectIntensity: (Int) -> Unit
) {
    if (activeTab == CaregiverTab.WHY_FINDER) {
        when (whyState) {
            is WhyFinderState.SelectingBodyPart -> {
                BodyPartSelectionGrid(
                    onSelectPart = onSelectBodyPart,
                    onNotSure = onNotSure,
                    onStop = onStop
                )
            }
            is WhyFinderState.SelectingIntensity -> {
                IntensityScaleRow(
                    onSelectLevel = onSelectIntensity,
                    onNotSure = onNotSure,
                    onStop = onStop
                )
            }
            is WhyFinderState.Finished -> {
                // Finished card has its own buttons
            }
            else -> {
                WhyFinderActionDock(
                    onYes = onYes,
                    onNo = onNo,
                    onNotSure = onNotSure,
                    onStop = onStop,
                    onRepeat = onRepeat,
                    onWait = onWait
                )
            }
        }
    } else {
        // Freeform mode uses 4-button dock
        val yesTts = stringResource(R.string.response_yes)
        val noTts = stringResource(R.string.response_no)
        val repeatTts = stringResource(R.string.response_tts_repeat)
        UserResponseDock(
            onResponse = { _, tts ->
                when (tts) {
                    yesTts -> onYes()
                    noTts -> onNo()
                    repeatTts -> onRepeat()
                    else -> onWait()
                }
            }
        )
    }
}

@Composable
private fun StatusBadge(
    lastReply: String?,
    isListening: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lastReply != null) {
            Surface(
                color = Color(0xFF2E7D32),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.replied_indicator, lastReply),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        } else if (isListening) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                )
                Text(
                    text = stringResource(R.string.listen_mode_listening),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun GiantPromptDisplay(
    activeText: String,
    isListening: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (activeText.isNotBlank()) {
            val textLen = activeText.length
            val fontSize = when {
                textLen < 30 -> 34.sp
                textLen < 70 -> 28.sp
                textLen < 140 -> 22.sp
                else -> 18.sp
            }
            Text(
                text = activeText,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = fontSize * 1.3f,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (isListening) {
            Text(
                text = stringResource(R.string.listen_mode_listening),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = stringResource(R.string.listen_mode_hint),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ManualKeyboardInputRow(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(stringResource(R.string.hint_quick_speak), fontSize = 13.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Button(onClick = onSubmit) {
            Text("✓")
        }
    }
}

@Composable
private fun UserResponseDock(
    onResponse: (displayText: String, ttsText: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strYes = stringResource(R.string.response_yes)
    val strNo = stringResource(R.string.response_no)
    val strRepeat = stringResource(R.string.response_repeat)
    val strWait = stringResource(R.string.response_wait)

    val ttsRepeat = stringResource(R.string.response_tts_repeat)
    val ttsWait = stringResource(R.string.response_tts_wait)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: YES & NO (Large primary targets)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onResponse(strYes, strYes) },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B5E20),
                    contentColor = Color(0xFFE8F5E9)
                )
            ) {
                Text(
                    text = "✓ $strYes",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { onResponse(strNo, strNo) },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7F1D1D),
                    contentColor = Color(0xFFFFEBEE)
                )
            ) {
                Text(
                    text = "✕ $strNo",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Row 2: REPEAT & WAIT (Comfortable, spacious touch targets)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = { onResponse(strRepeat, ttsRepeat) },
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "🔁 $strRepeat",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            FilledTonalButton(
                onClick = { onResponse(strWait, ttsWait) },
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "⏳ $strWait",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
