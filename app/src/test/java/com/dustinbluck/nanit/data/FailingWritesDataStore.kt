package com.dustinbluck.nanit.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import java.io.IOException

internal class FailingWritesDataStore(
    private val delegate: DataStore<Preferences>
) : DataStore<Preferences> {
    private var isFailing = false

    override val data: Flow<Preferences> = delegate.data

    fun failWrites() {
        isFailing = true
    }

    override suspend fun updateData(
        transform: suspend (Preferences) -> Preferences
    ): Preferences {
        if (isFailing) {
            throw IOException(WRITE_FAILURE_MESSAGE)
        }
        return delegate.updateData(transform)
    }

    private companion object {
        private const val WRITE_FAILURE_MESSAGE = "write failed"
    }
}
