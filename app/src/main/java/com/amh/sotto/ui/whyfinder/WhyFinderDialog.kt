package com.amh.sotto.ui.whyfinder

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amh.sotto.R
import com.amh.sotto.data.BodyRegions
import com.amh.sotto.data.WhyFinderRepository
import com.amh.sotto.data.WhyOutcome
import com.amh.sotto.theme.SottoColors
import com.amh.sotto.util.UsabilityTracker
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhyFinderDialog(
    repository: WhyFinderRepository,
    onSpeakText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tree = remember { repository.getTree() }
    val session = remember { WhyFinderSession(tree) }

    var isFlipped by rememberSaveable { mutableStateOf(false) }
    var privateNote by rememberSaveable { mutableStateOf("") }
    var sessionSaved by rememberSaveable { mutableStateOf(false) }
    var showWhyLog by rememberSaveable { mutableStateOf(false) }

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
                vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(60)
            }
        } catch (_: Exception) {}
    }

    val currentState = session.currentState

    val activeDisplayPrompt: String = when (currentState) {
        is WhyFinderState.AskingArea -> currentState.area.question
        is WhyFinderState.AskingQuestion -> currentState.question.text
        is WhyFinderState.SelectingBodyPart -> stringResource(R.string.why_finder_body_part_prompt)
        is WhyFinderState.SelectingIntensity -> stringResource(R.string.why_finder_intensity_prompt)
        is WhyFinderState.ConfirmingCause -> {
            val base = stringResource(R.string.why_finder_confirm_title)
            val cause = currentState.causeText
            val details = if (!currentState.bodyPart.isNullOrBlank()) {
                val partLabel = getBodyRegionLabel(currentState.bodyPart, context)
                val intensityStr = currentState.intensity?.let { " ($it/5)" } ?: ""
                " ($partLabel$intensityStr)"
            } else ""
            "$base\n\n$cause$details"
        }
        is WhyFinderState.Finished -> {
            when (currentState.outcome) {
                WhyOutcome.FOUND -> {
                    val base = stringResource(R.string.why_finder_found_title)
                    val cause = currentState.causeText ?: ""
                    val details = if (!currentState.bodyPart.isNullOrBlank()) {
                        val partLabel = getBodyRegionLabel(currentState.bodyPart, context)
                        val intensityStr = currentState.intensity?.let { " ($it/5)" } ?: ""
                        " ($partLabel$intensityStr)"
                    } else ""
                    "$base\n\n$cause$details"
                }
                WhyOutcome.NOT_FOUND -> stringResource(R.string.why_finder_not_found_title)
                WhyOutcome.STOPPED -> stringResource(R.string.why_finder_stopped_title)
            }
        }
    }

    val waitTtsResponse = stringResource(R.string.response_tts_wait)

    // Auto-read aloud when prompt changes if not in finished state
    LaunchedEffect(activeDisplayPrompt) {
        if (currentState !is WhyFinderState.Finished && activeDisplayPrompt.isNotBlank()) {
            onSpeakText(activeDisplayPrompt)
        }
    }

    fun finishAndSave(note: String?) {
        if (!sessionSaved) {
            sessionSaved = true
            val entry = session.toLogEntry(note)
            if (entry != null) {
                repository.addLog(entry)
                UsabilityTracker.recordWhyFinder(
                    context = context,
                    outcome = entry.outcome.name.lowercase(Locale.ROOT),
                    areaId = entry.areaId,
                    depth = entry.steps.size,
                    notSureCount = entry.steps.count { it.answer == "not_sure" },
                    bodyPart = entry.bodyPart,
                    intensity = entry.intensity,
                    durationMs = (entry.endedAt - entry.startedAt).coerceAtLeast(0L)
                )
            }
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = {
            if (currentState is WhyFinderState.Finished) {
                finishAndSave(privateNote)
            } else {
                session.stop()
                finishAndSave(null)
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Receptive & Display Card (Flippable for partner across table)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .then(if (isFlipped) Modifier.graphicsLayer { rotationZ = 180f } else Modifier)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Giant Prompt Display Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val textLen = activeDisplayPrompt.length
                                val fontSize = when {
                                    textLen < 35 -> 32.sp
                                    textLen < 80 -> 26.sp
                                    textLen < 150 -> 22.sp
                                    else -> 18.sp
                                }
                                Text(
                                    text = activeDisplayPrompt,
                                    fontSize = fontSize,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = fontSize * 1.3f,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (currentState is WhyFinderState.Finished) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = privateNote,
                                        onValueChange = { privateNote = it },
                                        placeholder = { Text(stringResource(R.string.why_finder_note_hint), fontSize = 13.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 3
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { finishAndSave(privateNote) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(stringResource(R.string.action_save_log))
                                        }
                                        OutlinedButton(
                                            onClick = { finishAndSave(null) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(stringResource(R.string.action_skip_note))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interaction Dock according to current session step
                        when (currentState) {
                            is WhyFinderState.AskingArea,
                            is WhyFinderState.AskingQuestion,
                            is WhyFinderState.ConfirmingCause -> {
                                WhyFinderActionDock(
                                    onYes = {
                                        vibrateFeedback()
                                        session.answerYes()
                                    },
                                    onNo = {
                                        vibrateFeedback()
                                        session.answerNo()
                                    },
                                    onNotSure = {
                                        vibrateFeedback()
                                        session.answerNotSure()
                                    },
                                    onStop = {
                                        vibrateFeedback()
                                        session.stop()
                                    },
                                    onRepeat = {
                                        vibrateFeedback()
                                        onSpeakText(activeDisplayPrompt)
                                    },
                                    onWait = {
                                        vibrateFeedback()
                                        onSpeakText(waitTtsResponse)
                                    }
                                )
                            }
                            is WhyFinderState.SelectingBodyPart -> {
                                BodyPartSelectionGrid(
                                    onSelectPart = { part ->
                                        vibrateFeedback()
                                        session.selectBodyPart(part)
                                    },
                                    onNotSure = {
                                        vibrateFeedback()
                                        session.answerNotSure()
                                    },
                                    onStop = {
                                        vibrateFeedback()
                                        session.stop()
                                    }
                                )
                            }
                            is WhyFinderState.SelectingIntensity -> {
                                IntensityScaleRow(
                                    onSelectLevel = { level ->
                                        vibrateFeedback()
                                        session.selectIntensity(level)
                                    },
                                    onNotSure = {
                                        vibrateFeedback()
                                        session.answerNotSure()
                                    },
                                    onStop = {
                                        vibrateFeedback()
                                        session.stop()
                                    }
                                )
                            }
                            is WhyFinderState.Finished -> {
                                // Handled directly inside scrollable card
                            }
                        }
                    }
                }

                // Caregiver Controls (Always faces the caregiver holding the phone)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { isFlipped = !isFlipped },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isFlipped) "🔄 " + stringResource(R.string.action_flip_back) else "🔄 " + stringResource(R.string.action_flip),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        IconButton(onClick = { onSpeakText(activeDisplayPrompt) }) {
                            Text("🔊", fontSize = 18.sp)
                        }
                        IconButton(onClick = { showWhyLog = true }) {
                            Text("📋", fontSize = 18.sp)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentState is WhyFinderState.Finished) {
                                    finishAndSave(privateNote)
                                } else {
                                    session.stop()
                                    finishAndSave(null)
                                }
                            }
                        ) {
                            Text("✕", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }

    if (showWhyLog) {
        WhyLogDialog(
            repository = repository,
            onDismiss = { showWhyLog = false }
        )
    }
}

@Composable
internal fun WhyFinderActionDock(
    onYes: () -> Unit,
    onNo: () -> Unit,
    onNotSure: () -> Unit,
    onStop: () -> Unit,
    onRepeat: () -> Unit,
    onWait: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: YES & NO
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onYes,
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SottoColors.SuccessContainer,
                    contentColor = SottoColors.SuccessText
                )
            ) {
                Text(
                    text = "✓ " + stringResource(R.string.response_yes),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onNo,
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SottoColors.CrisisStopContainer,
                    contentColor = SottoColors.CrisisStopText
                )
            ) {
                Text(
                    text = "✕ " + stringResource(R.string.response_no),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Row 2: NOT SURE & STOP
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onNotSure,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = "? " + stringResource(R.string.why_finder_btn_not_sure),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SottoColors.NeutralDarkContainer,
                    contentColor = SottoColors.NeutralDarkText
                )
            ) {
                Text(
                    text = "⏹ " + stringResource(R.string.why_finder_btn_stop),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Row 3: REPEAT & WAIT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onRepeat,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "🔁 " + stringResource(R.string.response_repeat),
                    fontSize = 14.sp
                )
            }

            FilledTonalButton(
                onClick = onWait,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "⏸ " + stringResource(R.string.response_wait),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
internal fun BodyPartSelectionGrid(
    onSelectPart: (String) -> Unit,
    onNotSure: () -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(BodyRegions.ALL) { regionKey ->
                Surface(
                    onClick = { onSelectPart(regionKey) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getBodyRegionLabel(regionKey, context),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onNotSure,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.why_finder_btn_not_sure))
            }
            Button(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SottoColors.NeutralDarkContainer)
            ) {
                Text(stringResource(R.string.why_finder_btn_stop))
            }
        }
    }
}

@Composable
internal fun IntensityScaleRow(
    onSelectLevel: (Int) -> Unit,
    onNotSure: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val colors = listOf(
                SottoColors.IntensityMild,
                SottoColors.IntensityLight,
                SottoColors.IntensityModerate,
                SottoColors.IntensityHigh,
                SottoColors.IntensitySevere
            )
            for (level in 1..5) {
                Button(
                    onClick = { onSelectLevel(level) },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors[level - 1])
                ) {
                    Text(
                        text = "$level",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onNotSure,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.why_finder_btn_not_sure))
            }
            Button(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SottoColors.NeutralDarkContainer)
            ) {
                Text(stringResource(R.string.why_finder_btn_stop))
            }
        }
    }
}

fun getBodyRegionLabel(regionKey: String, context: Context): String {
    return when (regionKey) {
        BodyRegions.HEAD -> context.getString(R.string.body_region_head)
        BodyRegions.EYES -> context.getString(R.string.body_region_eyes)
        BodyRegions.EARS -> context.getString(R.string.body_region_ears)
        BodyRegions.TEETH -> context.getString(R.string.body_region_teeth)
        BodyRegions.THROAT -> context.getString(R.string.body_region_throat)
        BodyRegions.CHEST -> context.getString(R.string.body_region_chest)
        BodyRegions.STOMACH -> context.getString(R.string.body_region_stomach)
        BodyRegions.BACK -> context.getString(R.string.body_region_back)
        BodyRegions.ARMS_HANDS -> context.getString(R.string.body_region_arms_hands)
        BodyRegions.LEGS_FEET -> context.getString(R.string.body_region_legs_feet)
        BodyRegions.SKIN -> context.getString(R.string.body_region_skin)
        else -> regionKey
    }
}
