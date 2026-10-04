package com.amh.sotto.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun SottoAppTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = SottoColors.TextPrimary,
        background = SottoColors.DarkBackground,
        surface = SottoColors.DarkSurface,
        onPrimary = SottoColors.DarkBackground,
        onBackground = SottoColors.TextPrimary,
        onSurface = SottoColors.TextPrimary
    )
    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}
