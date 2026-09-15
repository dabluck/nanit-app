package com.dustinbluck.nanit.ui.birthday

sealed interface BirthdayUiState {
    data object Loading : BirthdayUiState

    data object Error : BirthdayUiState

    data class Loaded(
        val name: String,
        val age: Age
    ) : BirthdayUiState
}
