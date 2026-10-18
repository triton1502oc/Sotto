package com.amh.sotto.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amh.sotto.R
import com.amh.sotto.data.Phrase
import com.amh.sotto.util.EmergencySafetyHelper
import com.amh.sotto.util.LocaleHelper
import com.amh.sotto.util.UsabilityTracker

@Composable
fun EmergencySafetySetupDialog(
    phrase: Phrase,
    onSavePhrase: (Phrase) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var area by rememberSaveable { mutableStateOf("") }
    var contactName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.safety_card_setup_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.safety_card_setup_msg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                EmergencyContactFormFields(
                    area = area,
                    onAreaChange = { area = it },
                    contactName = contactName,
                    onContactNameChange = { contactName = it },
                    phone = phone,
                    onPhoneChange = { phone = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetLocale = LocaleHelper.getLocaleForLanguage(LocaleHelper.getEffectiveLanguage(context))
                    val (displayText, spokenText) = EmergencySafetyHelper.formatSafetyPhrase(
                        context = context,
                        area = area,
                        contactName = contactName,
                        phone = phone,
                        locale = targetLocale
                    )
                    val updatedPhrase = phrase.copy(
                        text = displayText,
                        spokenText = spokenText,
                        isEmergency = true
                    )
                    onSavePhrase(updatedPhrase)
                    UsabilityTracker.recordActivation(context, "safety_card_configured")
                    onDismiss()
                },
                enabled = area.isNotBlank() && phone.isNotBlank()
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

@Composable
fun EmergencyContactFormFields(
    area: String,
    onAreaChange: (String) -> Unit,
    contactName: String,
    onContactNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = area,
            onValueChange = onAreaChange,
            label = { Text(stringResource(R.string.label_neighborhood_area)) },
            placeholder = { Text(stringResource(R.string.hint_neighborhood_area)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = contactName,
            onValueChange = onContactNameChange,
            label = { Text(stringResource(R.string.label_contact_name)) },
            placeholder = { Text(stringResource(R.string.hint_contact_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text(stringResource(R.string.label_emergency_phone)) },
            placeholder = { Text(stringResource(R.string.hint_emergency_phone)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

