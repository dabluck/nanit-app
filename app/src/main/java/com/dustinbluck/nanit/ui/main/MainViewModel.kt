package com.dustinbluck.nanit.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dustinbluck.nanit.data.Baby
import com.dustinbluck.nanit.data.BabyRepository
import com.dustinbluck.nanit.logging.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.LocalDate

class MainViewModel(
    private val babyRepository: BabyRepository,
    private val logger: Logger
) : ViewModel() {

    private val editState = MutableStateFlow<EditState>(EditState.Closed)

    val uiState: StateFlow<MainUiState> = combine<Baby, EditState, MainUiState>(
        babyRepository.baby,
        editState
    ) { baby, edit ->
        MainUiState.Loaded(
            baby = baby,
            editState = edit
        )
    }
        .catch { exception ->
            logger.e(
                TAG,
                LOAD_ERROR_LOG_MESSAGE,
                exception
            )
            emit(MainUiState.Error)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MainUiState.Loading
        )

    fun editName() {
        edit(EditField.NAME)
    }

    fun editBirthday() {
        edit(EditField.BIRTHDAY)
    }

    fun editPhoto() {
        edit(EditField.PHOTO)
    }

    fun cancelEdit() {
        editState.value = EditState.Closed
    }

    fun saveName(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isNotEmpty()) {
            save(
                field = EditField.NAME,
                errorLogMessage = NAME_SAVE_ERROR_LOG_MESSAGE
            ) {
                babyRepository.setName(trimmedName)
            }
        }
    }

    fun saveBirthday(birthday: LocalDate) {
        save(
            field = EditField.BIRTHDAY,
            errorLogMessage = BIRTHDAY_SAVE_ERROR_LOG_MESSAGE
        ) {
            babyRepository.setBirthday(birthday)
        }
    }

    fun savePhoto(openPhoto: () -> InputStream) {
        if (editState.value == EditState.Closed) {
            edit(EditField.PHOTO)
        }
        save(
            field = EditField.PHOTO,
            errorLogMessage = PHOTO_SAVE_ERROR_LOG_MESSAGE
        ) {
            babyRepository.setPhoto(openPhoto)
        }
    }

    private fun edit(field: EditField) {
        editState.value = EditState.Open(
            field = field,
            isSaving = false,
            saveFailed = false
        )
    }

    private fun save(
        field: EditField,
        errorLogMessage: String,
        write: suspend () -> Boolean
    ) {
        val currentState = editState.value
        if (currentState is EditState.Open && currentState.field == field && !currentState.isSaving) {
            editState.value = currentState.copy(
                isSaving = true,
                saveFailed = false
            )
            viewModelScope.launch {
                if (write()) {
                    editState.value = EditState.Closed
                } else {
                    logger.e(
                        TAG,
                        errorLogMessage
                    )
                    editState.update { state ->
                        if (state is EditState.Open && state.field == field) {
                            state.copy(
                                isSaving = false,
                                saveFailed = true
                            )
                        } else {
                            state
                        }
                    }
                }
            }
        }
    }

    private companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val TAG = "MainViewModel"
        private const val LOAD_ERROR_LOG_MESSAGE = "Failed to load baby"
        private const val NAME_SAVE_ERROR_LOG_MESSAGE = "Failed to save baby name"
        private const val BIRTHDAY_SAVE_ERROR_LOG_MESSAGE = "Failed to save baby birthday"
        private const val PHOTO_SAVE_ERROR_LOG_MESSAGE = "Failed to save baby photo"
    }
}
