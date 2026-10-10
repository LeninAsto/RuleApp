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
    val dark = when (preferences?.theme) { ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; else -> isSystemInDarkTheme() }
    val accent = when(source) {
        Source.SHIMMIE -> if(dark) Color(0xFFD1BCFF) else Color(0xFF6750A4)
        Source.PAHEAL -> if(dark) Color(0xFFF6BD70) else Color(0xFF805600)
        Source.RULE34 -> if(dark) Color(0xFF9BD67D) else Color(0xFF386A20)
        Source.DANBOORU -> if(dark) Color(0xFFA7C8FF) else Color(0xFF285EA6)
    }
    val neutral = if (dark) Color(0xFF141218) else Color(0xFFFDF8FF)
    val tonal = accent.copy(alpha = if (dark) 0.24f else 0.14f).compositeOver(neutral)
    fun ColorScheme.siteTones() = copy(primaryContainer = tonal, onPrimaryContainer = accent,
        secondary = accent, secondaryContainer = tonal, onSecondaryContainer = accent,
        tertiary = accent, tertiaryContainer = tonal, onTertiaryContainer = accent)
    val color = when {
        preferences?.dynamicColor != false && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> darkColorScheme(primary = accent, onPrimary = Color(0xFF171B12), background = neutral, surface = neutral).siteTones()
        else -> lightColorScheme(primary = accent, onPrimary = Color.White, background = neutral, surface = neutral).siteTones()
    }
    MaterialTheme(colorScheme = color, shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)), content = content)
}
