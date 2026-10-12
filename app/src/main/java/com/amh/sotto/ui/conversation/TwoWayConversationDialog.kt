package com.amh.sotto.ui.conversation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import com.amh.sotto.data.SharedPreferencesWhyFinderRepository
import com.amh.sotto.data.WhyFinderRepository
import com.amh.sotto.data.WhyLogEntry
import com.amh.sotto.data.WhyOutcome
import com.amh.sotto.data.WhyStep
import com.amh.sotto.data.WhyTree
import com.amh.sotto.theme.SottoColors
import com.amh.sotto.ui.category.getCategoryDisplayName
import com.amh.sotto.ui.whyfinder.BodyPartSelectionGrid
import com.amh.sotto.ui.whyfinder.IntensityScaleRow
import com.amh.sotto.ui.whyfinder.WhyFinderSession
import com.amh.sotto.ui.whyfinder.WhyFinderState
import com.amh.sotto.ui.whyfinder.getBodyRegionLabel
import com.amh.sotto.ui.whyfinder.getAreaDisplayName
import com.amh.sotto.util.UsabilityTracker
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.UUID

enum class CaregiverTab {
    WHY_FINDER,
    FREEFORM
}

private data class WhyQuestionOption(
    val text: String,
    val isAreaHeader: Boolean,
    val areaIndex: Int,
    val questionIndex: Int = -1
)

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
        mutableStateOf(CaregiverTab.FREEFORM)
    }

    var displayedText by rememberSaveable { mutableStateOf("") }
    var isFlipped by rememberSaveable { mutableStateOf(false) }

    val activity = remember(context) {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return@remember current
            current = current.baseContext
        }
        null
    }

    val configuration = LocalConfiguration.current
    val isTabletOrFoldable = configuration.smallestScreenWidthDp >= 600

    DisposableEffect(isFlipped) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (isFlipped) {
            if (!isTabletOrFoldable) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            }
            UsabilityTracker.recordActivation(context, "partner_mode_flip")
        }
        onDispose {
            if (isFlipped && !isTabletOrFoldable) {
                activity?.requestedOrientation = originalOrientation
            }
        }
    }

    var lastReply by remember { mutableStateOf<String?>(null) }
    var showQuestionsSheet by rememberSaveable { mutableStateOf(false) }
    var showKeyboardInput by rememberSaveable { mutableStateOf(false) }
    var manualInputText by rememberSaveable { mutableStateOf("") }

    // Why Finder session state
    val tree = remember { whyFinderRepository?.getTree() ?: WhyTree(emptyList()) }
    val whySession = remember { WhyFinderSession(tree) }
    val whyState = whySession.currentState

    var whySessionSaved by rememberSaveable { mutableStateOf(false) }

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

    // Auto-save completed Why Finder session
    fun finishAndSaveWhySession() {
        if (whySessionSaved) return
        val entry = whySession.toLogEntry(null)
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
            finishAndSaveWhySession()
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

    var showPostResolutionPrompt by rememberSaveable { mutableStateOf(false) }
    var triggerLoggedFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(triggerLoggedFeedback) {
        if (triggerLoggedFeedback != null) {
            delay(2500)
            triggerLoggedFeedback = null
        }
    }

    fun logPostResolutionTrigger(triggerKey: String) {
        val areaId = when (triggerKey) {
            "Pain" -> SharedPreferencesWhyFinderRepository.AREA_BODY
            "Noise" -> SharedPreferencesWhyFinderRepository.AREA_SENSES
            "Task" -> SharedPreferencesWhyFinderRepository.AREA_HAPPENED
            "Emotion" -> SharedPreferencesWhyFinderRepository.AREA_FEELINGS
            else -> "other"
        }
        val now = System.currentTimeMillis()
        val entry = WhyLogEntry(
            id = UUID.randomUUID().toString(),
            startedAt = now - 3000L,
            endedAt = now,
            outcome = WhyOutcome.FOUND,
            areaId = areaId,
            causeText = triggerKey,
            steps = listOf(
                WhyStep(
                    questionText = activeFreeformText.ifBlank { "Direct Yes/No Check-in" },
                    answer = "yes"
                )
            )
        )
        whyFinderRepository?.addLog(entry)
        UsabilityTracker.recordActivation(context, "why_log_quick_trigger")
        UsabilityTracker.recordWhyFinder(
            context = context,
            outcome = "found",
            areaId = areaId,
            depth = 1,
            notSureCount = 0,
            bodyPart = if (triggerKey == "Pain") "pain" else null,
            intensity = null,
            durationMs = 3000L
        )
        showPostResolutionPrompt = false
        triggerLoggedFeedback = triggerKey
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

    fun handleUserResponse(displayText: String, ttsText: String) {
        vibrateFeedback()
        lastReply = displayText
        if (activeTab == CaregiverTab.FREEFORM && (displayText == strYes || displayText == strNo)) {
            showPostResolutionPrompt = true
        }
        onSpeakResponse(ttsText)
    }

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
                    // Top: Rotated 180 degrees to face the person across the table
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
                            CaregiverDisplayCard(
                                activeTab = activeTab,
                                whyState = whyState,
                                whyPromptText = whyPromptText,
                                activeFreeformText = activeFreeformText,
                                lastReply = lastReply,
                                isListening = isListening,
                                onRestartWhyTree = {
                                    whySession.restart()
                                    whySessionSaved = false
                                },
                                onFinish = onDismiss,
                                onReadAloudPrompt = {
                                    val promptToSpeak = if (activeTab == CaregiverTab.WHY_FINDER) whyPromptText else activeFreeformText
                                    if (promptToSpeak.isNotBlank()) {
                                        onSpeakResponse(promptToSpeak)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            CaregiverResponseDock(
                                activeTab = activeTab,
                                whyState = whyState,
                                onYes = {
                                    handleUserResponse(strYes, strYes)
                                    if (activeTab == CaregiverTab.WHY_FINDER) whySession.answerYes()
                                },
                                onNo = {
                                    handleUserResponse(strNo, strNo)
                                    if (activeTab == CaregiverTab.WHY_FINDER) whySession.answerNo()
                                },
                                onWait = {
                                    handleUserResponse(strWait, ttsWait)
                                },
                                onRepeat = {
                                    handleUserResponse(strRepeat, ttsRepeat)
                                },
                                onNotSure = {
                                    handleUserResponse(strNotSure, strNotSure)
                                    if (activeTab == CaregiverTab.WHY_FINDER) whySession.answerNotSure()
                                },
                                onStop = {
                                    handleUserResponse(strStop, strStop)
                                    if (activeTab == CaregiverTab.WHY_FINDER) whySession.stop()
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom: Caregiver controls facing caregiver
                    AnimatedVisibility(visible = triggerLoggedFeedback != null && activeTab == CaregiverTab.FREEFORM) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SottoColors.SuccessGreen.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, SottoColors.SuccessGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.msg_trigger_logged, triggerLoggedFeedback ?: ""),
                                color = SottoColors.SuccessText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showPostResolutionPrompt && activeTab == CaregiverTab.FREEFORM) {
                        PostResolutionTriggerLogger(
                            onLogTrigger = { triggerKey -> logPostResolutionTrigger(triggerKey) },
                            onDismiss = { showPostResolutionPrompt = false },
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

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
                            },
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    CaregiverBottomMenu(
                        activeTab = activeTab,
                        onTabChange = { activeTab = it },
                        hasWhyFinder = whyFinderRepository != null,
                        isFlipped = true,
                        onToggleFlip = { isFlipped = false },
                        onToggleTriggerLogger = { showPostResolutionPrompt = !showPostResolutionPrompt },
                        canSpeakPrompt = whyPromptText.isNotBlank(),
                        onSpeakPrompt = { onSpeakResponse(whyPromptText) },
                        onPickCard = { showQuestionsSheet = true },
                        canStepBack = whySession.canStepBack(),
                        onStepBack = { whySession.stepBack() },
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
                        canReadAloudFreeform = activeFreeformText.isNotBlank(),
                        onReadAloudFreeform = { onSpeakResponse(activeFreeformText) },
                        onToggleKeyboard = { showKeyboardInput = !showKeyboardInput },
                        onDismiss = {
                            onStopListening()
                            onDismiss()
                        }
                    )
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: Receptive display card
                    CaregiverDisplayCard(
                        activeTab = activeTab,
                        whyState = whyState,
                        whyPromptText = whyPromptText,
                        activeFreeformText = activeFreeformText,
                        lastReply = lastReply,
                        isListening = isListening,
                        onRestartWhyTree = {
                            whySession.restart()
                            whySessionSaved = false
                        },
                        onFinish = onDismiss,
                        onReadAloudPrompt = {
                            val promptToSpeak = if (activeTab == CaregiverTab.WHY_FINDER) whyPromptText else activeFreeformText
                            if (promptToSpeak.isNotBlank()) {
                                onSpeakResponse(promptToSpeak)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 8.dp)
                    )

                    // Middle: 4 Response Cards (Yes, No, Not Sure, Stop)
                    CaregiverResponseDock(
                        activeTab = activeTab,
                        whyState = whyState,
                        onYes = {
                            handleUserResponse(strYes, strYes)
                            if (activeTab == CaregiverTab.WHY_FINDER) whySession.answerYes()
                        },
                        onNo = {
                            handleUserResponse(strNo, strNo)
                            if (activeTab == CaregiverTab.WHY_FINDER) whySession.answerNo()
                        },
                        onWait = {
                            handleUserResponse(strWait, ttsWait)
                        },
                        onRepeat = {
                            handleUserResponse(strRepeat, ttsRepeat)
                        },
                        onNotSure = {
                            handleUserResponse(strNotSure, strNotSure)
                            if (activeTab == CaregiverTab.WHY_FINDER) whySession.answerNotSure()
                        },
                        onStop = {
                            handleUserResponse(strStop, strStop)
                            if (activeTab == CaregiverTab.WHY_FINDER) whySession.stop()
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom: Caregiver 2-line menu
                    AnimatedVisibility(visible = triggerLoggedFeedback != null && activeTab == CaregiverTab.FREEFORM) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SottoColors.SuccessGreen.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, SottoColors.SuccessGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.msg_trigger_logged, triggerLoggedFeedback ?: ""),
                                color = SottoColors.SuccessText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showPostResolutionPrompt && activeTab == CaregiverTab.FREEFORM) {
                        PostResolutionTriggerLogger(
                            onLogTrigger = { triggerKey -> logPostResolutionTrigger(triggerKey) },
                            onDismiss = { showPostResolutionPrompt = false },
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

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
                            },
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    CaregiverBottomMenu(
                        activeTab = activeTab,
                        onTabChange = { activeTab = it },
                        hasWhyFinder = whyFinderRepository != null,
                        isFlipped = false,
                        onToggleFlip = { isFlipped = true },
                        onToggleTriggerLogger = { showPostResolutionPrompt = !showPostResolutionPrompt },
                        canSpeakPrompt = whyPromptText.isNotBlank(),
                        onSpeakPrompt = { onSpeakResponse(whyPromptText) },
                        onPickCard = { showQuestionsSheet = true },
                        canStepBack = whySession.canStepBack(),
                        onStepBack = { whySession.stepBack() },
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
                        canReadAloudFreeform = activeFreeformText.isNotBlank(),
                        onReadAloudFreeform = { onSpeakResponse(activeFreeformText) },
                        onToggleKeyboard = { showKeyboardInput = !showKeyboardInput },
                        onDismiss = {
                            onStopListening()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }

    // Card Picker Sheet (Why Tree Questions in Guided mode or Phrase Cards in Talk mode)
    if (showQuestionsSheet) {
        var selectedPromptCategory by rememberSaveable(activeTab) { mutableStateOf("ALL") }

        AlertDialog(
            onDismissRequest = { showQuestionsSheet = false },
            title = {
                Text(
                    text = if (activeTab == CaregiverTab.WHY_FINDER) {
                        "🌳 " + stringResource(R.string.why_tree_editor_title)
                    } else {
                        stringResource(R.string.action_caregiver_prompts)
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (activeTab == CaregiverTab.WHY_FINDER) {
                        // Why Tree Area Categories
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
                            items(tree.areas) { area ->
                                val isSelected = selectedPromptCategory == area.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPromptCategory = area.id },
                                    label = {
                                        Text(
                                            text = getAreaDisplayName(area.id, context),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        val availableWhyQuestions = remember(tree, selectedPromptCategory) {
                            if (selectedPromptCategory == "ALL") {
                                tree.areas.flatMapIndexed { aIdx, area ->
                                    listOf(WhyQuestionOption(area.question, isAreaHeader = true, areaIndex = aIdx)) +
                                    area.questions.mapIndexed { qIdx, q ->
                                        WhyQuestionOption(q.text, isAreaHeader = false, areaIndex = aIdx, questionIndex = qIdx)
                                    }
                                }
                            } else {
                                val aIdx = tree.areas.indexOfFirst { it.id == selectedPromptCategory }
                                if (aIdx >= 0) {
                                    val area = tree.areas[aIdx]
                                    listOf(WhyQuestionOption(area.question, isAreaHeader = true, areaIndex = aIdx)) +
                                    area.questions.mapIndexed { qIdx, q ->
                                        WhyQuestionOption(q.text, isAreaHeader = false, areaIndex = aIdx, questionIndex = qIdx)
                                    }
                                } else {
                                    emptyList()
                                }
                            }
                        }

                        if (availableWhyQuestions.isEmpty()) {
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
                                items(availableWhyQuestions) { option ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (option.isAreaHeader) {
                                                    whySession.jumpToArea(option.areaIndex)
                                                } else {
                                                    whySession.jumpToQuestion(option.areaIndex, option.questionIndex)
                                                }
                                                showQuestionsSheet = false
                                            },
                                        color = if (option.isAreaHeader) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = (if (option.isAreaHeader) "🏷️ " else "") + option.text,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (option.isAreaHeader) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                            color = if (option.isAreaHeader) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard AAC Phrases (Talk Mode)
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
                                                displayedText = phrase.text
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
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuestionsSheet = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

}

@Composable
private fun CaregiverBottomMenu(
    activeTab: CaregiverTab,
    onTabChange: (CaregiverTab) -> Unit,
    hasWhyFinder: Boolean,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit,
    canSpeakPrompt: Boolean,
    onSpeakPrompt: () -> Unit,
    onPickCard: () -> Unit,
    canStepBack: Boolean,
    onStepBack: () -> Unit,
    isListening: Boolean,
    onToggleListening: () -> Unit,
    canReadAloudFreeform: Boolean,
    onReadAloudFreeform: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onDismiss: () -> Unit,
    onToggleTriggerLogger: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Line 1: Mode Switcher Tabs & Global Controls (Flip & Close)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tabs: Guided / Mic
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                }

                // Global Controls: Material 3 Tool Capsule + Standalone Dismissal (#46)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    val flipRotation by animateFloatAsState(
                        targetValue = if (isFlipped) 180f else 0f,
                        label = "flipIconRotation"
                    )

                    // Utility Capsule: Log (if available) + Flip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            if (activeTab == CaregiverTab.FREEFORM && hasWhyFinder && onToggleTriggerLogger != null) {
                                val strLogTrigger = stringResource(R.string.label_log_trigger)
                                IconButton(
                                    onClick = {
                                        UsabilityTracker.startIntent("partner_tool")
                                        UsabilityTracker.endIntent(context, "partner_tool", outcome = "log")
                                        onToggleTriggerLogger()
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .semantics {
                                            role = Role.Button
                                            contentDescription = strLogTrigger
                                        }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_note_alt),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val flipDesc = if (isFlipped) stringResource(R.string.action_flip_back) else stringResource(R.string.action_flip)
                            val flipBg = if (isFlipped) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            val flipContentColor = if (isFlipped) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(flipBg)
                                    .clickable {
                                        UsabilityTracker.startIntent("partner_tool")
                                        UsabilityTracker.endIntent(context, "partner_tool", outcome = "flip")
                                        onToggleFlip()
                                    }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = flipDesc
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_flip_partner),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer { rotationZ = flipRotation },
                                    tint = flipContentColor
                                )
                            }
                        }
                    }

                    // Standalone Modal Dismissal
                    val dismissDesc = stringResource(R.string.cd_dismiss)
                    IconButton(
                        onClick = {
                            UsabilityTracker.startIntent("partner_tool")
                            UsabilityTracker.endIntent(context, "partner_tool", outcome = "close")
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = dismissDesc
                            }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Line 2: Mode Actions (#50: Asymmetric Dock & Vector Icons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = LocalContext.current
                if (activeTab == CaregiverTab.WHY_FINDER) {
                    FilledTonalButton(
                        onClick = {
                            UsabilityTracker.startIntent("caregiver_action")
                            UsabilityTracker.endIntent(context, "caregiver_action", outcome = "speak")
                            onSpeakPrompt()
                        },
                        enabled = canSpeakPrompt,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_volume_up),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_speak),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            UsabilityTracker.startIntent("caregiver_action")
                            UsabilityTracker.endIntent(context, "caregiver_action", outcome = "cards")
                            onPickCard()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cards),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_cards),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (canStepBack) {
                        OutlinedButton(
                            onClick = onStepBack,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_replay),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_step_back),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    val configuration = LocalConfiguration.current
                    val isCompactScreen = configuration.screenWidthDp <= 360 || configuration.fontScale > 1.15f
                    val speakCd = stringResource(R.string.action_speak)
                    val cardsCd = stringResource(R.string.action_cards)
                    val typeCd = stringResource(R.string.action_type)

                    // Companion 1: Speak
                    FilledTonalButton(
                        onClick = {
                            UsabilityTracker.startIntent("caregiver_action")
                            UsabilityTracker.endIntent(context, "caregiver_action", outcome = "speak")
                            onReadAloudFreeform()
                        },
                        enabled = canReadAloudFreeform,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = if (isCompactScreen) 0.dp else 6.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                role = Role.Button
                                contentDescription = speakCd
                            }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_volume_up),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        if (!isCompactScreen) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = speakCd,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Companion 2: Cards
                    OutlinedButton(
                        onClick = {
                            UsabilityTracker.startIntent("caregiver_action")
                            UsabilityTracker.endIntent(context, "caregiver_action", outcome = "cards")
                            onPickCard()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = if (isCompactScreen) 0.dp else 6.dp, vertical = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                role = Role.Button
                                contentDescription = cardsCd
                            }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cards),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        if (!isCompactScreen) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = cardsCd,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // HERO: Listen / Mic (weight 1.5f)
                    val micColor by animateColorAsState(
                        targetValue = if (isListening) SottoColors.RecordingPulseRed else MaterialTheme.colorScheme.primaryContainer,
                        label = "micBg"
                    )
                    val micContentColor = if (isListening) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    val micLabel = if (isListening) stringResource(R.string.action_stop) else stringResource(R.string.action_mic)

                    FilledTonalButton(
                        onClick = {
                            UsabilityTracker.startIntent("caregiver_action")
                            UsabilityTracker.endIntent(context, "caregiver_action", outcome = "listen")
                            onToggleListening()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = micColor,
                            contentColor = micContentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .semantics {
                                role = Role.Button
                                contentDescription = micLabel
                            }
                    ) {
                        Icon(
                            painter = painterResource(if (isListening) R.drawable.ic_close_bold else R.drawable.ic_mic),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = micLabel,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Companion 3: Type
                    OutlinedButton(
                        onClick = {
                            UsabilityTracker.startIntent("caregiver_action")
                            UsabilityTracker.endIntent(context, "caregiver_action", outcome = "type")
                            onToggleKeyboard()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = if (isCompactScreen) 0.dp else 6.dp, vertical = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                role = Role.Button
                                contentDescription = typeCd
                            }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_keyboard),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        if (!isCompactScreen) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = typeCd,
                                style = MaterialTheme.typography.labelMedium,
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

@Composable
private fun CaregiverDisplayCard(
    activeTab: CaregiverTab,
    whyState: WhyFinderState,
    whyPromptText: String,
    activeFreeformText: String,
    lastReply: String?,
    isListening: Boolean,
    onRestartWhyTree: () -> Unit,
    onFinish: () -> Unit,
    onReadAloudPrompt: () -> Unit,
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
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_done))
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
                    isListening = if (activeTab == CaregiverTab.FREEFORM) isListening else false,
                    onClick = if (prompt.isNotBlank()) onReadAloudPrompt else null
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun UnifiedFourCardResponseDock(
    onYes: () -> Unit,
    onNo: () -> Unit,
    secondaryLeftText: String,
    onSecondaryLeft: () -> Unit,
    secondaryRightText: String,
    onSecondaryRight: () -> Unit,
    secondaryLeftColors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = SottoColors.NeutralDarkContainer,
        contentColor = SottoColors.NeutralDarkText
    ),
    secondaryRightColors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = SottoColors.NeutralDarkContainer,
        contentColor = SottoColors.NeutralDarkText
    ),
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val strYes = stringResource(R.string.response_yes)
    val strNo = stringResource(R.string.response_no)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: YES & NO (Primary Crisis Buttons >= 80dp touch target with tactile haptics)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onYes()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 80.dp)
                    .height(80.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "✓ $strYes"
                    },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SottoColors.SuccessContainer,
                    contentColor = SottoColors.SuccessText
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "✓ $strYes",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNo()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 80.dp)
                    .height(80.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "✕ $strNo"
                    },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SottoColors.CrisisStopContainer,
                    contentColor = SottoColors.CrisisStopText
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "✕ $strNo",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Row 2: Secondary actions (Wait / Repeat in Talk mode vs Not sure / Stop in Guided mode)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSecondaryLeft()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 54.dp)
                    .height(54.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = secondaryLeftText
                    },
                shape = RoundedCornerShape(16.dp),
                colors = secondaryLeftColors
            ) {
                Text(
                    text = secondaryLeftText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSecondaryRight()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 54.dp)
                    .height(54.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = secondaryRightText
                    },
                shape = RoundedCornerShape(16.dp),
                colors = secondaryRightColors
            ) {
                Text(
                    text = secondaryRightText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
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
    onWait: () -> Unit,
    onRepeat: () -> Unit,
    onNotSure: () -> Unit,
    onStop: () -> Unit,
    onSelectBodyPart: (String) -> Unit,
    onSelectIntensity: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strWait = stringResource(R.string.response_wait)
    val strRepeat = stringResource(R.string.response_repeat)
    val strNotSure = stringResource(R.string.why_finder_btn_not_sure)
    val strStop = stringResource(R.string.why_finder_btn_stop)

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
                // Handled in Finished card
            }
            else -> {
                UnifiedFourCardResponseDock(
                    onYes = onYes,
                    onNo = onNo,
                    secondaryLeftText = "? $strNotSure",
                    onSecondaryLeft = onNotSure,
                    secondaryRightText = "⏹ $strStop",
                    onSecondaryRight = onStop,
                    secondaryLeftColors = ButtonDefaults.buttonColors(
                        containerColor = SottoColors.WarningHighContainer,
                        contentColor = SottoColors.WarningHighText
                    ),
                    secondaryRightColors = ButtonDefaults.buttonColors(
                        containerColor = SottoColors.NeutralStoppedContainer,
                        contentColor = SottoColors.NeutralStoppedText
                    ),
                    modifier = modifier
                )
            }
        }
    } else {
        // Freeform / Talk mode: Wait & Repeat
        UnifiedFourCardResponseDock(
            onYes = onYes,
            onNo = onNo,
            secondaryLeftText = "⏳ $strWait",
            onSecondaryLeft = onWait,
            secondaryRightText = "🔁 $strRepeat",
            onSecondaryRight = onRepeat,
            secondaryLeftColors = ButtonDefaults.buttonColors(
                containerColor = SottoColors.NeutralDarkContainer,
                contentColor = SottoColors.NeutralDarkText
            ),
            secondaryRightColors = ButtonDefaults.buttonColors(
                containerColor = SottoColors.NeutralDarkContainer,
                contentColor = SottoColors.NeutralDarkText
            ),
            modifier = modifier
        )
    }
}

@Composable
private fun PostResolutionTriggerLogger(
    onLogTrigger: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝 " + stringResource(R.string.label_log_trigger),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val triggers = listOf(
                    "Pain" to stringResource(R.string.trigger_pain),
                    "Noise" to stringResource(R.string.trigger_noise),
                    "Task" to stringResource(R.string.trigger_task),
                    "Emotion" to stringResource(R.string.trigger_emotion),
                    "Other" to stringResource(R.string.trigger_other)
                )

                triggers.forEach { (key, label) ->
                    FilledTonalButton(
                        onClick = { onLogTrigger(key) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
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
                color = SottoColors.SuccessGreen,
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
                        .background(SottoColors.RecordingDotRed)
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
    isListening: Boolean,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .then(
                if (onClick != null && activeText.isNotBlank()) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClick)
                } else Modifier
            ),
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
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
