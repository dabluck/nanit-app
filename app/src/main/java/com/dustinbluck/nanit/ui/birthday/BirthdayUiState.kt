package com.dustinbluck.nanit.ui.birthday

import java.io.File

sealed interface BirthdayUiState {
    data object Loading : BirthdayUiState

    data object Error : BirthdayUiState

    data class Loaded(
        val name: String,
        val age: Age,
        val photo: File?
    ) : BirthdayUiState
}
