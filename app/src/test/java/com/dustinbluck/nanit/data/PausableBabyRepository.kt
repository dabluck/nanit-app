package com.dustinbluck.nanit.data

import kotlinx.coroutines.CompletableDeferred
import java.io.InputStream
import java.time.LocalDate

/**
 * Holds every write until [resumeWrites], so tests can act while a save is still in progress.
 */
internal class PausableBabyRepository(delegate: BabyRepository) : BabyRepository by delegate {
    private val writeResult = CompletableDeferred<Boolean>()

    var writeCount = 0
        private set

    fun resumeWrites(success: Boolean) {
        writeResult.complete(success)
    }

    override suspend fun setName(name: String): Boolean {
        return awaitWrite()
    }

    override suspend fun setBirthday(birthday: LocalDate): Boolean {
        return awaitWrite()
    }

    override suspend fun setPhoto(openPhoto: () -> InputStream): Boolean {
        return awaitWrite()
    }

    override suspend fun clearPhoto(): Boolean {
        return awaitWrite()
    }

    private suspend fun awaitWrite(): Boolean {
        writeCount++
        return writeResult.await()
    }
}
