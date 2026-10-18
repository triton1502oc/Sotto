package com.amh.sotto.ui.main

import android.app.Activity
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amh.sotto.R
import com.amh.sotto.data.Phrase
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.theme.SottoColors
import com.amh.sotto.ui.category.getCategoryDisplayName
import com.amh.sotto.util.LocaleHelper
import com.amh.sotto.util.SpeechRecognitionHelper
import com.amh.sotto.util.TranslationHelper
import com.amh.sotto.util.UsabilityTracker

@Composable
fun AddEditPhraseDialog(
    phraseToEdit: Phrase?,
    initialAddText: String,
    phrases: List<Phrase>,
    categories: List<String>,
    selectedCategory: String,
    allCategoryKey: String,
    voiceSettings: VoiceSettings,
    onSavePhrase: (Phrase) -> Unit,
    onDeletePhrase: (Phrase) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val voiceInputPrompt = stringResource(R.string.cd_voice_input)
    var textValue by remember(phraseToEdit) { mutableStateOf(phraseToEdit?.text ?: initialAddText) }
    var spokenTextValue by remember(phraseToEdit) { mutableStateOf(phraseToEdit?.spokenText ?: "") }
    var spokenLangValue by remember(phraseToEdit) { mutableStateOf(phraseToEdit?.spokenLanguage ?: LocaleHelper.LANG_AUTO) }
    var isSpokenTextExpanded by remember(phraseToEdit) { mutableStateOf(!phraseToEdit?.spokenText.isNullOrBlank()) }
    var isTranslating by remember { mutableStateOf(false) }
    var isModelDownloading by remember { mutableStateOf(false) }
    var showModelDownloadConfirmDialog by remember { mutableStateOf(false) }
    var translationErrorResId by remember { mutableStateOf<Int?>(null) }
    var selectedCat by remember(phraseToEdit) {
        mutableStateOf(
            if (phraseToEdit?.isEmergency == true) Phrase.CATEGORY_EMERGENCY
            else (phraseToEdit?.category ?: if (selectedCategory != allCategoryKey) selectedCategory else Phrase.CATEGORY_GENERAL)
        )
    }
    val isEditModeDialog = phraseToEdit != null
    val isDuplicateText = remember(textValue, phraseToEdit, phrases) {
        val trimmed = textValue.trim()
        trimmed.isNotBlank() && phrases.any { p ->
            p.text.trim().equals(trimmed, ignoreCase = true) &&
                (phraseToEdit == null || !p.text.trim().equals(phraseToEdit.text.trim(), ignoreCase = true))
        }
    }

    fun executeTranslation(targetLang: String, sourceLang: String) {
        translationErrorResId = null
        TranslationHelper.translate(
            text = textValue.trim(),
            sourceLangCode = sourceLang,
            targetLangCode = targetLang,
            onProgress = { isTranslating = it },
            onSuccess = { translated ->
                spokenTextValue = translated
                spokenLangValue = targetLang
                isModelDownloading = false
            },
            onError = {
                isModelDownloading = false
                translationErrorResId = R.string.error_translation_failed
            }
        )
    }

    if (showModelDownloadConfirmDialog) {
        val targetLangName = LocaleHelper.getLanguageDisplayName(voiceSettings.secondaryLanguage)
        AlertDialog(
            onDismissRequest = { showModelDownloadConfirmDialog = false },
            title = { Text(stringResource(R.string.dialog_download_model_title)) },
            text = {
                Text(stringResource(R.string.dialog_download_model_message, targetLangName))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showModelDownloadConfirmDialog = false
                        isModelDownloading = true
                        val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                        val sourceLang = LocaleHelper.resolvePhraseLocale(LocaleHelper.LANG_AUTO, textValue, effectiveLang).language
                        val targetLang = voiceSettings.secondaryLanguage
                        executeTranslation(targetLang, sourceLang)
                    }
                ) {
                    Text(stringResource(R.string.action_download_and_translate))
                }
            },
            dismissButton = {
                TextButton(onClick = { showModelDownloadConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                textValue = if (textValue.isBlank()) matches[0] else "$textValue ${matches[0]}"
            }
        }
    }

    val spokenSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                spokenTextValue = if (spokenTextValue.isBlank()) matches[0] else "$spokenTextValue ${matches[0]}"
            }
        }
    }

    val handleSave: () -> Unit = {
        if (isDuplicateText) {
            UsabilityTracker.recordFriction(context, "duplicate_phrase_blocked")
        } else if (textValue.isNotBlank()) {
            val isEmergency = selectedCat == Phrase.CATEGORY_EMERGENCY
            val newPhrase = Phrase(
                text = textValue.trim(),
                spokenText = if (voiceSettings.showLanguageSwitcher) spokenTextValue.trim().takeIf { it.isNotBlank() } else phraseToEdit?.spokenText,
                spokenLanguage = if (voiceSettings.showLanguageSwitcher) spokenLangValue.takeIf { spokenTextValue.isNotBlank() } else phraseToEdit?.spokenLanguage,
                language = LocaleHelper.LANG_AUTO,
                isEmergency = isEmergency,
                category = selectedCat
            )
            onSavePhrase(newPhrase)
            if (newPhrase.isEmergency) {
                UsabilityTracker.recordSafetyCardSaved(context)
            }
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditModeDialog) {
                    stringResource(R.string.dialog_edit_phrase_title)
                } else {
                    stringResource(R.string.dialog_add_phrase_title)
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Phrase Text Field with Voice Input (Microphone button)
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(stringResource(R.string.label_phrase_text)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            handleSave()
                        }
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val intent = SpeechRecognitionHelper.createSpeechIntent(context, prompt = voiceInputPrompt)
                                try {
                                    speechLauncher.launch(intent)
                                } catch (_: Exception) {
                                    // Ignore if speech recognizer not present
                                }
                            }
                        ) {
                            Text("🎤", fontSize = 20.sp)
                        }
                    },
                    isError = isDuplicateText,
                    supportingText = if (isDuplicateText) {
                        { Text(stringResource(R.string.error_phrase_exists), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2
                )

                if (voiceSettings.showLanguageSwitcher) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Expandable Alternate Spoken Text
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isSpokenTextExpanded = !isSpokenTextExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🗣️", fontSize = 14.sp)
                            Text(
                                text = stringResource(R.string.label_spoken_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = if (isSpokenTextExpanded) "▲" else "▼",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isSpokenTextExpanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.label_spoken_language) + ": ${LocaleHelper.getLanguageDisplayName(voiceSettings.secondaryLanguage)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (textValue.isNotBlank() && !isTranslating && !isModelDownloading) {
                                        translationErrorResId = null
                                        val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                                        val sourceLang = LocaleHelper.resolvePhraseLocale(LocaleHelper.LANG_AUTO, textValue, effectiveLang).language
                                        val targetLang = voiceSettings.secondaryLanguage
                                        TranslationHelper.isModelDownloaded(targetLang) { isDownloaded ->
                                            if (isDownloaded) {
                                                executeTranslation(targetLang, sourceLang)
                                            } else {
                                                showModelDownloadConfirmDialog = true
                                            }
                                        }
                                    }
                                },
                                enabled = textValue.isNotBlank() && !isTranslating && !isModelDownloading,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                if (isTranslating || isModelDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isModelDownloading) stringResource(R.string.status_downloading_model) else stringResource(R.string.status_translating),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                } else {
                                    Text(
                                        text = "🌐 " + stringResource(R.string.action_auto_translate),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        if (translationErrorResId != null) {
                            Text(
                                text = stringResource(translationErrorResId!!),
                                style = MaterialTheme.typography.bodySmall,
                                color = SottoColors.DestructiveRed,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        OutlinedTextField(
                            value = spokenTextValue,
                            onValueChange = { spokenTextValue = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.hint_spoken_text),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    handleSave()
                                }
                            ),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val targetLang = if (spokenLangValue != LocaleHelper.LANG_AUTO) spokenLangValue else voiceSettings.secondaryLanguage
                                        val intent = SpeechRecognitionHelper.createSpeechIntent(context, prompt = voiceInputPrompt, language = targetLang)
                                        try {
                                            spokenSpeechLauncher.launch(intent)
                                        } catch (_: Exception) {
                                            // Ignore if speech recognizer not present
                                        }
                                    }
                                ) {
                                    Text("🎤", fontSize = 20.sp)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Selection
                Text(
                    text = stringResource(R.string.label_category),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { catName ->
                        val isSelected = selectedCat == catName
                        val isEmergencyCat = catName.equals(Phrase.CATEGORY_EMERGENCY, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCat = catName },
                            leadingIcon = if (isEmergencyCat) {
                                {
                                    Text(
                                        text = "🛡️",
                                        fontSize = 12.sp
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = getCategoryDisplayName(catName),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isEmergencyCat) SottoColors.EmergencyChipBackground else MaterialTheme.colorScheme.surfaceVariant,
                                selectedLabelColor = if (isEmergencyCat) SottoColors.EmergencyTextSecondary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (isEmergencyCat && isSelected) {
                                BorderStroke(1.dp, SottoColors.EmergencyAccent)
                            } else null
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = handleSave,
                enabled = textValue.isNotBlank() && !isDuplicateText
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEditModeDialog) {
                    TextButton(onClick = {
                        onDeletePhrase(phraseToEdit)
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.action_delete), color = SottoColors.DestructiveRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}
