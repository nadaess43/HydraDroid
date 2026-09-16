package com.hydradroid.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Точная копия src/renderer/src/scss/globals.scss ──
object HydraColors {
    val Background = Color(0xFF121212)        // $background-color
    val DarkBackground = Color(0xFF0D0D0D)    // $dark-background-color
    val Muted = Color(0xFFF0F1F7)             // $muted-color (заголовки/иконки)
    val Body = Color(0xFFD0D1D7)              // $body-color (текст)
    val TextBright = Color(0xFFDADBE1)        // game-card content, hero, header input
    val Border = Color(0x14FFFFFF)            // rgba(255,255,255,0.08)
    val Success = Color(0xFF1C9749)
    val Danger = Color(0xFF801D1E)
    val Error = Color(0xFFE11D48)
    val Warning = Color(0xFFFFC107)
    val BrandTeal = Color(0xFF16B195)         // $brand-teal
    val BrandBlue = Color(0xFF3E62C0)         // $brand-blue
    val PlatinumCyan = Color(0xFF30EDC5)
    val ClassicsWashed = Color(0xFFF6746B)
    val SkeletonBase = Color(0xFF1C1C1C)
    val SkeletonHighlight = Color(0xFF444444)
    val ScrollbarThumb = Color(0x26FFFFFF)
    val SecondaryText60 = Color(0x99FFFFFF)
    val SecondaryText50 = Color(0x80FFFFFF)
    val ActiveMenuItem = Color(0x1AFFFFFF)     // rgba(255,255,255,0.1)
    val HoverMenuItem = Color(0x26FFFFFF)      // 0.15
    val GameBadgeGreen = Color(0x2622C55E)    // rgba(34,197,94,0.15)
    val ProtonPink = Color(0xFFF50057)
    val OnPrimaryLight = Color(0xFF121212)       // текст на primary-кнопке (button--primary: muted bg)
    val DisabledOpacity = 0.5f                  // $disabled-opacity
    val ActiveOpacity = 0.7f                    // $active-opacity
    val BadgeBackground = Color(0x1AFFFFFF)      // badge.scss: rgba(255,255,255,0.1)
    val BadgeBorder = Color(0x33FFFFFF)          // badge.scss: rgba(255,255,255,0.2)

    // Градиенты оригинала
    // button--cloud: linear-gradient(133deg, #14b9bb 0%, #0a86d9 100%)
    val CloudButtonGradient = Brush.linearGradient(
        listOf(Color(0xFF14B9BB), Color(0xFF0A86D9))
    )
    val CloudGradient = Brush.linearGradient(listOf(BrandTeal, BrandBlue))
    val CloudGradient270 = Brush.linearGradient(listOf(Color(0xFF16B195), Color(0xFF3E62C0)))
    val HeroBackdrop = Brush.verticalGradient(
        0.25f to Color(0x00000000), 1f to Color(0xCC000000)
    )
    val GameCardBackdrop = Brush.verticalGradient(
        0.5f to Color(0x00000000), 1f to Color(0xB3000000)
    )
    val ContentBackground = Brush.verticalGradient(
        listOf(DarkBackground, Background)
    )
    val ClassicsRainbow = Brush.sweepGradient(
        listOf(
            Color(0xFFFB0026), Color(0xFFC80078), Color(0xFF7300A4),
            Color(0xFFF8C802), Color(0xFFFC5812), Color(0xFFFB0026)
        )
    )
}

object HydraDimens {
    const val RadiusCard = 4          // game-card, modal, sidebar item
    const val RadiusBadge = 6
    const val RadiusButton = 8        // button, header search, classics card
    const val SpacingUnit = 8
    const val BodyFontSize = 14
    const val HintFontSize = 13
    const val SmallFontSize = 12
    const val SidebarWidthDp = 250
    const val HeaderHeightDp = 64
    const val GameCardHeightDp = 180
    const val HeroHeightCompact = 180
    const val HeroHeightExpanded = 400
}
