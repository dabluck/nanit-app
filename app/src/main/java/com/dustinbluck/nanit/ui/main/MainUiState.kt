package com.dustinbluck.nanit.ui.main

import com.dustinbluck.nanit.data.Baby

sealed interface MainUiState {
    data object Loading : MainUiState

    data object Error : MainUiState

    data class Loaded(
        val baby: Baby,
        val nameEditState: NameEditState
    ) : MainUiState {
        val isBirthdayEnabled: Boolean
            get() = !baby.name.isNullOrBlank() && baby.birthday != null
    }
}
