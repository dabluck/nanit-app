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

class MainViewModel(
    private val babyRepository: BabyRepository,
    private val logger: Logger
) : ViewModel() {

    private val nameEditState = MutableStateFlow<NameEditState>(NameEditState.Closed)

    val uiState: StateFlow<MainUiState> = combine<Baby, NameEditState, MainUiState>(
        babyRepository.baby,
        nameEditState
    ) { baby, nameEdit ->
        MainUiState.Loaded(
            baby = baby,
            nameEditState = nameEdit
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
        nameEditState.value = NameEditState.Open(
            isSaving = false,
            saveFailed = false
        )
    }

    fun cancelNameEdit() {
        nameEditState.value = NameEditState.Closed
    }

    fun saveName(name: String) {
        val trimmedName = name.trim()
        val currentState = nameEditState.value
        if (trimmedName.isNotEmpty() && currentState is NameEditState.Open && !currentState.isSaving) {
            nameEditState.value = NameEditState.Open(
                isSaving = true,
                saveFailed = false
            )
            viewModelScope.launch {
                if (babyRepository.setName(trimmedName)) {
                    nameEditState.value = NameEditState.Closed
                } else {
                    logger.e(
                        TAG,
                        NAME_SAVE_ERROR_LOG_MESSAGE
                    )
                    nameEditState.update { state ->
                        if (state is NameEditState.Open) {
                            NameEditState.Open(
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
    }
}
