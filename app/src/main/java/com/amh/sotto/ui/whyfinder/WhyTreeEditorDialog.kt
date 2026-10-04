package com.amh.sotto.ui.whyfinder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var questionToEdit by remember { mutableStateOf<WhyQuestion?>(null) }
    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var editingAreaQuestion by remember { mutableStateOf<WhyArea?>(null) }
    var questionToDelete by remember { mutableStateOf<WhyQuestion?>(null) }

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
                    Column {
                        Text(
                            text = "🌳 " + stringResource(R.string.why_tree_editor_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
                        TextButton(onClick = { showResetConfirm = true }) {
                            Text(
                                text = stringResource(R.string.why_tree_reset_default),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Text("✕", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                                    Text("✏️ Edit", fontSize = 12.sp)
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
                            Text("+ " + stringResource(R.string.action_add_question), fontSize = 13.sp)
                        }
                    }

                    // Questions List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentArea.questions, key = { it.id }) { question ->
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
                                        IconButton(
                                            onClick = { questionToEdit = question },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text("✏️", fontSize = 14.sp)
                                        }
                                        if (currentArea.questions.size > 1) {
                                            IconButton(
                                                onClick = { questionToDelete = question },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Text("🗑️", fontSize = 14.sp)
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
        var editText by rememberSaveable { mutableStateOf(target.text) }
        AlertDialog(
            onDismissRequest = { questionToEdit = null },
            title = { Text(stringResource(R.string.hint_question_text)) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text(stringResource(R.string.hint_question_text)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editText.isNotBlank() && currentArea != null) {
                            val updatedQuestions = currentArea.questions.map {
                                if (it.id == target.id) it.copy(text = editText.trim()) else it
                            }
                            val updatedAreas = tree.areas.map {
                                if (it.id == currentArea.id) it.copy(questions = updatedQuestions) else it
                            }
                            updateTree(tree.copy(areas = updatedAreas))
                        }
                        questionToEdit = null
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { questionToEdit = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Add Question Dialog
    if (showAddQuestionDialog && currentArea != null) {
        var newText by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddQuestionDialog = false },
            title = { Text(stringResource(R.string.action_add_question)) },
            text = {
                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    label = { Text(stringResource(R.string.hint_question_text)) },
                    placeholder = { Text("e.g. Is your shirt too tight?") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newText.isNotBlank()) {
                            val newQ = WhyQuestion(
                                id = "custom_${UUID.randomUUID()}",
                                text = newText.trim(),
                                type = WhyQuestionType.YES_NO
                            )
                            val updatedQuestions = currentArea.questions + newQ
                            val updatedAreas = tree.areas.map {
                                if (it.id == currentArea.id) it.copy(questions = updatedQuestions) else it
                            }
                            updateTree(tree.copy(areas = updatedAreas))
                        }
                        showAddQuestionDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddQuestionDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Edit Area Opening Question Dialog
    editingAreaQuestion?.let { targetArea ->
        var editOpeningText by rememberSaveable { mutableStateOf(targetArea.question) }
        AlertDialog(
            onDismissRequest = { editingAreaQuestion = null },
            title = { Text(stringResource(R.string.label_area_question)) },
            text = {
                OutlinedTextField(
                    value = editOpeningText,
                    onValueChange = { editOpeningText = it },
                    label = { Text(stringResource(R.string.label_area_question)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editOpeningText.isNotBlank()) {
                            val updatedAreas = tree.areas.map {
                                if (it.id == targetArea.id) it.copy(question = editOpeningText.trim()) else it
                            }
                            updateTree(tree.copy(areas = updatedAreas))
                        }
                        editingAreaQuestion = null
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAreaQuestion = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
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
