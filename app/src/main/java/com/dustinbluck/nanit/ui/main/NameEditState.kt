package com.dustinbluck.nanit.ui.main

sealed interface NameEditState {
    data object Closed : NameEditState

    data class Open(
        val isSaving: Boolean,
        val saveFailed: Boolean
    ) : NameEditState
}
