package com.dustinbluck.nanit.ui.birthday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dustinbluck.nanit.data.Baby
import com.dustinbluck.nanit.data.BabyRepository
import com.dustinbluck.nanit.logging.Logger
import com.dustinbluck.nanit.ui.photo.PhotoEditState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.Clock
import java.time.LocalDate

class BirthdayViewModel(
    private val babyRepository: BabyRepository,
    private val clock: Clock,
    private val logger: Logger
) : ViewModel() {

    private val photoEdit = MutableStateFlow<PhotoEditState>(PhotoEditState.Closed)

    val photoEditState: StateFlow<PhotoEditState> = photoEdit.asStateFlow()

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

    fun editPhoto() {
        photoEdit.value = PhotoEditState.Open(
            isSaving = false,
            saveFailed = false
        )
    }

    fun cancelPhotoEdit() {
        photoEdit.value = PhotoEditState.Closed
    }

    fun savePhoto(openPhoto: () -> InputStream) {
        savePhotoEdit(PHOTO_SAVE_ERROR_LOG_MESSAGE) {
            babyRepository.setPhoto(openPhoto)
        }
    }

    fun clearPhoto() {
        savePhotoEdit(PHOTO_CLEAR_ERROR_LOG_MESSAGE) {
            babyRepository.clearPhoto()
        }
    }

    private fun savePhotoEdit(
        errorLogMessage: String,
        write: suspend () -> Boolean
    ) {
        if (photoEdit.value == PhotoEditState.Closed) {
            editPhoto()
        }
        val currentState = photoEdit.value
        if (currentState is PhotoEditState.Open && !currentState.isSaving) {
            photoEdit.value = currentState.copy(
                isSaving = true,
                saveFailed = false
            )
            viewModelScope.launch {
                val saved = write()
                if (!saved) {
                    logger.e(
                        TAG,
                        errorLogMessage
                    )
                }
                photoEdit.update { state ->
                    if (state is PhotoEditState.Open) {
                        if (saved) {
                            PhotoEditState.Closed
                        } else {
                            state.copy(
                                isSaving = false,
                                saveFailed = true
                            )
                        }
                    } else {
                        state
                    }
                }
            }
        }
    }

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
            age = age,
            photo = baby.photo
        )
    }

    private companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val TAG = "BirthdayViewModel"
        private const val LOAD_ERROR_LOG_MESSAGE = "Failed to load baby"
        private const val MISSING_DETAILS_LOG_MESSAGE = "Baby is missing a name or birthday"
        private const val FUTURE_BIRTHDAY_LOG_MESSAGE = "Baby birthday is in the future"
        private const val PHOTO_SAVE_ERROR_LOG_MESSAGE = "Failed to save baby photo"
        private const val PHOTO_CLEAR_ERROR_LOG_MESSAGE = "Failed to clear baby photo"
    }
}
