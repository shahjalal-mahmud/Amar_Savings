package com.appriyo.amarsavings.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
//  AmarSavings — Premium Palette
//  Sophisticated indigo → violet primary with warm amber & coral accents.
// ─────────────────────────────────────────────────────────────────────────────

// Primary brand — Indigo / Violet
val Indigo50  = Color(0xFFF3F4FF)
val Indigo100 = Color(0xFFE5E8FF)
val Indigo200 = Color(0xFFC8CFFF)
val Indigo300 = Color(0xFFA4A8FF)
val Indigo400 = Color(0xFF8189FF)
val Indigo500 = Color(0xFF6C63FF)   // Signature
val Indigo600 = Color(0xFF5547E8)
val Indigo700 = Color(0xFF4034C2)
val Indigo800 = Color(0xFF2E2596)
val Indigo900 = Color(0xFF1E1868)

// Secondary — Violet
val Violet400 = Color(0xFFB48BFF)
val Violet500 = Color(0xFF9D6BFF)
val Violet600 = Color(0xFF7E47E8)

// Tertiary — Amber (positive accent)
val Amber200 = Color(0xFFFFE6B0)
val Amber300 = Color(0xFFFFD27A)
val Amber400 = Color(0xFFFFC247)
val Amber500 = Color(0xFFF5A623)
val Amber600 = Color(0xFFD88A0E)

// Positive — Mint (savings)
val Mint400 = Color(0xFF4FD1B5)
val Mint500 = Color(0xFF2EB89A)
val Mint600 = Color(0xFF1E947D)
val Mint100 = Color(0xFFD9F7EF)

// Negative — Coral (withdrawal)
val Coral300 = Color(0xFFFF8A95)
val Coral400 = Color(0xFFFF6B7A)
val Coral500 = Color(0xFFE94B5C)
val Coral600 = Color(0xFFC73849)
val Coral100 = Color(0xFFFFE4E7)

// Neutrals — Cool gray
val Gray0   = Color(0xFFFFFFFF)
val Gray25  = Color(0xFFFBFBFD)
val Gray50  = Color(0xFFF7F8FA)
val Gray75  = Color(0xFFF1F2F6)
val Gray100 = Color(0xFFE7E9EE)
val Gray150 = Color(0xFFD7DAE0)
val Gray200 = Color(0xFFC5C9D1)
val Gray300 = Color(0xFFA1A6B2)
val Gray400 = Color(0xFF7A7F8C)
val Gray500 = Color(0xFF5B6070)
val Gray600 = Color(0xFF444858)
val Gray700 = Color(0xFF2E3240)
val Gray750 = Color(0xFF252A36)
val Gray800 = Color(0xFF1C2030)
val Gray850 = Color(0xFF161A26)
val Gray900 = Color(0xFF10131C)
val Gray950 = Color(0xFF0A0C14)

// Dark Surfaces
val SurfaceDarkBase   = Color(0xFF0B0E17)   // App background
val SurfaceDarkLow    = Color(0xFF11151F)   // Slightly raised
val SurfaceDark       = Color(0xFF161B27)   // Cards
val SurfaceDarkHigh   = Color(0xFF1D2332)   // Elevated
val SurfaceDarkHigher = Color(0xFF252B3D)   // Popovers

// Dark container variants for surface tints (used in Theme.kt)
val DarkContainerPrimary    = Color(0xFF22264A)
val DarkContainerSecondary  = Color(0xFF322447)
val DarkContainerTertiary   = Color(0xFF3D2F12)
val DarkContainerError      = Color(0xFF3A1A21)
val DarkOutline             = Color(0xFF363D52)
val DarkOutlineVariant      = Color(0xFF222838)

// Light container variants
val LightContainerSecondary = Color(0xFFEFE8FF)
val LightContainerTertiary  = Color(0xFFFFF4DA)
val LightOnTertiaryContainer = Color(0xFF6B440A)

// Light Surfaces
val SurfaceLightBase   = Color(0xFFFAFAFC)
val SurfaceLightLow    = Color(0xFFFFFFFF)
val SurfaceLight       = Color(0xFFFFFFFF)
val SurfaceLightHigh   = Color(0xFFF5F5F9)
val SurfaceLightHigher = Color(0xFFEBECF2)

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

// ─────────────────────────────────────────────────────────────────────────────
//  Gradients — eagerly evaluated to avoid per-recomposition allocation
// ─────────────────────────────────────────────────────────────────────────────

/** Signature hero gradient — Indigo → Violet (light theme) */
val GradientHeroLight: Brush = Brush.linearGradient(
    colors = listOf(Indigo500, Violet500)
)

/** Signature hero gradient — Indigo → Violet (dark theme) */
val GradientHeroDark: Brush = Brush.linearGradient(
    colors = listOf(Indigo600, Violet600)
)