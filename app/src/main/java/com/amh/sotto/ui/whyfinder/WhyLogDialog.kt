package com.amh.sotto.ui.whyfinder

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.amh.sotto.R
import com.amh.sotto.data.SharedPreferencesWhyFinderRepository
import com.amh.sotto.data.WhyFinderRepository
import com.amh.sotto.data.WhyLogEntry
import com.amh.sotto.data.WhyOutcome
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhyLogDialog(
    repository: WhyFinderRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(repository.getLogs()) }
    var entryToDelete by remember { mutableStateOf<WhyLogEntry?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔒 " + stringResource(R.string.why_log_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "On-device only • Private & offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (logs.isNotEmpty()) {
                            TextButton(onClick = { showClearConfirm = true }) {
                                Text(
                                    text = stringResource(R.string.action_clear_all),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Text("✕", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📋", fontSize = 48.sp)
                            Text(
                                text = stringResource(R.string.why_log_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val thirtyDaysAgo = remember { System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L }
                    val thirtyDayLogs = remember(logs) { logs.filter { it.startedAt >= thirtyDaysAgo } }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (thirtyDayLogs.isNotEmpty()) {
                            item {
                                ThirtyDayPatternsCard(logs = thirtyDayLogs, context = context)
                            }
                        }

                        items(logs, key = { it.id }) { entry ->
                            WhyLogEntryCard(
                                entry = entry,
                                context = context,
                                onDelete = { entryToDelete = entry }
                            )
                        }
                    }
                }
            }
        }
    }

    // Single Entry Delete Confirmation Dialog
    entryToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text(stringResource(R.string.why_log_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.deleteLog(target.id)
                        logs = repository.getLogs()
                        entryToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.action_clear_all)) },
            text = { Text(stringResource(R.string.why_log_clear_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.clearLogs()
                        logs = emptyList()
                        showClearConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.action_clear_all), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ThirtyDayPatternsCard(
    logs: List<WhyLogEntry>,
    context: Context
) {
    val totalCount = logs.size
    val foundCount = logs.count { it.outcome == WhyOutcome.FOUND }

    val topCauseEntry = remember(logs) {
        logs.filter { it.outcome == WhyOutcome.FOUND && !it.causeText.isNullOrBlank() }
            .groupBy { entry ->
                val part = entry.bodyPart?.let { getBodyRegionLabel(it, context) }
                val cause = entry.causeText ?: ""
                if (part != null) "$cause ($part)" else cause
            }
            .maxByOrNull { it.value.size }
    }

    val topAreaEntry = remember(logs) {
        logs.filter { !it.areaId.isNullOrBlank() }
            .groupBy { it.areaId }
            .maxByOrNull { it.value.size }
    }

    val timeSummary = remember(logs) {
        val buckets = mutableMapOf<String, Int>()
        val cal = Calendar.getInstance()
        logs.forEach { entry ->
            cal.timeInMillis = entry.startedAt
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val bucketName = when (hour) {
                in 5..11 -> context.getString(R.string.time_morning)
                in 12..16 -> context.getString(R.string.time_afternoon)
                in 17..21 -> context.getString(R.string.time_evening)
                else -> context.getString(R.string.time_night)
            }
            buckets[bucketName] = (buckets[bucketName] ?: 0) + 1
        }
        buckets.entries
            .sortedByDescending { it.value }
            .joinToString(", ") { "${it.key} (${it.value})" }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 " + stringResource(R.string.why_log_patterns_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$foundCount / $totalCount resolved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (topCauseEntry != null) {
                Text(
                    text = stringResource(R.string.why_log_top_cause, topCauseEntry.key, topCauseEntry.value.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (topAreaEntry != null) {
                val areaName = getAreaDisplayName(topAreaEntry.key, context)
                Text(
                    text = stringResource(R.string.why_log_top_area, areaName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (timeSummary.isNotBlank()) {
                Text(
                    text = stringResource(R.string.why_log_time_summary, timeSummary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun WhyLogEntryCard(
    entry: WhyLogEntry,
    context: Context,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedTime = remember(entry.startedAt) { dateFormat.format(Date(entry.startedAt)) }

    val outcomeBadge = when (entry.outcome) {
        WhyOutcome.FOUND -> Triple("✓ Found", Color(0xFF1B5E20), Color(0xFFE8F5E9))
        WhyOutcome.NOT_FOUND -> Triple("? Not Found", Color(0xFF5D4037), Color(0xFFEFEBE9))
        WhyOutcome.STOPPED -> Triple("⏹ Stopped", Color(0xFF37474F), Color(0xFFECEFF1))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Time & Outcome Badge & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = outcomeBadge.second
                    ) {
                        Text(
                            text = outcomeBadge.first,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = outcomeBadge.third,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Text("🗑️", fontSize = 14.sp)
                }
            }

            // Cause / Outcome Details
            val causeTitle = if (!entry.causeText.isNullOrBlank()) {
                val partLabel = entry.bodyPart?.let { getBodyRegionLabel(it, context) }
                val intensityStr = entry.intensity?.let { " (Intensity: $it/5)" } ?: ""
                val partStr = if (partLabel != null) " • $partLabel" else ""
                "${entry.causeText}$partStr$intensityStr"
            } else {
                when (entry.outcome) {
                    WhyOutcome.FOUND -> "Found"
                    WhyOutcome.NOT_FOUND -> stringResource(R.string.why_finder_not_found_title)
                    WhyOutcome.STOPPED -> stringResource(R.string.why_finder_stopped_title)
                }
            }

            Text(
                text = causeTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Area and Duration info
            val areaLabel = getAreaDisplayName(entry.areaId, context)
            val durationSec = ((entry.endedAt - entry.startedAt) / 1000L).coerceAtLeast(0L)
            val stepsCount = entry.steps.size

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (areaLabel.isNotBlank()) {
                    Text(
                        text = "Area: $areaLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "${stepsCount} steps • ${durationSec}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Note if present
            if (!entry.note.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "📝 ${entry.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun getAreaDisplayName(areaId: String?, context: Context): String {
    return when (areaId) {
        SharedPreferencesWhyFinderRepository.AREA_BODY -> context.getString(R.string.why_area_body_title)
        SharedPreferencesWhyFinderRepository.AREA_SENSES -> context.getString(R.string.why_area_senses_title)
        SharedPreferencesWhyFinderRepository.AREA_FEELINGS -> context.getString(R.string.why_area_feelings_title)
        SharedPreferencesWhyFinderRepository.AREA_PERSON -> context.getString(R.string.why_area_person_title)
        SharedPreferencesWhyFinderRepository.AREA_HAPPENED -> context.getString(R.string.why_area_happened_title)
        SharedPreferencesWhyFinderRepository.AREA_COMING_UP -> context.getString(R.string.why_area_coming_up_title)
        else -> areaId ?: ""
    }
}
