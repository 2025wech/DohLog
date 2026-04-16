package com.autoledger.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.autoledger.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular,  FontWeight.Normal),
    Font(R.font.inter_medium,   FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold,     FontWeight.Bold),
)

val AutoledgerTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 24.sp,          // was 32.sp
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 20.sp,          // was 24.sp
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp              // was 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp              // was 15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 13.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp              // was 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp              // was 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp
    ),
    labelLarge = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 13.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 10.sp,          // was 11.sp
        letterSpacing = 0.3.sp
    )
)