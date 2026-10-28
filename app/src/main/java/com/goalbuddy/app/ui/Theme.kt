package com.goalbuddy.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goalbuddy.app.data.AppSettings

fun Long.toColor(): Color = Color(this)

fun Color.toArgbLong(): Long {
    val a = (alpha * 255f + 0.5f).toInt()
    val r = (red * 255f + 0.5f).toInt()
    val g = (green * 255f + 0.5f).toInt()
    val b = (blue * 255f + 0.5f).toInt()
    return ((a.toLong() and 0xFF) shl 24) or ((r.toLong() and 0xFF) shl 16) or ((g.toLong() and 0xFF) shl 8) or (b.toLong() and 0xFF)
}

data class Palette(
    val bg1: Color,
    val bg2: Color,
    val card: Color,
    val onBg: Color,
    val onCard: Color,
    val sub: Color,
    val accent: Color,
    val dark: Boolean,
    val corner: Dp
)

val LocalPalette = staticCompositionLocalOf {
    Palette(Color.White, Color.White, Color.White, Color.Black, Color.Black, Color.Gray, Color(0xFF7C4DFF), false, 18.dp)
}

data class ThemePreset(
    val name: String, val accent: Long, val bg1: Long, val bg2: Long, val card: Long, val cardOpacity: Float
)

val themePresets = listOf(
    ThemePreset("💜 Lavender", 0xFF7C4DFF, 0xFFF3EEFF, 0xFFFFE9F3, 0xFFFFFFFF, 0.85f),
    ThemePreset("🌙 Midnight", 0xFF8C9EFF, 0xFF0F1027, 0xFF2A1B4A, 0xFF23234A, 0.8f),
    ThemePreset("🌿 Mint", 0xFF00A884, 0xFFE6FFF6, 0xFFE3F4FF, 0xFFFFFFFF, 0.88f),
    ThemePreset("🌅 Sunset", 0xFFFF6E40, 0xFFFFF1E0, 0xFFFFD6E0, 0xFFFFFFFF, 0.85f),
    ThemePreset("🌊 Ocean", 0xFF0091EA, 0xFF03253F, 0xFF005B8C, 0xFF0B3C5D, 0.8f),
    ThemePreset("📄 Paper", 0xFF455A64, 0xFFFAFAF7, 0xFFF1EFE8, 0xFFFFFFFF, 1f),
    ThemePreset("🍬 Candy", 0xFFFF4081, 0xFFFFE4F1, 0xFFE4F3FF, 0xFFFFFFFF, 0.8f),
    ThemePreset("🌲 Forest", 0xFF66BB6A, 0xFF0E1F14, 0xFF1E3A26, 0xFF1F3527, 0.85f),
    ThemePreset("🖤 AMOLED", 0xFFFFD740, 0xFF000000, 0xFF000000, 0xFF151515, 1f),
    ThemePreset("🍑 Peach", 0xFFFF8A65, 0xFFFFF3EC, 0xFFFFF8E1, 0xFFFFFFFF, 0.9f)
)

fun AppSettings.applyPreset(i: Int): AppSettings {
    val p = themePresets[i]
    return copy(themePreset = i, accent = p.accent, bgColor1 = p.bg1, bgColor2 = p.bg2, cardColor = p.card, cardOpacity = p.cardOpacity)
}

@Composable
fun GoalTheme(st: AppSettings, content: @Composable () -> Unit) {
    val bg1 = st.bgColor1.toColor()
    val bg2 = if (st.bgStyle == 0) bg1 else st.bgColor2.toColor()
    val card = st.cardColor.toColor().copy(alpha = st.cardOpacity)
    val dark = (bg1.luminance() + bg2.luminance()) / 2f < 0.4f
    val cardDark = st.cardColor.toColor().luminance() < 0.4f
    val onBg = if (dark) Color(0xFFF5F3FF) else Color(0xFF1E1B2E)
    val onCard = if (cardDark) Color(0xFFF5F3FF) else Color(0xFF1E1B2E)
    val accent = st.accent.toColor()
    val pal = Palette(
        bg1 = bg1, bg2 = bg2, card = card, onBg = onBg, onCard = onCard,
        sub = onCard.copy(alpha = 0.6f), accent = accent, dark = dark, corner = st.cornerRadius.dp
    )
    val scheme = if (dark) darkColorScheme(
        primary = accent, secondary = accent, onPrimary = Color.White,
        background = bg1, surface = st.cardColor.toColor(), onSurface = onCard, onBackground = onBg,
        surfaceVariant = st.cardColor.toColor(), onSurfaceVariant = onCard.copy(alpha = 0.75f)
    ) else lightColorScheme(
        primary = accent, secondary = accent, onPrimary = Color.White,
        background = bg1, surface = st.cardColor.toColor(), onSurface = onCard, onBackground = onBg,
        surfaceVariant = Color(0xFFF1ECFA), onSurfaceVariant = onCard.copy(alpha = 0.75f)
    )
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalPalette provides pal,
        LocalDensity provides Density(density.density, density.fontScale * st.fontScale)
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
