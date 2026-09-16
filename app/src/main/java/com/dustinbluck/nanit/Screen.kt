package com.dustinbluck.nanit

import com.dustinbluck.nanit.ui.birthday.BirthdayMode
import java.io.Serializable

sealed interface Screen : Serializable {
    data object Main : Screen
    data class Birthday(val mode: BirthdayMode) : Screen
}
