package com.dustinbluck.nanit.ui.photo

sealed interface PhotoEditState {
    data object Closed : PhotoEditState

    data class Open(
        val isSaving: Boolean,
        val saveFailed: Boolean
    ) : PhotoEditState
}
