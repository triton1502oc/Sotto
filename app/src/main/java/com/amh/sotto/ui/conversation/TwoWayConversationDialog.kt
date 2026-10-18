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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
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
    whyFinderRepository: WhyFinderRepository? = null,
    initialTab: CaregiverTab? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val effectiveCategories = categories

    val partnerPrefs = remember(context) {
        context.getSharedPreferences("sotto_partner_prefs", Context.MODE_PRIVATE)
    }

    var activeTab by rememberSaveable {
        mutableStateOf(
            initialTab ?: run {
                val saved = partnerPrefs.getString("saved_partner_tab", null)
                if (saved != null) {
                    try {
                        CaregiverTab.valueOf(saved)
                    } catch (e: Exception) {
                        CaregiverTab.FREEFORM
                    }
                } else {
                    CaregiverTab.FREEFORM
                }
            }
        )
    }

    fun selectTab(tab: CaregiverTab) {
        if (isListening) onStopListening()
        activeTab = tab
        partnerPrefs.edit().putString("saved_partner_tab", tab.name).apply()
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

    // Keep screen continuously illuminated during crisis sessions (#68)
    val view = LocalView.current
    DisposableEffect(view, activity) {
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        dialogWindow?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onStopListening()
        }
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

    var savedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    val whySessionSaved = savedSessionId == whySession.sessionId

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
            savedSessionId = whySession.sessionId
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

    val launchTime = remember { SystemClock.elapsedRealtime() }
    var hasLoggedExit by remember { mutableStateOf(false) }
    var hasRecordedFirstAnswer by rememberSaveable { mutableStateOf(false) }
    var autoSpeakQuestions by rememberSaveable {
        mutableStateOf(partnerPrefs.getBoolean("auto_speak_questions", true))
    }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    fun dismissPartnerMode(outcome: String) {
        onStopListening()
        if (!hasLoggedExit) {
            hasLoggedExit = true
            UsabilityTracker.recordPartnerExit(
                context,
                outcome,
                SystemClock.elapsedRealtime() - launchTime
            )
        }
        onDismiss()
    }

    fun handleDismissAttempt() {
        val isActiveGuided = activeTab == CaregiverTab.WHY_FINDER &&
            whyState !is WhyFinderState.Finished &&
            (whySession.steps.isNotEmpty() || whySession.canStepBack())
        if (isActiveGuided) {
            showExitConfirmDialog = true
        } else {
            val outcome = if (whyState is WhyFinderState.Finished) "completed" else "dismissed"
            dismissPartnerMode(outcome)
        }
    }

    LaunchedEffect(whyPromptText, autoSpeakQuestions, activeTab) {
        if (activeTab == CaregiverTab.WHY_FINDER && autoSpeakQuestions && whyState !is WhyFinderState.Finished && whyPromptText.isNotBlank()) {
            onSpeakResponse(whyPromptText)
        }
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

    fun handleUserResponse(displayText: String, ttsText: String, responseOutcome: String? = null) {
        vibrateFeedback()
        lastReply = displayText
        if (responseOutcome != null) {
            UsabilityTracker.startIntent("partner_response")
            UsabilityTracker.endIntent(context, "partner_response", outcome = responseOutcome)
            UsabilityTracker.recordPartnerResponse(context, responseOutcome)
        }
        if (!hasRecordedFirstAnswer) {
            hasRecordedFirstAnswer = true
            val timeToFirstAnswer = (SystemClock.elapsedRealtime() - launchTime).coerceAtLeast(0L)
            UsabilityTracker.recordPartnerTimeToFirstAnswer(
                context = context,
                durationMs = timeToFirstAnswer,
                outcome = responseOutcome ?: "answered",
                tab = activeTab.name.lowercase(Locale.ROOT)
            )
        }
        onSpeakResponse(ttsText)
    }

    val flipCardRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "flipCardRotation"
    )

    Dialog(
        onDismissRequest = {
            handleDismissAttempt()
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
                            .graphicsLayer { rotationZ = flipCardRotation }
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
                                    savedSessionId = null
                                    UsabilityTracker.recordWhyFinderAction(context, "restart")
                                },
                                onFinish = { dismissPartnerMode("completed") },
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
                                    handleUserResponse(strYes, strYes, "yes")
                                    if (activeTab == CaregiverTab.WHY_FINDER) whySession.answerYes()
                                },
                                onNo = {
                                    handleUserResponse(strNo, strNo, "no")
                                    if (activeTab == CaregiverTab.WHY_FINDER) whySession.answerNo()
                                },
                                onWait = {
                                    handleUserResponse(strWait, ttsWait, "wait")
                                },
                                onNotSure = {
                                    handleUserResponse(strNotSure, strNotSure, "not_sure")
                                    if (activeTab == CaregiverTab.WHY_FINDER) whySession.answerNotSure()
                                },
                                onStop = {
                                    handleUserResponse(strStop, strStop, "stop")
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

                    CaregiverMirrorHUD(
                        activeTab = activeTab,
                        whyState = whyState,
                        currentPrompt = if (activeTab == CaregiverTab.WHY_FINDER) whyPromptText else activeFreeformText,
                        lastReply = lastReply,
                        onRestartWhyTree = {
                            whySession.restart()
                            savedSessionId = null
                            UsabilityTracker.recordWhyFinderAction(context, "restart")
                        },
                        onDone = {
                            val outcome = if (whyState is WhyFinderState.Finished) "completed" else "dismissed"
                            dismissPartnerMode(outcome)
                        },
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    CaregiverBottomMenu(
                        activeTab = activeTab,
                        onTabChange = { selectTab(it) },
                        hasWhyFinder = whyFinderRepository != null,
                        isFlipped = true,
                        onToggleFlip = {
                            if (isListening) onStopListening()
                            isFlipped = false
                            UsabilityTracker.startIntent("partner_flip")
                            UsabilityTracker.endIntent(context, "partner_flip", outcome = "unflipped")
                        },
                        onToggleTriggerLogger = { showPostResolutionPrompt = !showPostResolutionPrompt },
                        canSpeakPrompt = whyPromptText.isNotBlank(),
                        onSpeakPrompt = { onSpeakResponse(whyPromptText) },
                        onPickCard = { showQuestionsSheet = true },
                        canStepBack = whySession.canStepBack(),
                        onStepBack = {
                            val wasFinished = whyState is WhyFinderState.Finished
                            whySession.stepBack()
                            savedSessionId = null
                            if (wasFinished) {
                                UsabilityTracker.recordWhyFinderAction(context, "correction")
                            }
                        },
                        autoSpeakQuestions = autoSpeakQuestions,
                        onToggleAutoSpeak = {
                            autoSpeakQuestions = !autoSpeakQuestions
                            partnerPrefs.edit().putBoolean("auto_speak_questions", autoSpeakQuestions).apply()
                        },
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
                            handleDismissAttempt()
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
                            savedSessionId = null
                            UsabilityTracker.recordWhyFinderAction(context, "restart")
                        },
                        onFinish = { dismissPartnerMode("completed") },
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
                            handleUserResponse(strYes, strYes, "yes")
                            if (activeTab == CaregiverTab.WHY_FINDER) whySession.answerYes()
                        },
                        onNo = {
                            handleUserResponse(strNo, strNo, "no")
                            if (activeTab == CaregiverTab.WHY_FINDER) whySession.answerNo()
                        },
                        onWait = {
                            handleUserResponse(strWait, ttsWait, "wait")
                        },
                        onNotSure = {
                            handleUserResponse(strNotSure, strNotSure, "not_sure")
                            if (activeTab == CaregiverTab.WHY_FINDER) whySession.answerNotSure()
                        },
                        onStop = {
                            handleUserResponse(strStop, strStop, "stop")
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
                        onTabChange = { selectTab(it) },
                        hasWhyFinder = whyFinderRepository != null,
                        isFlipped = false,
                        onToggleFlip = {
                            if (isListening) onStopListening()
                            isFlipped = true
                            UsabilityTracker.startIntent("partner_flip")
                            UsabilityTracker.endIntent(context, "partner_flip", outcome = "flipped")
                        },
                        onToggleTriggerLogger = { showPostResolutionPrompt = !showPostResolutionPrompt },
                        canSpeakPrompt = whyPromptText.isNotBlank(),
                        onSpeakPrompt = { onSpeakResponse(whyPromptText) },
                        onPickCard = { showQuestionsSheet = true },
                        canStepBack = whySession.canStepBack(),
                        onStepBack = {
                            val wasFinished = whyState is WhyFinderState.Finished
                            whySession.stepBack()
                            savedSessionId = null
                            if (wasFinished) {
                                UsabilityTracker.recordWhyFinderAction(context, "correction")
                            }
                        },
                        autoSpeakQuestions = autoSpeakQuestions,
                        onToggleAutoSpeak = {
                            autoSpeakQuestions = !autoSpeakQuestions
                            partnerPrefs.edit().putBoolean("auto_speak_questions", autoSpeakQuestions).apply()
                        },
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
                            handleDismissAttempt()
                        }
                    )
                }
            }
        }
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text(stringResource(R.string.title_exit_partner_mode)) },
            text = { Text(stringResource(R.string.msg_exit_partner_mode_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        whySession.stop()
                        dismissPartnerMode("cancelled")
                    }
                ) {
                    Text(stringResource(R.string.action_exit), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text(stringResource(R.string.action_continue))
                }
            }
        )
    }

    // Card Picker Sheet (Why Tree Questions in Guided mode or Phrase Cards in Talk mode)
    if (showQuestionsSheet) {
        var selectedPromptCategory by rememberSaveable(activeTab) {
            val initialCat = if (activeTab == CaregiverTab.FREEFORM) {
                val savedCat = partnerPrefs.getString("saved_freeform_prompt_category", "ALL") ?: "ALL"
                if (savedCat == "ALL" || savedCat == "CAREGIVER_QUESTIONS" || effectiveCategories.contains(savedCat)) {
                    savedCat
                } else {
                    "ALL"
                }
            } else {
                "ALL"
            }
            mutableStateOf(initialCat)
        }

        fun selectFreeformCategory(cat: String) {
            selectedPromptCategory = cat
            partnerPrefs.edit().putString("saved_freeform_prompt_category", cat).apply()
        }

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
                        // Curated board categories/decks + Dedicated 2nd-person caregiver prompts (#79)
                        val freeformQuestions = remember(context) {
                            try {
                                context.resources.getStringArray(R.array.caregiver_questions_freeform).toList()
                            } catch (e: Exception) {
                                listOf(
                                    "Are you in pain?",
                                    "Do you need water or food?",
                                    "Do you need the restroom?",
                                    "Are you feeling cold or warm?",
                                    "Do you want to rest or sleep?",
                                    "Do you need a quiet space?",
                                    "Would you like more time?",
                                    "Do you want to leave now?"
                                )
                            }
                        }

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
                                    onClick = { selectFreeformCategory("ALL") },
                                    label = {
                                        Text(
                                            text = stringResource(R.string.category_all),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                            item {
                                val isSelected = selectedPromptCategory == "CAREGIVER_QUESTIONS"
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectFreeformCategory("CAREGIVER_QUESTIONS") },
                                    label = {
                                        Text(
                                            text = "💬 " + stringResource(R.string.action_caregiver_prompts),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                            items(effectiveCategories) { catName ->
                                val isSelected = selectedPromptCategory == catName
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectFreeformCategory(catName) },
                                    label = {
                                        Text(
                                            text = getCategoryDisplayName(catName),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        val availablePrompts = remember(allPhrases, freeformQuestions, selectedPromptCategory) {
                            when (selectedPromptCategory) {
                                "ALL" -> (allPhrases.map { it.text } + freeformQuestions).distinct()
                                "CAREGIVER_QUESTIONS" -> freeformQuestions
                                Phrase.CATEGORY_EMERGENCY -> allPhrases.filter { it.isEmergency }.map { it.text }
                                else -> allPhrases.filter { !it.isEmergency && it.category == selectedPromptCategory }.map { it.text }
                            }
                        }

                        if (availablePrompts.isEmpty()) {
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
                                items(availablePrompts) { question ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                UsabilityTracker.recordActivation(context, "partner_freeform_question_picked")
                                                displayedText = question
                                                showQuestionsSheet = false
                                            },
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = question,
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
private fun CaregiverMirrorHUD(
    activeTab: CaregiverTab,
    whyState: WhyFinderState,
    currentPrompt: String,
    lastReply: String?,
    onRestartWhyTree: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        if (activeTab == CaregiverTab.WHY_FINDER && whyState is WhyFinderState.Finished) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val outcomeLabel = if (whyState.outcome == WhyOutcome.FOUND) {
                    stringResource(R.string.why_finder_found_title) + (whyState.causeText?.let { ": $it" } ?: "")
                } else if (whyState.outcome == WhyOutcome.STOPPED) {
                    stringResource(R.string.why_finder_stopped_title)
                } else {
                    stringResource(R.string.why_finder_not_found_title)
                }
                Text(
                    text = outcomeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onRestartWhyTree,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(stringResource(R.string.action_restart), style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = onDone,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(stringResource(R.string.action_done), style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(if (activeTab == CaregiverTab.WHY_FINDER) R.drawable.ic_help else R.drawable.ic_partner_mode),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentPrompt.ifBlank { stringResource(R.string.listen_mode_hint) },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (lastReply != null) {
                    Surface(
                        color = SottoColors.SuccessGreen,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = lastReply,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

