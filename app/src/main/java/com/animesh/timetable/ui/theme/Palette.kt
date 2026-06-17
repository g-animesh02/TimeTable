package com.animesh.timetable.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Deterministic color for a label (subject or professor), so the same name always maps
 * to the same color. Lightness adapts to the theme so text stays readable on both.
 */
fun colorForKey(key: String, dark: Boolean): Color {
    if (key.isBlank()) return if (dark) Color(0xFFB8B8C2) else Color(0xFF6B6B76)
    // Spread the hash across the hue wheel; the golden-ratio step keeps nearby names distinct.
    val h = abs(key.hashCode())
    val hue = (h * 137.508) % 360.0
    val saturation = 0.58f
    val lightness = if (dark) 0.70f else 0.40f
    return Color.hsl(hue.toFloat(), saturation, lightness)
}
