package com.animesh.timetable.ui.theme

import androidx.compose.ui.graphics.Color

// Minimalist indigo/violet accent on near-neutral surfaces.
val Indigo = Color(0xFF5B5BD6)
val IndigoDark = Color(0xFF3A3AA8)
val IndigoSoft = Color(0xFFE7E7FB)

val NeutralBg = Color(0xFFFAFAFC)
val NeutralSurface = Color(0xFFFFFFFF)
val NeutralLine = Color(0xFFEAEAF0)

val DarkBg = Color(0xFF111114)
val DarkSurface = Color(0xFF1B1B20)
val DarkLine = Color(0xFF2A2A31)

// Calm, distinct per-day accents (used as thin markers, not fills — minimalist).
val DayColors = listOf(
    Color(0xFF5B5BD6), // Monday   – indigo
    Color(0xFF2BA89E), // Tuesday  – teal
    Color(0xFFE08A2B), // Wednesday– amber
    Color(0xFF4F8EF7), // Thursday – blue
    Color(0xFFD45D9B), // Friday   – pink
    Color(0xFF6Fae46)  // Saturday – green
)

val Good = Color(0xFF2E9E5B)
val Warn = Color(0xFFE0A22B)
val Bad = Color(0xFFD4504E)
