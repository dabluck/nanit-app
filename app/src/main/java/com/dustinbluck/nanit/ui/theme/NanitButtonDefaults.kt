package com.dustinbluck.nanit.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object NanitButtonDefaults {
    val ContainerHeight = 48.dp

    val ContentPadding = PaddingValues(
        horizontal = 24.dp,
        vertical = 12.dp
    )

    val IconSize: Dp
        @Composable get() = ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)

    val TextStyle: TextStyle
        @Composable get() = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight)
}
