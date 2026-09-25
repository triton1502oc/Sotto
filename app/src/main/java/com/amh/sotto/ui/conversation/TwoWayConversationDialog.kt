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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlinx.coroutines.delay

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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var displayedText by rememberSaveable { mutableStateOf("") }
    var isFlipped by rememberSaveable { mutableStateOf(false) }
    var lastReply by remember { mutableStateOf<String?>(null) }
    var showQuestionsSheet by rememberSaveable { mutableStateOf(false) }
    var showKeyboardInput by rememberSaveable { mutableStateOf(false) }
    var manualInputText by rememberSaveable { mutableStateOf("") }

    val activeText = displayedText.ifBlank { liveSpokenText }

    // Update displayed text whenever speech recognition outputs new words
    LaunchedEffect(liveSpokenText) {
        if (liveSpokenText.isNotBlank()) {
            displayedText = liveSpokenText
        }
    }

    // Auto-clear reply indicator after 3 seconds
    LaunchedEffect(lastReply) {
        if (lastReply != null) {
            delay(3000)
            lastReply = null
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
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    StatusBadge(lastReply = lastReply, isListening = isListening)
                                    GiantPromptDisplay(activeText = activeText, isListening = isListening)
                                    Box(modifier = Modifier.height(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 4-Button User Rapid-Response Dock facing partner right-side-up
                            UserResponseDock(
                                onResponse = { display, tts -> handleUserResponse(display, tts) }
                            )
                        }
                    }

                    // Bottom: Caregiver controls facing the caregiver holding the phone
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Optional manual keyboard input
                        AnimatedVisibility(visible = showKeyboardInput) {
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

                        // Caregiver Toolbar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Flip Back Button
                                FilledTonalButton(
                                    onClick = { isFlipped = false },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "🔄 " + stringResource(R.string.action_flip),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                // Questions Sheet Button
                                OutlinedButton(
                                    onClick = { showQuestionsSheet = true },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "📋 " + stringResource(R.string.action_caregiver_prompts),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mic Button
                                val micColor by animateColorAsState(
                                    targetValue = if (isListening) Color(0xFFC62828) else MaterialTheme.colorScheme.primaryContainer,
                                    label = "micBgFlipped"
                                )
                                FilledTonalButton(
                                    onClick = {
                                        if (isListening) {
                                            onStopListening()
                                        } else {
                                            displayedText = ""
                                            onClearText()
                                            onStartListening()
                                        }
                                    },
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

                                // Read Aloud Button
                                if (activeText.isNotBlank()) {
                                    IconButton(
                                        onClick = { onSpeakResponse(activeText) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("🔊", fontSize = 17.sp)
                                    }
                                }

                                // Keyboard Toggle Button
                                IconButton(
                                    onClick = { showKeyboardInput = !showKeyboardInput },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("⌨️", fontSize = 17.sp)
                                }

                                // Close Dialog Button
                                IconButton(
                                    onClick = {
                                        onStopListening()
                                        onDismiss()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // NORMAL MODE (Side-by-Side / User Alone)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Action Bar (Caregiver Tools)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 180-degree Flip Button
                            OutlinedButton(
                                onClick = { isFlipped = true },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "🔄 " + stringResource(R.string.action_flip),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Preset Questions Button
                            OutlinedButton(
                                onClick = { showQuestionsSheet = true },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "📋 " + stringResource(R.string.action_caregiver_prompts),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Clear text button
                            if (activeText.isNotBlank()) {
                                IconButton(onClick = {
                                    displayedText = ""
                                    onClearText()
                                    onStopListening()
                                }) {
                                    Text("🗑️", fontSize = 18.sp)
                                }
                            }

                            // Close Dialog
                            IconButton(onClick = {
                                onStopListening()
                                onDismiss()
                            }) {
                                Text("✕", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // Middle: Receptive Display Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top info badge (Listening / Last Reply)
                            StatusBadge(lastReply = lastReply, isListening = isListening)

                            // Center: Giant text prompt
                            GiantPromptDisplay(activeText = activeText, isListening = isListening)

                            // Caregiver Controls (Mic / Read Aloud / Keyboard Input)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val micButtonColor by animateColorAsState(
                                        targetValue = if (isListening) Color(0xFFC62828) else MaterialTheme.colorScheme.primaryContainer,
                                        label = "micBgNormal"
                                    )
                                    val micTextColor = if (isListening) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

                                    // Listen & Read Mic Button
                                    FilledTonalButton(
                                        onClick = {
                                            if (isListening) {
                                                onStopListening()
                                            } else {
                                                displayedText = ""
                                                onClearText()
                                                onStartListening()
                                            }
                                        },
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = micButtonColor,
                                            contentColor = micTextColor
                                        ),
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = if (isListening) "⏹ " + stringResource(R.string.listen_mode_tap_to_stop) else "🎤 " + stringResource(R.string.action_listen_mode),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Read Aloud Button (Spoken TTS for question)
                                    if (activeText.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = { onSpeakResponse(activeText) },
                                            shape = RoundedCornerShape(24.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "🔊 " + stringResource(R.string.action_read_aloud),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(onClick = { showKeyboardInput = !showKeyboardInput }) {
                                        Text("⌨️", fontSize = 20.sp)
                                    }
                                }

                                // Optional Caregiver Manual Keyboard Input Row
                                AnimatedVisibility(visible = showKeyboardInput) {
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
                            }
                        }
                    }

                    // Bottom: User Rapid-Response Dock (4 Big Spacious Touch Targets)
                    UserResponseDock(
                        onResponse = { display, tts -> handleUserResponse(display, tts) }
                    )
                }
            }
        }
    }

    // Caregiver Questions Picker Dialog (Category-Filtered)
    if (showQuestionsSheet) {
        var selectedPromptCategory by remember { mutableStateOf(Phrase.CATEGORY_CARE) }
        AlertDialog(
            onDismissRequest = { showQuestionsSheet = false },
            title = {
                Text(
                    text = stringResource(R.string.action_caregiver_prompts),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Category Filter Chips
                    val categoryOptions = listOf(
                        Phrase.CATEGORY_CARE to R.string.category_care,
                        "ALL" to R.string.category_all,
                        Phrase.CATEGORY_NEEDS to R.string.category_needs,
                        Phrase.CATEGORY_SOCIAL to R.string.category_social,
                        Phrase.CATEGORY_EMERGENCY to R.string.category_emergency,
                        Phrase.CATEGORY_GENERAL to R.string.category_general
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categoryOptions) { (catKey, strRes) ->
                            val isSelected = selectedPromptCategory == catKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPromptCategory = catKey },
                                label = {
                                    Text(
                                        text = stringResource(strRes),
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
