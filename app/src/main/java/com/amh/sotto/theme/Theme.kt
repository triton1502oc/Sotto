package com.amh.sotto.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.amh.sotto.data.VoiceSettings

val CharcoalColorScheme: ColorScheme = darkColorScheme(
    primary = SottoPalettes.CharcoalTextPrimary,
    onPrimary = SottoPalettes.CharcoalBackground,
    background = SottoPalettes.CharcoalBackground,
    onBackground = SottoPalettes.CharcoalTextPrimary,
    surface = SottoPalettes.CharcoalSurface,
    onSurface = SottoPalettes.CharcoalTextPrimary,
    surfaceVariant = SottoPalettes.CharcoalSurfaceVariant,
    onSurfaceVariant = SottoPalettes.CharcoalTextSecondary,
    outlineVariant = SottoPalettes.CharcoalOutlineVariant
)

val OledBlackColorScheme: ColorScheme = darkColorScheme(
    primary = SottoPalettes.OledBlackTextPrimary,
    onPrimary = SottoPalettes.OledBlackBackground,
    background = SottoPalettes.OledBlackBackground,
    onBackground = SottoPalettes.OledBlackTextPrimary,
    surface = SottoPalettes.OledBlackSurface,
    onSurface = SottoPalettes.OledBlackTextPrimary,
    surfaceVariant = SottoPalettes.OledBlackSurfaceVariant,
    onSurfaceVariant = SottoPalettes.OledBlackTextSecondary,
    outlineVariant = SottoPalettes.OledBlackOutlineVariant
)

val WarmAmberColorScheme: ColorScheme = darkColorScheme(
    primary = SottoPalettes.WarmAmberAccent,
    onPrimary = SottoPalettes.WarmAmberBackground,
    background = SottoPalettes.WarmAmberBackground,
    onBackground = SottoPalettes.WarmAmberTextPrimary,
    surface = SottoPalettes.WarmAmberSurface,
    onSurface = SottoPalettes.WarmAmberTextPrimary,
    surfaceVariant = SottoPalettes.WarmAmberSurfaceVariant,
    onSurfaceVariant = SottoPalettes.WarmAmberTextSecondary,
    outlineVariant = SottoPalettes.WarmAmberOutlineVariant
)

val SlateNavyColorScheme: ColorScheme = darkColorScheme(
    primary = SottoPalettes.SlateNavyAccent,
    onPrimary = SottoPalettes.SlateNavyBackground,
    background = SottoPalettes.SlateNavyBackground,
    onBackground = SottoPalettes.SlateNavyTextPrimary,
    surface = SottoPalettes.SlateNavySurface,
    onSurface = SottoPalettes.SlateNavyTextPrimary,
    surfaceVariant = SottoPalettes.SlateNavySurfaceVariant,
    onSurfaceVariant = SottoPalettes.SlateNavyTextSecondary,
    outlineVariant = SottoPalettes.SlateNavyOutlineVariant
)

val SoftParchmentColorScheme: ColorScheme = lightColorScheme(
    primary = SottoPalettes.SoftParchmentAccent,
    onPrimary = SottoPalettes.SoftParchmentBackground,
    background = SottoPalettes.SoftParchmentBackground,
    onBackground = SottoPalettes.SoftParchmentTextPrimary,
    surface = SottoPalettes.SoftParchmentSurface,
    onSurface = SottoPalettes.SoftParchmentTextPrimary,
    surfaceVariant = SottoPalettes.SoftParchmentSurfaceVariant,
    onSurfaceVariant = SottoPalettes.SoftParchmentTextSecondary,
    outlineVariant = SottoPalettes.SoftParchmentOutlineVariant
)

@Composable
fun SottoAppTheme(
    appTheme: String = VoiceSettings.THEME_CHARCOAL,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        VoiceSettings.THEME_OLED_BLACK -> OledBlackColorScheme
        VoiceSettings.THEME_WARM_AMBER -> WarmAmberColorScheme
        VoiceSettings.THEME_SLATE_NAVY -> SlateNavyColorScheme
        VoiceSettings.THEME_SOFT_PARCHMENT -> SoftParchmentColorScheme
        else -> CharcoalColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
