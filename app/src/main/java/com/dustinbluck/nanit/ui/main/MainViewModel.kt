package com.dustinbluck.nanit.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dustinbluck.nanit.data.Baby
import com.dustinbluck.nanit.data.BabyRepository
import com.dustinbluck.nanit.logging.Logger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    babyRepository: BabyRepository,
    logger: Logger
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = babyRepository.baby
        .map<Baby, MainUiState> { baby ->
            MainUiState.Loaded(baby)
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

    private companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val TAG = "MainViewModel"
        private const val LOAD_ERROR_LOG_MESSAGE = "Failed to load baby"
    }
}
