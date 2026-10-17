package com.amh.sotto.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amh.sotto.R
import com.amh.sotto.data.Phrase
import com.amh.sotto.ui.category.getCategoryDisplayName

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExportBackupDialog(
    phraseCount: Int,
    areaCount: Int,
    logCount: Int,
    phrases: List<Phrase> = emptyList(),
    categories: List<String> = emptyList(),
    onConfirmExport: (exportPhrases: Boolean, exportTree: Boolean, exportLog: Boolean, selectedCategories: Set<String>?) -> Unit,
    onShareQR: ((exportPhrases: Boolean, exportTree: Boolean, exportLog: Boolean, selectedCategories: Set<String>?) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var exportPhrases by rememberSaveable { mutableStateOf(true) }
    var exportTree by rememberSaveable { mutableStateOf(true) }
    var exportLog by rememberSaveable { mutableStateOf(false) }
    var selectedCategories by remember(categories) { mutableStateOf(categories.toSet()) }

    val categoryCounts = remember(phrases, categories) {
        categories.associateWith { cat -> phrases.count { it.category == cat } }
    }
    val effectivePhraseCount = if (categories.isNotEmpty()) {
        phrases.count { it.category in selectedCategories }
    } else {
        phraseCount
    }
    val hasPhrasesSelected = exportPhrases && (categories.isEmpty() || selectedCategories.isNotEmpty())
    val hasSelection = hasPhrasesSelected || exportTree || exportLog
    val effectiveSelectedCategories = if (categories.isNotEmpty()) selectedCategories else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_export_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_backup_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Option 1: Phrases & Categories Card Tile
                SelectableBackupOptionCard(
                    checked = exportPhrases,
                    onCheckedChange = { exportPhrases = it },
                    title = stringResource(R.string.export_opt_phrases, effectivePhraseCount),
                    iconRes = R.drawable.ic_cards,
                    extraContent = if (exportPhrases && categories.isNotEmpty()) {
                        {
                            val allSelected = selectedCategories.size == categories.size
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.export_select_categories),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                    Text(
                                        text = stringResource(
                                            if (allSelected) R.string.action_deselect_all else R.string.action_select_all
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                selectedCategories = if (allSelected) emptySet() else categories.toSet()
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    categories.forEach { category ->
                                        val isSelected = category in selectedCategories
                                        val count = categoryCounts[category] ?: 0
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedCategories = if (isSelected) {
                                                    selectedCategories - category
                                                } else {
                                                    selectedCategories + category
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = "${getCategoryDisplayName(category)} ($count)",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else null
                )

                // Option 2: Question Tree Card Tile
                SelectableBackupOptionCard(
                    checked = exportTree,
                    onCheckedChange = { exportTree = it },
                    title = stringResource(R.string.export_opt_why_tree, areaCount),
                    iconRes = R.drawable.ic_account_tree
                )

                // Option 3: Private Why Log Card Tile (Warning if selected)
                SelectableBackupOptionCard(
                    checked = exportLog,
                    onCheckedChange = { exportLog = it },
                    title = stringResource(R.string.export_opt_why_log, logCount),
                    iconRes = R.drawable.ic_shield,
                    warning = stringResource(R.string.export_why_log_warning),
                    isWarningActive = true
                )

                if (onShareQR != null) {
                    OutlinedButton(
                        onClick = { onShareQR(hasPhrasesSelected, exportTree, exportLog, effectiveSelectedCategories) },
                        enabled = hasPhrasesSelected && effectivePhraseCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_qr_code_scanner),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.action_share_qr),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmExport(hasPhrasesSelected, exportTree, exportLog, effectiveSelectedCategories) },
                enabled = hasSelection,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun SelectableBackupOptionCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    iconRes: Int,
    warning: String? = null,
    isWarningActive: Boolean = false,
    extraContent: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(12.dp),
        color = if (isWarningActive && checked) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        } else if (checked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = BorderStroke(
            width = if (checked) 1.5.dp else 1.dp,
            color = if (isWarningActive && checked) {
                MaterialTheme.colorScheme.error
            } else if (checked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isWarningActive && checked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (warning != null && checked) {
                Text(
                    text = warning,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 44.dp, end = 8.dp, bottom = 4.dp)
                )
            }
            if (extraContent != null && checked) {
                extraContent()
            }
        }
    }
}
