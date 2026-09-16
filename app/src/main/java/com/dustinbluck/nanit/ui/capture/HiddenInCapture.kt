package com.dustinbluck.nanit.ui.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

val LocalCaptureAlpha = compositionLocalOf { 1f }


// handy way to hide things on the share page without duplicating view hierarchy
@Composable
fun Modifier.hiddenInCapture(): Modifier {
    val captureAlpha = LocalCaptureAlpha.current
    return graphicsLayer {
        alpha = captureAlpha
    }
}
