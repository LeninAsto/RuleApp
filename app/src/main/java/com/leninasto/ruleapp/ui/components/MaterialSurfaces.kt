package com.leninasto.ruleapp.ui.components
import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.t

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

// Keep stable-library experimental surface opt-ins inside the design system.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: @Composable () -> Unit, navigationIcon: @Composable () -> Unit = {}, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(title = title, navigationIcon = navigationIcon, actions = actions,
        expandedHeight = if (LocalDensity.current.fontScale > 1.3f) 96.dp else 64.dp)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, content = content)
}

@Composable
fun AppLoadingIndicator() {
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = androidx.compose.ui.Modifier.size(48.dp),
        factory = { context ->
            com.google.android.material.progressindicator.CircularProgressIndicator(
                android.view.ContextThemeWrapper(context, com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar)
            ).apply {
                val density = resources.displayMetrics.density
                indicatorSize = (48 * density).toInt()
                indicatorInset = 0
                trackThickness = (4 * density).toInt()
                waveAmplitude = (2 * density).toInt()
                setWavelength((14 * density).toInt())
                isIndeterminate = true
                contentDescription = t(R.string.ui_loading)
            }
        },
        update = { view -> view.setIndicatorColor(color.toArgb()); view.trackColor = track.toArgb() }
    )
}
