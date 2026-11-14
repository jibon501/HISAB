package com.hisab.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hisab.app.R

// ---------- রঙের পাতা (স্লেট + পান্না সবুজ ডিজাইন) ----------
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate600 = Color(0xFF475569)
val Slate700 = Color(0xFF334155)
val Slate900 = Color(0xFF0F172A)

val Emerald50 = Color(0xFFECFDF5)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald200 = Color(0xFFA7F3D0)
val Emerald600 = Color(0xFF059669)
val Emerald700 = Color(0xFF047857)
val Emerald800 = Color(0xFF065F46)
val Teal700 = Color(0xFF0F766E)

val Red50 = Color(0xFFFEF2F2)
val Red100 = Color(0xFFFEE2E2)
val Red600 = Color(0xFFDC2626)
val Red700 = Color(0xFFB91C1C)

val Purple50 = Color(0xFFFAF5FF)
val Purple100 = Color(0xFFF3E8FF)
val Purple600 = Color(0xFF9333EA)
val Purple700 = Color(0xFF7E22CE)

val Indigo50 = Color(0xFFEEF2FF)
val Indigo100 = Color(0xFFE0E7FF)
val Indigo600 = Color(0xFF4F46E5)

val Amber50 = Color(0xFFFFFBEB)
val Amber100 = Color(0xFFFEF3C7)
val Amber200 = Color(0xFFFDE68A)
val Amber700 = Color(0xFFB45309)
val Amber800 = Color(0xFF92400E)
val Amber900 = Color(0xFF78350F)

val Blue50 = Color(0xFFEFF6FF)
val Blue600 = Color(0xFF2563EB)
val Blue700 = Color(0xFF1D4ED8)

private val HisabColors = lightColorScheme(
    primary = Emerald600,
    onPrimary = Color.White,
    primaryContainer = Emerald50,
    onPrimaryContainer = Emerald800,
    secondary = Blue600,
    onSecondary = Color.White,
    secondaryContainer = Blue50,
    onSecondaryContainer = Blue700,
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Amber50,
    onTertiaryContainer = Amber900,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Red600,
    onError = Color.White,
    errorContainer = Red50,
    onErrorContainer = Red700,
    // ডায়ালগ ও ফর্মের ব্যাকগ্রাউন্ড সাদা (লেখা-কাটা বক্সের রঙও এটাই)
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Slate100,
)

/** হিন্দ শিলিগুড়ি ফন্ট: বাংলা লেখা সব ফোনে একই রকম সুন্দর দেখায় */
val HindSiliguri = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal),
    Font(R.font.hind_siliguri, FontWeight.Medium),
    Font(R.font.hind_siliguri_semibold, FontWeight.SemiBold),
    Font(R.font.hind_siliguri_bold, FontWeight.Bold),
    Font(R.font.hind_siliguri_bold, FontWeight.ExtraBold),
)

private fun TextStyle.hind() = copy(fontFamily = HindSiliguri)

private val HisabTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.hind(),
        displayMedium = t.displayMedium.hind(),
        displaySmall = t.displaySmall.hind(),
        headlineLarge = t.headlineLarge.hind(),
        headlineMedium = t.headlineMedium.hind(),
        headlineSmall = t.headlineSmall.hind(),
        titleLarge = t.titleLarge.hind().copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
        titleMedium = t.titleMedium.hind().copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
        titleSmall = t.titleSmall.hind().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.hind().copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodyMedium = t.bodyMedium.hind().copy(fontSize = 13.sp, lineHeight = 18.sp),
        bodySmall = t.bodySmall.hind(),
        labelLarge = t.labelLarge.hind().copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
        labelMedium = t.labelMedium.hind().copy(fontWeight = FontWeight.SemiBold),
        labelSmall = t.labelSmall.hind(),
    )
}

/** স্লেট-পান্না রঙের হালকা থিম ও হিন্দ শিলিগুড়ি ফন্ট */
@Composable
fun HisabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HisabColors,
        typography = HisabTypography,
        content = content,
    )
}
