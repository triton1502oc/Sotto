package com.amh.sotto.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amh.sotto.R
import com.amh.sotto.data.Phrase
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.util.EmergencySafetyHelper
import com.amh.sotto.util.LocaleHelper
import com.amh.sotto.util.UsabilityTracker

@Composable
fun WelcomeOnboardingDialog(
    initialRole: String = VoiceSettings.ROLE_UNSET,
    emergencyPhrase: Phrase?,
    onSaveEmergencyPhrase: (Phrase) -> Unit,
    onFinish: (role: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    var selectedRole by rememberSaveable {
        mutableStateOf(if (initialRole != VoiceSettings.ROLE_UNSET) initialRole else VoiceSettings.ROLE_SELF)
    }

    var area by rememberSaveable { mutableStateOf("") }
    var contactName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        UsabilityTracker.recordActivation(context, "onboarding_started")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row with Step Indicator & Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_step_indicator, currentStep, 3),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            val roleToSave = if (selectedRole.isNotBlank() && selectedRole != VoiceSettings.ROLE_UNSET) {
                                selectedRole
                            } else {
                                VoiceSettings.ROLE_SELF
                            }
                            UsabilityTracker.recordRoleSet(context, roleToSave)
                            UsabilityTracker.recordActivation(context, "onboarding_completed")
                            onFinish(roleToSave)
                        }
                    ) {
                        Text(stringResource(R.string.action_skip))
                    }
                }

                when (currentStep) {
                    1 -> {
                        // Step 1: Role & Intent
                        Text(
                            text = stringResource(R.string.onboarding_step_role_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.onboarding_step_role_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val roles = listOf(
                            Triple(
                                VoiceSettings.ROLE_SELF,
                                stringResource(R.string.role_self_title),
                                stringResource(R.string.role_self_desc)
                            ),
                            Triple(
                                VoiceSettings.ROLE_CAREGIVER,
                                stringResource(R.string.role_caregiver_title),
                                stringResource(R.string.role_caregiver_desc)
                            ),
                            Triple(
                                VoiceSettings.ROLE_PROFESSIONAL,
                                stringResource(R.string.role_professional_title),
                                stringResource(R.string.role_professional_desc)
                            )
                        )

                        roles.forEach { (roleKey, title, desc) ->
                            val isSelected = selectedRole == roleKey
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedRole = roleKey
                                        UsabilityTracker.recordRoleSet(context, roleKey)
                                        currentStep = 2
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // Step 2: Emergency Safety Setup
                        Text(
                            text = stringResource(R.string.onboarding_step_safety_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.onboarding_step_safety_desc),
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

                        Spacer(modifier = Modifier.height(8.dp))

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
                                val targetPhrase = emergencyPhrase?.copy(
                                    text = displayText,
                                    spokenText = spokenText,
                                    isEmergency = true
                                ) ?: Phrase(
                                    text = displayText,
                                    language = LocaleHelper.LANG_AUTO,
                                    spokenText = spokenText,
                                    isEmergency = true,
                                    category = Phrase.CATEGORY_EMERGENCY
                                )
                                onSaveEmergencyPhrase(targetPhrase)
                                UsabilityTracker.recordActivation(context, "safety_card_configured")
                                currentStep = 3
                            },
                            enabled = area.isNotBlank() && phone.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_save_safety_card))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { currentStep = 1 }) {
                                Text(stringResource(R.string.action_back))
                            }
                            TextButton(onClick = { currentStep = 3 }) {
                                Text(stringResource(R.string.action_skip_for_later))
                            }
                        }
                    }

                    3 -> {
                        // Step 3: Ready / Get Started
                        Text(
                            text = stringResource(R.string.onboarding_step_ready_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (selectedRole == VoiceSettings.ROLE_CAREGIVER) {
                                            R.drawable.ic_partner_mode
                                        } else {
                                            R.drawable.ic_volume_up
                                        }
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (selectedRole == VoiceSettings.ROLE_CAREGIVER) {
                                        stringResource(R.string.onboarding_step_ready_caregiver_desc)
                                    } else {
                                        stringResource(R.string.onboarding_step_ready_self_desc)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                UsabilityTracker.recordRoleSet(context, selectedRole)
                                UsabilityTracker.recordActivation(context, "onboarding_completed")
                                onFinish(selectedRole)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_get_started))
                        }
                    }
                }
            }
        }
    }
}
