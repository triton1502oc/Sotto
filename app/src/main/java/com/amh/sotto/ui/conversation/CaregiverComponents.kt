package com.amh.sotto.ui.conversation

import android.content.Context
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amh.sotto.R
import com.amh.sotto.data.WhyOutcome
import com.amh.sotto.theme.SottoColors
import com.amh.sotto.ui.whyfinder.BodyPartSelectionGrid
import com.amh.sotto.ui.whyfinder.IntensityScaleRow
import com.amh.sotto.ui.whyfinder.WhyFinderState
import com.amh.sotto.ui.whyfinder.getBodyRegionLabel
import com.amh.sotto.util.CardShareHelper
import com.amh.sotto.util.ShareableCardSpec
import com.amh.sotto.util.UsabilityTracker
import java.util.Locale

@Composable
internal fun CaregiverBottomMenu(
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
    autoSpeakQuestions: Boolean = true,
    onToggleAutoSpeak: () -> Unit = {},
    isListening: Boolean,
    onToggleListening: () -> Unit,
    canReadAloudFreeform: Boolean,
    onReadAloudFreeform: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onDismiss: () -> Unit,
    onToggleTriggerLogger: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isCompactScreen = configuration.screenWidthDp <= 360 || configuration.fontScale > 1.15f

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isCompactScreen) 8.dp else 14.dp, vertical = 10.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(if (isCompactScreen) 4.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasWhyFinder) {
                        FilterChip(
                            selected = activeTab == CaregiverTab.WHY_FINDER,
                            onClick = { onTabChange(CaregiverTab.WHY_FINDER) },
                            label = {
                                Text(
                                    text = "🧭 " + stringResource(R.string.mode_why_finder),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                        FilterChip(
                            selected = activeTab == CaregiverTab.FREEFORM,
                            onClick = { onTabChange(CaregiverTab.FREEFORM) },
                            label = {
                                Text(
                                    text = "💬 " + stringResource(R.string.mode_freeform),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                // Global Controls: Material 3 Tool Capsule + Standalone Dismissal (#46, #68)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isCompactScreen) 8.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    val flipRotation by animateFloatAsState(
                        targetValue = if (isFlipped) 180f else 0f,
                        label = "flipIconRotation"
                    )

                    // Utility Capsule: Log (if available) + Flip
                    Surface(
                        shape = RoundedCornerShape(14.dp),
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
                                        .size(if (isCompactScreen) 38.dp else 48.dp)
                                        .semantics {
                                            role = Role.Button
                                            contentDescription = strLogTrigger
                                        }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_note_alt),
                                        contentDescription = null,
                                        modifier = Modifier.size(if (isCompactScreen) 18.dp else 20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val flipDesc = if (isFlipped) stringResource(R.string.action_flip_back) else stringResource(R.string.action_flip)
                            val flipBg = if (isFlipped) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            val flipContentColor = if (isFlipped) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .size(if (isCompactScreen) 38.dp else 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
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
                                        .size(if (isCompactScreen) 20.dp else 22.dp)
                                        .graphicsLayer { rotationZ = flipRotation },
                                    tint = flipContentColor
                                )
                            }
                        }
                    }

                    // Standalone Modal Dismissal (>= 48dp target)
                    val dismissDesc = stringResource(R.string.cd_dismiss)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(if (isCompactScreen) 40.dp else 48.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = dismissDesc
                            }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier.size(if (isCompactScreen) 22.dp else 24.dp),
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

                    FilterChip(
                        selected = autoSpeakQuestions,
                        onClick = onToggleAutoSpeak,
                        label = {
                            Text(
                                text = stringResource(R.string.action_auto_speak),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                } else {
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
internal fun CaregiverDisplayCard(
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val (iconRes, titleRes) = when (whyState.outcome) {
                                WhyOutcome.FOUND -> R.drawable.ic_check_bold to R.string.why_finder_found_title
                                WhyOutcome.STOPPED -> R.drawable.ic_close_bold to R.string.why_finder_stopped_title
                                else -> R.drawable.ic_explore to R.string.why_finder_not_found_title
                            }
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }

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
                            Icon(
                                painter = painterResource(R.drawable.ic_refresh),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_restart))
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

            val context = LocalContext.current
            val shareCd = stringResource(R.string.cd_share_card)
            val partnerBadgeLabel = stringResource(R.string.share_card_partner_badge)
            val brandFooter = stringResource(R.string.share_card_brand_footer)
            val activePrompt = if (activeTab == CaregiverTab.WHY_FINDER) whyPromptText else activeFreeformText
            val isFinishedWhy = activeTab == CaregiverTab.WHY_FINDER && whyState is WhyFinderState.Finished
            val canShareCard = isFinishedWhy || activePrompt.isNotBlank() || !lastReply.isNullOrBlank()
            val formattedReplyLabel = lastReply?.takeIf { it.isNotBlank() }?.let {
                stringResource(R.string.share_card_reply_format, it)
            }
            val finishedWhyTitle = if (whyState is WhyFinderState.Finished) {
                val titleRes = when (whyState.outcome) {
                    WhyOutcome.FOUND -> R.string.why_finder_found_title
                    WhyOutcome.STOPPED -> R.string.why_finder_stopped_title
                    else -> R.string.why_finder_not_found_title
                }
                stringResource(titleRes)
            } else {
                ""
            }

            if (canShareCard) {
                IconButton(
                    onClick = {
                        val (primaryToShare, secondaryToShare) = if (activeTab == CaregiverTab.WHY_FINDER && whyState is WhyFinderState.Finished) {
                            val primary = whyState.causeText?.takeIf { it.isNotBlank() } ?: finishedWhyTitle
                            val detail = if (!whyState.bodyPart.isNullOrBlank()) {
                                val partLabel = getBodyRegionLabel(whyState.bodyPart, context)
                                val intensityStr = whyState.intensity?.let { " • $it/5" } ?: ""
                                "$partLabel$intensityStr"
                            } else {
                                formattedReplyLabel
                            }
                            primary to detail
                        } else {
                            val primary = activePrompt.ifBlank { lastReply.orEmpty() }
                            val secondary = if (activePrompt.isNotBlank()) formattedReplyLabel else null
                            primary to secondary
                        }

                        com.amh.sotto.util.CardShareHelper.shareCard(
                            context = context,
                            spec = com.amh.sotto.util.ShareableCardSpec(
                                primaryText = primaryToShare,
                                secondaryText = secondaryToShare,
                                badgeLabel = partnerBadgeLabel,
                                isEmergency = false,
                                footerBrandText = brandFooter
                            ),
                            sourceTag = "partner_mode"
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = shareCd,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun UnifiedTactileResponseDock(
    onYes: () -> Unit,
    onNo: () -> Unit,
    onWait: () -> Unit,
    onNotSure: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val strYes = stringResource(R.string.response_yes)
    val strNo = stringResource(R.string.response_no)
    val strWait = stringResource(R.string.response_wait)
    val strNotSure = stringResource(R.string.why_finder_btn_not_sure)
    val strStop = stringResource(R.string.why_finder_btn_stop)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: YES & NO (Primary Tactile Crisis Buttons >= 80dp touch target with tactile haptics)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TactileResponseButton(
                text = strYes,
                iconRes = R.drawable.ic_check_bold,
                containerColor = SottoColors.SuccessContainer,
                contentColor = SottoColors.SuccessText,
                onClick = onYes,
                height = 80.dp,
                shapeRadius = 20.dp,
                iconSize = 24.dp,
                spacerWidth = 8.dp,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                hapticType = HapticFeedbackType.LongPress,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                contentPadding = ButtonDefaults.ContentPadding,
                maxLines = Int.MAX_VALUE
            )

            TactileResponseButton(
                text = strNo,
                iconRes = R.drawable.ic_close_bold,
                containerColor = SottoColors.CrisisNoContainer,
                contentColor = SottoColors.CrisisNoText,
                onClick = onNo,
                height = 80.dp,
                shapeRadius = 20.dp,
                iconSize = 24.dp,
                spacerWidth = 8.dp,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                hapticType = HapticFeedbackType.LongPress,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                contentPadding = ButtonDefaults.ContentPadding,
                maxLines = Int.MAX_VALUE
            )
        }

        // Row 2: Regulatory Actions (Wait, Not Sure, Stop) with Vector Icons and AAA Contrast
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TactileResponseButton(
                text = strWait,
                iconRes = R.drawable.ic_hourglass,
                containerColor = SottoColors.NeutralDarkContainer,
                contentColor = SottoColors.NeutralDarkText,
                onClick = onWait
            )

            TactileResponseButton(
                text = strNotSure,
                iconRes = R.drawable.ic_help,
                containerColor = SottoColors.NeutralDarkContainer,
                contentColor = SottoColors.NeutralDarkText,
                onClick = onNotSure
            )

            TactileResponseButton(
                text = strStop,
                iconRes = R.drawable.ic_stop_circle,
                containerColor = SottoColors.NeutralStoppedContainer,
                contentColor = SottoColors.NeutralStoppedText,
                onClick = onStop
            )
        }
    }
}

@Composable
private fun RowScope.TactileResponseButton(
    text: String,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    height: Dp = 54.dp,
    shapeRadius: Dp = 18.dp,
    iconSize: Dp = 18.dp,
    spacerWidth: Dp = 4.dp,
    fontSize: TextUnit = 15.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    elevation: ButtonElevation? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    maxLines: Int = 1
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(hapticType)
            onClick()
        },
        modifier = Modifier
            .weight(1f)
            .heightIn(min = height)
            .height(height)
            .semantics {
                role = Role.Button
                contentDescription = text
            },
        shape = RoundedCornerShape(shapeRadius),
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = elevation
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(spacerWidth))
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun CaregiverResponseDock(
    activeTab: CaregiverTab,
    whyState: WhyFinderState,
    onYes: () -> Unit,
    onNo: () -> Unit,
    onWait: () -> Unit,
    onNotSure: () -> Unit,
    onStop: () -> Unit,
    onSelectBodyPart: (String) -> Unit,
    onSelectIntensity: (Int) -> Unit,
    modifier: Modifier = Modifier
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
                // Handled in Finished card
            }
            else -> {
                UnifiedTactileResponseDock(
                    onYes = onYes,
                    onNo = onNo,
                    onWait = onWait,
                    onNotSure = onNotSure,
                    onStop = onStop,
                    modifier = modifier
                )
            }
        }
    } else {
        // Freeform / Talk mode
        UnifiedTactileResponseDock(
            onYes = onYes,
            onNo = onNo,
            onWait = onWait,
            onNotSure = onNotSure,
            onStop = onStop,
            modifier = modifier
        )
    }
}

@Composable
internal fun PostResolutionTriggerLogger(
    onLogTrigger: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_note_alt),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.label_log_trigger),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.cd_dismiss),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val triggers = listOf(
                "Pain" to stringResource(R.string.trigger_pain),
                "Noise" to stringResource(R.string.trigger_noise),
                "Task" to stringResource(R.string.trigger_task),
                "Emotion" to stringResource(R.string.trigger_emotion),
                "Other" to stringResource(R.string.trigger_other)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(triggers) { (key, label) ->
                    FilledTonalButton(
                        onClick = {
                            UsabilityTracker.startIntent("trigger_logged")
                            UsabilityTracker.endIntent(context, "trigger_logged", outcome = key.lowercase(Locale.ROOT))
                            onLogTrigger(key)
                        },
                        modifier = Modifier.heightIn(min = 38.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun StatusBadge(
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
internal fun GiantPromptDisplay(
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
internal fun ManualKeyboardInputRow(
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
