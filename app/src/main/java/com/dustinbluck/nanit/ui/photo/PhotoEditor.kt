package com.dustinbluck.nanit.ui.photo

import com.dustinbluck.nanit.data.BabyRepository
import com.dustinbluck.nanit.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream

class PhotoEditor(
    private val babyRepository: BabyRepository,
    private val logger: Logger,
    private val scope: CoroutineScope
) {

    private val photoEdit = MutableStateFlow<PhotoEditState>(PhotoEditState.Closed)

    val state: StateFlow<PhotoEditState> = photoEdit.asStateFlow()

    fun open() {
        photoEdit.value = PhotoEditState.Open(
            isSaving = false,
            saveFailed = false
        )
    }

    fun cancel() {
        photoEdit.value = PhotoEditState.Closed
    }

    fun savePhoto(openPhoto: () -> InputStream) {
        save(PHOTO_SAVE_ERROR_LOG_MESSAGE) {
            babyRepository.setPhoto(openPhoto)
        }
    }

    fun clearPhoto() {
        save(PHOTO_CLEAR_ERROR_LOG_MESSAGE) {
            babyRepository.clearPhoto()
        }
    }

    private fun save(
        errorLogMessage: String,
        write: suspend () -> Boolean
    ) {
        if (photoEdit.value == PhotoEditState.Closed) {
            open()
        }
        val currentState = photoEdit.value
        if (currentState is PhotoEditState.Open && !currentState.isSaving) {
            photoEdit.value = currentState.copy(
                isSaving = true,
                saveFailed = false
            )
            scope.launch {
                val saved = write()
                if (!saved) {
                    logger.e(
                        TAG,
                        errorLogMessage
                    )
                }
                // can race with a newer save in theory but in practice is safe enough for user speed
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

    private companion object {
        private const val TAG = "PhotoEditor"
        private const val PHOTO_SAVE_ERROR_LOG_MESSAGE = "Failed to save baby photo"
        private const val PHOTO_CLEAR_ERROR_LOG_MESSAGE = "Failed to clear baby photo"
    }
}
