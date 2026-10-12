package com.leninasto.ruleapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.leninasto.ruleapp.R

/** Render the supplied PNG layers; adaptive-icon XML is not a Compose painter. */
@Composable
fun AppLogo(size: Dp = 80.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(24.dp))) {
        Image(painterResource(R.mipmap.ic_launcher_background), null, Modifier.fillMaxSize())
        Image(painterResource(R.mipmap.ic_launcher_foreground), null, Modifier.fillMaxSize())
    }
}
