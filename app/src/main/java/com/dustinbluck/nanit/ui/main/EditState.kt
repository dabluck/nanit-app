package com.dustinbluck.nanit.ui.main

sealed interface EditState {
    data object Closed : EditState

    data class Open(
        val field: EditField,
        val isSaving: Boolean,
        val saveFailed: Boolean
    ) : EditState
}
