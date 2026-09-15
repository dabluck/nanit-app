package com.dustinbluck.nanit

import com.dustinbluck.nanit.ui.birthday.BirthdayMode

sealed interface Screen {
    data object Main : Screen
    data class Birthday(val mode: BirthdayMode) : Screen
}
