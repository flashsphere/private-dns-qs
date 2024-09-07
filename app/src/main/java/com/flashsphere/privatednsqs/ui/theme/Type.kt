package com.flashsphere.privatednsqs.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val AppTypography = Typography()

val Monospace: FontFamily = try {
    FontFamily(Font(DeviceFontFamilyName("monospace"), FontWeight.Normal))
} catch (_: Exception) {
    FontFamily.SansSerif
}
