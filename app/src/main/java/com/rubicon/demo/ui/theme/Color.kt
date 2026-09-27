package com.rubicon.demo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Rubicon palette: crimson primary, plum secondary, gold tertiary.
val Crimson = Color(0xFF8C1D33)
val CrimsonDark = Color(0xFF560A1D)
val CrimsonContainer = Color(0xFFFFD9DD)
val Plum = Color(0xFF6B4E52)
val PlumContainer = Color(0xFFF4DDE0)
val Gold = Color(0xFF7B5800)
val GoldContainer = Color(0xFFFFDF9E)
val Paper = Color(0xFFFFF8F6)
val PaperVariant = Color(0xFFF3DDDF)
val Ink = Color(0xFF201A1B)
val InkVariant = Color(0xFF524345)
val Outline = Color(0xFF847375)

val LightColors = lightColorScheme(
    primary = Crimson,
    onPrimary = Color.White,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = Color(0xFF3B0715),
    secondary = Plum,
    onSecondary = Color.White,
    secondaryContainer = PlumContainer,
    onSecondaryContainer = Color(0xFF261012),
    tertiary = Gold,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = Color(0xFF271900),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperVariant,
    onSurfaceVariant = InkVariant,
    outline = Outline,
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB2B9),
    onPrimary = CrimsonDark,
    primaryContainer = Color(0xFF71152A),
    onPrimaryContainer = CrimsonContainer,
    secondary = Color(0xFFE5BDC1),
    onSecondary = Color(0xFF3C2226),
    secondaryContainer = Color(0xFF52353A),
    onSecondaryContainer = PlumContainer,
    tertiary = Color(0xFFE9C36A),
    onTertiary = Color(0xFF412D00),
    tertiaryContainer = Color(0xFF5C4200),
    onTertiaryContainer = GoldContainer,
    background = Color(0xFF1A1113),
    onBackground = Color(0xFFF0DEE0),
    surface = Color(0xFF1A1113),
    onSurface = Color(0xFFF0DEE0),
    surfaceVariant = InkVariant,
    onSurfaceVariant = Color(0xFFD7C1C4),
    outline = Color(0xFF9F8C8E),
)
