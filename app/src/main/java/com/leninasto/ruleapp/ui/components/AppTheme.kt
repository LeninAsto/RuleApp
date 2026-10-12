package com.leninasto.ruleapp.ui.components

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leninasto.ruleapp.data.local.AppPreferences
import com.leninasto.ruleapp.data.local.ThemeMode
import com.leninasto.ruleapp.domain.Source

@Composable
fun AppTheme(preferences: AppPreferences?, source: Source = Source.SHIMMIE, content: @Composable () -> Unit) {
    com.leninasto.ruleapp.core.AppStrings.initialize(LocalContext.current.applicationContext)
    val dark = when (preferences?.theme) { ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; else -> isSystemInDarkTheme() }
    val accent = if (preferences?.sourceAccent != true) { if (dark) Color(0xFFE2BEA7) else Color(0xFF805B43) } else when(source) {
        Source.SHIMMIE -> if(dark) Color(0xFFD1BCFF) else Color(0xFF6750A4)
        Source.PAHEAL -> if(dark) Color(0xFFF6BD70) else Color(0xFF805600)
        Source.RULE34 -> if(dark) Color(0xFF9BD67D) else Color(0xFF386A20)
        Source.DANBOORU -> if(dark) Color(0xFFA7C8FF) else Color(0xFF285EA6)
    }
    val neutral = if (dark) Color(0xFF191510) else Color(0xFFFFF8F2)
    val tonal = accent.copy(alpha = if (dark) 0.24f else 0.14f).compositeOver(neutral)
    fun ColorScheme.siteTones() = copy(primaryContainer = tonal, onPrimaryContainer = accent,
        secondary = accent, secondaryContainer = tonal, onSecondaryContainer = accent,
        tertiary = accent, tertiaryContainer = tonal, onTertiaryContainer = accent,
        onSurface = if (dark) Color(0xFFF0E0D2) else Color(0xFF2A201A),
        onSurfaceVariant = if (dark) Color(0xFFD2BFB0) else Color(0xFF59483B),
        surfaceVariant = if (dark) Color(0xFF3B332D) else Color(0xFFE8DBCF),
        surfaceContainerLowest = if (dark) Color(0xFF100D0A) else Color.White,
        surfaceContainerLow = if (dark) Color(0xFF201A16) else Color(0xFFFCF1E7),
        surfaceContainer = if (dark) Color(0xFF251F1A) else Color(0xFFF5E9DF),
        surfaceContainerHigh = if (dark) Color(0xFF302923) else Color(0xFFEEE1D6),
        surfaceContainerHighest = if (dark) Color(0xFF3B332D) else Color(0xFFE8DBCF),
        surfaceDim = if (dark) neutral else Color(0xFFE6D7CB),
        surfaceBright = if (dark) Color(0xFF403731) else neutral,
        outline = if (dark) Color(0xFFAB9686) else Color(0xFF897567))
    val color = when {
        preferences?.dynamicColor == true && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> darkColorScheme(primary = accent, onPrimary = Color(0xFF332012), background = neutral, surface = neutral).siteTones()
        else -> lightColorScheme(primary = accent, onPrimary = Color.White, background = neutral, surface = neutral).siteTones()
    }
    MaterialTheme(colorScheme = color, shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)), content = content)
}
