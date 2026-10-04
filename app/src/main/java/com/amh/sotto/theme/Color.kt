package com.amh.sotto.theme

import androidx.compose.ui.graphics.Color

/**
 * Centralized semantic color tokens for Sotto's high-contrast, low-stimulus dark theme.
 */
object SottoColors {
    // Core dark palette
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val TextPrimary = Color(0xFFE0E0E0)

    // Emergency / Crisis tokens
    val EmergencyAccent = Color(0xFFFFB74D) // Amber accent, borders, primary emergency buttons
    val EmergencyContainer = Color(0xFF1E1710) // Dark warm container for emergency cards
    val EmergencyText = Color(0xFFFFE0B2) // Light amber text
    val EmergencyTextSecondary = Color(0xFFFFCC80) // Chip text & subtitles
    val EmergencyChipBackground = Color(0xFF4E342E) // Dark brown chip background
    val EmergencyChipBackgroundDark = Color(0xFF3E2723)
    val EmergencyCardBackground = Color(0xFF251E14) // Dark card surface for emergency list items
    val EmergencyTranslucent = Color(0x33FFB74D)

    // Status: Destructive / Danger / Deletion
    val DestructiveRed = Color(0xFFEF5350) // Delete buttons, error text
    val CrisisStopContainer = Color(0xFF7F1D1D) // Deep red container
    val CrisisStopText = Color(0xFFFFEBEE) // Pale pink text
    val RecordingPulseRed = Color(0xFFC62828) // Listening pulse animation
    val RecordingDotRed = Color(0xFFE53935) // Recording active indicator

    // Status: Success / Found / Active
    val SuccessGreen = Color(0xFF2E7D32) // Checkmarks & success icons
    val SuccessContainer = Color(0xFF1B5E20) // Deep green container
    val SuccessText = Color(0xFFE8F5E9) // Pale green text
    val ActiveGreen = Color(0xFF4CAF50) // Online/ready status
    val DownloadedGreen = Color(0xFF81C784) // Downloaded indicator

    // Status: Warning / Attention
    val WarningOrange = Color(0xFFFFA726) // Amber/orange warnings
    val WarningAmber = Color(0xFFFFC107) // Amber initializing pill
    val WarningHighContainer = Color(0xFFE65100) // Deep orange
    val WarningHighText = Color(0xFFFFF3E0) // Light orange text

    // Neutral dark controls
    val NeutralDarkContainer = Color(0xFF424242)
    val NeutralDarkText = Color(0xFFEEEEEE)
    val NeutralStoppedContainer = Color(0xFF37474F)
    val NeutralStoppedText = Color(0xFFECEFF1)
    val NeutralNotFoundContainer = Color(0xFF5D4037)
    val NeutralNotFoundText = Color(0xFFEFEBE9)

    // Pain / Discomfort 5-Level Intensity Scale
    val IntensityMild = Color(0xFF2E7D32)
    val IntensityLight = Color(0xFF558B2F)
    val IntensityModerate = Color(0xFFF9A825)
    val IntensityHigh = Color(0xFFE65100)
    val IntensitySevere = Color(0xFFB71C1C)
}
