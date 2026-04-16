package com.autoledger.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Brand colors ──────────────────────────────────────────────────────────
val SafaricomGreen    = Color(0xFF00B341)
val SafaricomGreenDim = Color(0xFF00802E)
val AirtelRed         = Color(0xFFFF0000)
val TrueBlack         = Color(0xFF000000)
val SurfaceBlack      = Color(0xFF080808)
val CardBlack         = Color(0xFF0D0D0D)
val BorderSubtle      = Color(0xFF1A1A1A)
val TextPrimary       = Color(0xFFFFFFFF)
val TextSecondary     = Color(0xFF888888)
val TextMuted         = Color(0xFF444444)
val ErrorRed          = Color(0xFFFF4D4D)

// ── White theme colors ────────────────────────────────────────────────────
val WhiteBackground   = Color(0xFFFFFFFF)
val WhiteSurface      = Color(0xFFF5F5F5)
val WhiteCard         = Color(0xFFEEEEEE)
val WhiteBorder       = Color(0xFFDDDDDD)
val WhiteTextPrimary  = Color(0xFF111111)
val WhiteTextSecondary= Color(0xFF555555)
val WhiteTextMuted    = Color(0xFF999999)

// ── Theme enum ────────────────────────────────────────────────────────────
enum class AppTheme { OLED_BLACK, WHITE }

// ── CompositionLocal for theme access ─────────────────────────────────────
val LocalAppTheme = compositionLocalOf { AppTheme.OLED_BLACK }

// ── OLED dark color scheme ────────────────────────────────────────────────
private val OledColorScheme = darkColorScheme(
    primary              = SafaricomGreen,
    onPrimary            = TrueBlack,
    primaryContainer     = Color(0xFF0D1F0D),
    onPrimaryContainer   = SafaricomGreen,
    background           = TrueBlack,
    onBackground         = TextPrimary,
    surface              = SurfaceBlack,
    onSurface            = TextPrimary,
    surfaceVariant       = CardBlack,
    onSurfaceVariant     = TextSecondary,
    outline              = BorderSubtle,
    error                = ErrorRed,
    onError              = TrueBlack,
)

// ── White light color scheme ──────────────────────────────────────────────
private val WhiteColorScheme = lightColorScheme(
    primary              = SafaricomGreen,
    onPrimary            = WhiteBackground,
    primaryContainer     = Color(0xFFE8F5E9),
    onPrimaryContainer   = Color(0xFF00401A),
    background           = WhiteBackground,
    onBackground         = WhiteTextPrimary,
    surface              = WhiteSurface,
    onSurface            = WhiteTextPrimary,
    surfaceVariant       = WhiteCard,
    onSurfaceVariant     = WhiteTextSecondary,
    outline              = WhiteBorder,
    error                = ErrorRed,
    onError              = WhiteBackground,
)

@Composable
fun AutoledgerTheme(
    appTheme : AppTheme = AppTheme.OLED_BLACK,
    content  : @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.OLED_BLACK -> OledColorScheme
        AppTheme.WHITE      -> WhiteColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AutoledgerTypography,
        content     = content
    )
}