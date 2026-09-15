package com.dustinbluck.nanit.ui.birthday

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
import java.time.Clock
import java.time.LocalDate

class BirthdayViewModel(
    babyRepository: BabyRepository,
    private val clock: Clock,
    private val logger: Logger
) : ViewModel() {

    val uiState: StateFlow<BirthdayUiState> = babyRepository.baby
        .map(::toUiState)
        .catch { exception ->
            logger.e(
                TAG,
                LOAD_ERROR_LOG_MESSAGE,
                exception
            )
            emit(BirthdayUiState.Error)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = BirthdayUiState.Loading
        )

    private fun toUiState(baby: Baby): BirthdayUiState {
        val name = baby.name
        val birthday = baby.birthday
        if (name.isNullOrBlank() || birthday == null) {
            logger.e(
                TAG,
                MISSING_DETAILS_LOG_MESSAGE
            )
            return BirthdayUiState.Error
        }
        val age = AgeCalculatorUtil.calculateAge(
            birthday = birthday,
            today = LocalDate.now(clock)
        )
        if (age == null) {
            logger.e(
                TAG,
                FUTURE_BIRTHDAY_LOG_MESSAGE
            )
            return BirthdayUiState.Error
        }
        return BirthdayUiState.Loaded(
            name = name,
            age = age
        )
    }

    private companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val TAG = "BirthdayViewModel"
        private const val LOAD_ERROR_LOG_MESSAGE = "Failed to load baby"
        private const val MISSING_DETAILS_LOG_MESSAGE = "Baby is missing a name or birthday"
        private const val FUTURE_BIRTHDAY_LOG_MESSAGE = "Baby birthday is in the future"
    }
}
