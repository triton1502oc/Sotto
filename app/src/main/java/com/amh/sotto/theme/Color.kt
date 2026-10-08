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

    // Emergency / Safety tokens
    val EmergencyAccent = Color(0xFFFFB74D) // Amber accent, borders, primary emergency buttons
    val EmergencyContainer = Color(0xFF1E1710) // Dark warm container for emergency cards
    val EmergencyText = Color(0xFFFFE0B2) // Light amber text
    val EmergencyTextSecondary = Color(0xFFFFCC80) // Chip text & subtitles
    val EmergencyChipBackground = Color(0xFF4E342E) // Dark brown chip background
    val EmergencyChipBackgroundDark = Color(0xFF3E2723)
    val EmergencyCardBackground = Color(0xFF251E14) // Dark card surface for emergency list items
    val EmergencyTranslucent = Color(0x33FFB74D)

    // Safety semantic aliases
    val SafetyAccent = EmergencyAccent
    val SafetyContainer = EmergencyContainer
    val SafetyText = EmergencyText
    val SafetyTextSecondary = EmergencyTextSecondary
    val SafetyChipBackground = EmergencyChipBackground
    val SafetyChipBackgroundDark = EmergencyChipBackgroundDark
    val SafetyCardBackground = EmergencyCardBackground
    val SafetyTranslucent = EmergencyTranslucent

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

/**
 * Curated sensory-minded theme palettes.
 */
object SottoPalettes {
    // Charcoal (Default Dark)
    val CharcoalBackground = Color(0xFF121212)
    val CharcoalSurface = Color(0xFF1E1E1E)
    val CharcoalSurfaceVariant = Color(0xFF2A2A2A)
    val CharcoalTextPrimary = Color(0xFFE0E0E0)
    val CharcoalTextSecondary = Color(0xFFAAAAAA)
    val CharcoalOutlineVariant = Color(0xFF424242)

    // OLED Pure Black
    val OledBlackBackground = Color(0xFF000000)
    val OledBlackSurface = Color(0xFF121212)
    val OledBlackSurfaceVariant = Color(0xFF1E1E1E)
    val OledBlackTextPrimary = Color(0xFFFFFFFF)
    val OledBlackTextSecondary = Color(0xFFB0B0B0)
    val OledBlackOutlineVariant = Color(0xFF333333)

    // Warm Amber / Low Strain (Dark Sepia)
    val WarmAmberBackground = Color(0xFF191512)
    val WarmAmberSurface = Color(0xFF241E1A)
    val WarmAmberSurfaceVariant = Color(0xFF322A24)
    val WarmAmberTextPrimary = Color(0xFFEAE0D5)
    val WarmAmberTextSecondary = Color(0xFFBFAFA0)
    val WarmAmberAccent = Color(0xFFD4A373)
    val WarmAmberOutlineVariant = Color(0xFF483C34)

    // Slate Navy
    val SlateNavyBackground = Color(0xFF0F141C)
    val SlateNavySurface = Color(0xFF161E2A)
    val SlateNavySurfaceVariant = Color(0xFF202A3A)
    val SlateNavyTextPrimary = Color(0xFFDEE3EB)
    val SlateNavyTextSecondary = Color(0xFFA8B4C4)
    val SlateNavyAccent = Color(0xFF8FA8C8)
    val SlateNavyOutlineVariant = Color(0xFF2B3A50)

    // Soft Parchment (Daylight / Anti-Halation)
    val SoftParchmentBackground = Color(0xFFF5F2EB)
    val SoftParchmentSurface = Color(0xFFEAE5DB)
    val SoftParchmentSurfaceVariant = Color(0xFFDFD9CE)
    val SoftParchmentTextPrimary = Color(0xFF1A1A1A)
    val SoftParchmentTextSecondary = Color(0xFF5A5752)
    val SoftParchmentAccent = Color(0xFF4A463F)
    val SoftParchmentOutlineVariant = Color(0xFFCCC5B8)
}
