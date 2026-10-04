package com.amh.sotto.ui.category

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amh.sotto.R
import com.amh.sotto.data.Phrase
import com.amh.sotto.theme.SottoColors
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun getCategoryDisplayName(catKey: String): String {
    return when (catKey) {
        "ALL" -> stringResource(R.string.category_all)
        Phrase.CATEGORY_EMERGENCY -> stringResource(R.string.category_emergency)
        Phrase.CATEGORY_NEEDS -> stringResource(R.string.category_needs)
        Phrase.CATEGORY_SOCIAL -> stringResource(R.string.category_social)
        Phrase.CATEGORY_CARE -> stringResource(R.string.category_care)
        Phrase.CATEGORY_GENERAL -> stringResource(R.string.category_general)
        else -> catKey
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onReorderCategories: (fromIndex: Int, toIndex: Int) -> Unit,
    onAddCategory: () -> Unit,
    onRenameCategory: (categoryName: String) -> Unit,
    onDeleteCategory: (categoryName: String) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorderCategories(from.index, to.index)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_title_manage_categories),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(categories, key = { _, cat -> cat }) { index, catName ->
                        ReorderableItem(reorderableLazyListState, key = catName) { isDragging ->
                            val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                            val isEmergency = catName.equals(Phrase.CATEGORY_EMERGENCY, ignoreCase = true)
                            val isGeneral = catName.equals(Phrase.CATEGORY_GENERAL, ignoreCase = true)
                            val isProtectedFromDelete = Phrase.isUndeletableCategory(catName)
                            val isProtectedFromRename = Phrase.isUnrenamableCategory(catName)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isEmergency) SottoColors.EmergencyChipBackgroundDark else MaterialTheme.colorScheme.surfaceVariant,
                                shadowElevation = elevation.value,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Drag handle
                                    IconButton(
                                        onClick = {},
                                        modifier = Modifier
                                            .size(36.dp)
                                            .draggableHandle()
                                    ) {
                                        Text(
                                            "☰",
                                            fontSize = 18.sp,
                                            color = if (isEmergency) SottoColors.EmergencyTextSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Single-step reorder buttons (for fine motor accessibility)
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) onReorderCategories(index, index - 1)
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text(
                                                "▲",
                                                fontSize = 11.sp,
                                                color = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (index < categories.size - 1) onReorderCategories(index, index + 1)
                                            },
                                            enabled = index < categories.size - 1,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text(
                                                "▼",
                                                fontSize = 11.sp,
                                                color = if (index < categories.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Category Name & Subtitle / Badges
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = getCategoryDisplayName(catName),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isEmergency) SottoColors.EmergencyTextSecondary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isEmergency) {
                                            Text(
                                                text = stringResource(R.string.badge_emergency_locked),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SottoColors.EmergencyAccent
                                            )
                                        } else if (isGeneral) {
                                            Text(
                                                text = stringResource(R.string.badge_general_default),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    // Edit / Rename button
                                    if (!isProtectedFromRename) {
                                        IconButton(
                                            onClick = { onRenameCategory(catName) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text("✏️", fontSize = 14.sp)
                                        }
                                    }

                                    // Delete button
                                    if (!isProtectedFromDelete) {
                                        IconButton(
                                            onClick = { onDeleteCategory(catName) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onAddCategory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ ${stringResource(R.string.action_add_category)}")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_save))
            }
        }
    )
}
