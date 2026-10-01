package com.rubicon.demo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Rubicon palette: a light sky blue, a slate blue and a teal accent.
val Sky = Color(0xFF2E7CF6)
val SkyContainer = Color(0xFFDCE9FF)
val Slate = Color(0xFF4F6B92)
val SlateContainer = Color(0xFFDDE6F4)
val Teal = Color(0xFF00897B)
val TealContainer = Color(0xFFC9F1EB)
val Amber = Color(0xFFB26A00)
val Paper = Color(0xFFF6F9FE)

/** Status colors for event updates and log levels, the same in both themes. */
val Success = Color(0xFF2E9E62)
val Warning = Color(0xFFE09B1B)
val Danger = Color(0xFFD64545)

val LightColors =
    lightColorScheme(
        primary = Sky,
        onPrimary = Color.White,
        primaryContainer = SkyContainer,
        onPrimaryContainer = Color(0xFF0B2A5B),
        secondary = Slate,
        onSecondary = Color.White,
        secondaryContainer = SlateContainer,
        onSecondaryContainer = Color(0xFF13233B),
        tertiary = Teal,
        onTertiary = Color.White,
        tertiaryContainer = TealContainer,
        onTertiaryContainer = Color(0xFF00201C),
        background = Paper,
        onBackground = Color(0xFF151C27),
        surface = Paper,
        onSurface = Color(0xFF151C27),
        surfaceVariant = Color(0xFFE5EDF8),
        onSurfaceVariant = Color(0xFF46536A),
        surfaceContainer = Color(0xFFEDF3FC),
        surfaceContainerHigh = Color(0xFFE6EEF9),
        surfaceContainerLow = Color(0xFFF1F6FD),
        outline = Color(0xFF7B8798),
        outlineVariant = Color(0xFFC9D3E2),
        error = Danger,
    )

val DarkColors =
    darkColorScheme(
        primary = Color(0xFF9CC4FF),
        onPrimary = Color(0xFF0A2A57),
        primaryContainer = Color(0xFF17407E),
        onPrimaryContainer = SkyContainer,
        secondary = Color(0xFFB7C8E4),
        onSecondary = Color(0xFF203350),
        secondaryContainer = Color(0xFF344A6B),
        onSecondaryContainer = SlateContainer,
        tertiary = Color(0xFF6FD8C9),
        onTertiary = Color(0xFF003731),
        tertiaryContainer = Color(0xFF005047),
        onTertiaryContainer = TealContainer,
        background = Color(0xFF0E141D),
        onBackground = Color(0xFFE1E7F1),
        surface = Color(0xFF0E141D),
        onSurface = Color(0xFFE1E7F1),
        surfaceVariant = Color(0xFF1E2837),
        onSurfaceVariant = Color(0xFFBAC6D8),
        surfaceContainer = Color(0xFF172030),
        surfaceContainerHigh = Color(0xFF1E2837),
        surfaceContainerLow = Color(0xFF131B27),
        outline = Color(0xFF6F7C90),
        outlineVariant = Color(0xFF2E3A4C),
        error = Color(0xFFFF8A80),
    )
