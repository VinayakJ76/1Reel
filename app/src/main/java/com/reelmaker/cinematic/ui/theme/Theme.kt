package com.reelmaker.cinematic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CinematicColors = darkColorScheme(
    primary = Violet,
    secondary = Magenta,
    tertiary = Mint,
    background = Midnight,
    surface = Slate
)

@Composable
fun CinematicReelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CinematicColors,
        typography = Typography,
        content = content
    )
}
