package com.amh.sotto.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amh.sotto.R
import com.amh.sotto.data.Phrase
import com.amh.sotto.theme.SottoColors

@Composable
fun AddCategoryDialog(
    categories: List<String>,
    allCategoryKey: String,
    onAddCategory: (String) -> Boolean,
    onCategoryAdded: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var catInput by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val reservedError = stringResource(R.string.error_category_reserved)
    val existsError = stringResource(R.string.error_category_exists)

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    if (categories.any { it.equals(trimmed, ignoreCase = true) }) {
                        errorMessage = existsError
                        return@Button
                    }
                    if (onAddCategory(trimmed)) {
                        onCategoryAdded(trimmed)
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun EditCategoryDialog(
    category: String,
    categories: List<String>,
    allCategoryKey: String,
    onRenameCategory: (String, String) -> Boolean,
    onCategoryRenamed: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var catInput by rememberSaveable(category) { mutableStateOf(category) }
    var errorMessage by rememberSaveable(category) { mutableStateOf<String?>(null) }
    val reservedError = stringResource(R.string.error_category_reserved)
    val existsError = stringResource(R.string.error_category_exists)

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    if (trimmed.equals(category, ignoreCase = true)) {
                        onDismiss()
                        return@Button
                    }
                    if (trimmed.equals(allCategoryKey, ignoreCase = true) || Phrase.isUnrenamableCategory(trimmed)) {
                        errorMessage = reservedError
                        return@Button
                    }
                    if (categories.any { it.equals(trimmed, ignoreCase = true) && !it.equals(category, ignoreCase = true) }) {
                        errorMessage = existsError
                        return@Button
                    }
                    if (onRenameCategory(category, trimmed)) {
                        onCategoryRenamed(trimmed)
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.action_rename_category))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!Phrase.isUndeletableCategory(category)) {
                    TextButton(
                        onClick = {
                            onRequestDelete(category)
                        }
                    ) {
                        Text(stringResource(R.string.action_delete_category), color = SottoColors.DestructiveRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    )
}

@Composable
fun DeleteCategoryConfirmDialog(
    category: String,
    onConfirmDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val displayName = getCategoryDisplayName(category)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_delete_category)) },
        text = {
            Text(stringResource(R.string.dialog_message_delete_category, displayName))
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDelete(category)
                    onDismiss()
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
