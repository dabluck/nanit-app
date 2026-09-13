package com.dustinbluck.nanit

sealed interface Screen {
    data object Main : Screen
    data object Birthday : Screen
}
