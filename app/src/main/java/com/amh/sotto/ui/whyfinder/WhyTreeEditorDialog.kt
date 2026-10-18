package com.amh.sotto.ui.whyfinder

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import com.amh.sotto.util.SpeechRecognitionHelper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amh.sotto.R
import com.amh.sotto.data.WhyArea
import com.amh.sotto.data.WhyFinderRepository
import com.amh.sotto.data.WhyQuestion
import com.amh.sotto.data.WhyQuestionType
import com.amh.sotto.data.WhyTree
import com.amh.sotto.util.LocaleHelper
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhyTreeEditorDialog(
    repository: WhyFinderRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var tree by remember { mutableStateOf(repository.getTree()) }
    var selectedAreaId by rememberSaveable {
        mutableStateOf(tree.areas.firstOrNull()?.id ?: "")
    }

    var showResetConfirm by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var questionToEdit by remember { mutableStateOf<WhyQuestion?>(null) }
    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var editingAreaQuestion by remember { mutableStateOf<WhyArea?>(null) }
    var questionToDelete by remember { mutableStateOf<WhyQuestion?>(null) }

    var activeSpeechCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                activeSpeechCallback?.invoke(matches[0])
            }
        }
        activeSpeechCallback = null
    }

    fun launchSpeech(onResult: (String) -> Unit) {
        activeSpeechCallback = onResult
        val intent = SpeechRecognitionHelper.createSpeechIntent(context, preferOffline = true)
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            activeSpeechCallback = null
        }
    }

    val currentArea = tree.areas.firstOrNull { it.id == selectedAreaId } ?: tree.areas.firstOrNull()

    fun updateTree(newTree: WhyTree) {
        repository.saveTree(newTree)
        tree = newTree
    }

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
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_account_tree),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.why_tree_editor_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Customise yes/no narrowing questions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.action_done),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Text(
                                    text = "⋮",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.why_tree_reset_default),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_replay),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showResetConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }

                // Area Filter Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tree.areas.forEach { area ->
                        val isSelected = area.id == selectedAreaId
                        val areaName = getAreaDisplayName(area.id, context)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAreaId = area.id },
                            label = {
                                Text(
                                    text = areaName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                if (currentArea != null) {
                    // Area Opening Question Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.label_area_question),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(
                                    onClick = { editingAreaQuestion = currentArea },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_edit),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(stringResource(R.string.action_edit_question), fontSize = 12.sp)
                                    }
                                }
                            }
                            Text(
                                text = currentArea.question,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Questions Section Header with Add button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Questions (${currentArea.questions.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FilledTonalButton(
                            onClick = { showAddQuestionDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(stringResource(R.string.action_add_question), fontSize = 13.sp)
                            }
                        }
                    }

                    // Questions List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(currentArea.questions, key = { _, question -> question.id }) { index, question ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = question.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (question.type == WhyQuestionType.BODY_MAP) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "📍 Body Region Map",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isFirst = index == 0
                                        val isLast = index == currentArea.questions.lastIndex

                                        IconButton(
                                            onClick = {
                                                if (!isFirst) {
                                                    val mutable = currentArea.questions.toMutableList()
                                                    val temp = mutable[index]
                                                    mutable[index] = mutable[index - 1]
                                                    mutable[index - 1] = temp
                                                    val updatedAreas = tree.areas.map {
                                                        if (it.id == currentArea.id) it.copy(questions = mutable) else it
                                                    }
                                                    updateTree(tree.copy(areas = updatedAreas))
                                                }
                                            },
                                            enabled = !isFirst,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_arrow_up),
                                                contentDescription = stringResource(R.string.cd_move_up),
                                                modifier = Modifier.size(16.dp),
                                                tint = if (!isFirst) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (!isLast) {
                                                    val mutable = currentArea.questions.toMutableList()
                                                    val temp = mutable[index]
                                                    mutable[index] = mutable[index + 1]
                                                    mutable[index + 1] = temp
                                                    val updatedAreas = tree.areas.map {
                                                        if (it.id == currentArea.id) it.copy(questions = mutable) else it
                                                    }
                                                    updateTree(tree.copy(areas = updatedAreas))
                                                }
                                            },
                                            enabled = !isLast,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_arrow_down),
                                                contentDescription = stringResource(R.string.cd_move_down),
                                                modifier = Modifier.size(16.dp),
                                                tint = if (!isLast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                            )
                                        }

                                        IconButton(
                                            onClick = { questionToEdit = question },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_edit),
                                                contentDescription = stringResource(R.string.action_edit_question),
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (currentArea.questions.size > 1) {
                                            IconButton(
                                                onClick = { questionToDelete = question },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_delete),
                                                    contentDescription = stringResource(R.string.action_delete),
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
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

    // Edit Question Dialog
    questionToEdit?.let { target ->
        QuestionInputDialog(
            title = stringResource(R.string.hint_question_text),
            label = stringResource(R.string.hint_question_text),
            initialText = target.text,
            onSpeechLaunch = { callback -> launchSpeech(callback) },
            onConfirm = { trimmed ->
                if (currentArea != null) {
                    val updatedQuestions = currentArea.questions.map {
                        if (it.id == target.id) it.copy(text = trimmed) else it
                    }
                    val updatedAreas = tree.areas.map {
                        if (it.id == currentArea.id) it.copy(questions = updatedQuestions) else it
                    }
                    updateTree(tree.copy(areas = updatedAreas))
                }
            },
            onDismiss = { questionToEdit = null }
        )
    }

    // Add Question Dialog
    if (showAddQuestionDialog && currentArea != null) {
        QuestionInputDialog(
            title = stringResource(R.string.action_add_question),
            label = stringResource(R.string.hint_question_text),
            placeholder = "e.g. Is your shirt too tight?",
            onSpeechLaunch = { callback -> launchSpeech(callback) },
            onConfirm = { trimmed ->
                val newQ = WhyQuestion(
                    id = "custom_${UUID.randomUUID()}",
                    text = trimmed,
                    type = WhyQuestionType.YES_NO
                )
                val updatedQuestions = currentArea.questions + newQ
                val updatedAreas = tree.areas.map {
                    if (it.id == currentArea.id) it.copy(questions = updatedQuestions) else it
                }
                updateTree(tree.copy(areas = updatedAreas))
            },
            onDismiss = { showAddQuestionDialog = false }
        )
    }

    // Edit Area Opening Question Dialog
    editingAreaQuestion?.let { targetArea ->
        QuestionInputDialog(
            title = stringResource(R.string.label_area_question),
            label = stringResource(R.string.label_area_question),
            initialText = targetArea.question,
            onSpeechLaunch = { callback -> launchSpeech(callback) },
            onConfirm = { trimmed ->
                val updatedAreas = tree.areas.map {
                    if (it.id == targetArea.id) it.copy(question = trimmed) else it
                }
                updateTree(tree.copy(areas = updatedAreas))
            },
            onDismiss = { editingAreaQuestion = null }
        )
    }

    // Delete Question Confirmation Dialog
    questionToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { questionToDelete = null },
            title = { Text(stringResource(R.string.confirm_delete_question)) },
            text = { Text(target.text) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (currentArea != null) {
                            val updatedQuestions = currentArea.questions.filter { it.id != target.id }
                            val updatedAreas = tree.areas.map {
                                if (it.id == currentArea.id) it.copy(questions = updatedQuestions) else it
                            }
                            updateTree(tree.copy(areas = updatedAreas))
                        }
                        questionToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { questionToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.why_tree_reset_default)) },
            text = { Text(stringResource(R.string.why_tree_reset_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val defaultTree = repository.resetTreeToDefault()
                        tree = defaultTree
                        showResetConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.why_tree_reset_default), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun QuestionInputDialog(
    title: String,
    label: String,
    initialText: String = "",
    placeholder: String? = null,
    onSpeechLaunch: ((String) -> Unit) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable(initialText) { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                placeholder = placeholder?.let { { Text(it) } },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (text.isNotBlank()) {
                            IconButton(onClick = { text = "" }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.cd_clear_text),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                onSpeechLaunch { spoken ->
                                    text = if (text.isBlank()) spoken else "$text $spoken"
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_mic),
                                contentDescription = stringResource(R.string.cd_voice_input),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

