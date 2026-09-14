package com.dustinbluck.nanit.deps

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.dustinbluck.nanit.data.PreferencesBabyRepository
import com.dustinbluck.nanit.logging.FakeLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
internal class TestDependencyFactory(private val root: File) {
    fun babyDataStoreFile(): File {
        return File(
            root,
            BABY_DATA_STORE_FILE_NAME
        )
    }

    fun babyPhotoDirectory(): File {
        return File(
            root,
            BABY_PHOTO_DIRECTORY_NAME
        )
    }

    fun babyDataStore(file: File = babyDataStoreFile()): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            file
        }
    }

    fun babyRepository(
        dataStore: DataStore<Preferences> = babyDataStore(),
        photoDirectory: File = babyPhotoDirectory(),
        ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    ): PreferencesBabyRepository {
        return PreferencesBabyRepository(
            dataStore = dataStore,
            producePhotoDirectory = {
                photoDirectory
            },
            ioDispatcher = ioDispatcher
        )
    }

    fun logger(): FakeLogger {
        return FakeLogger()
    }

    private companion object {
        private const val BABY_DATA_STORE_FILE_NAME = "baby.preferences_pb"
        private const val BABY_PHOTO_DIRECTORY_NAME = "baby_photo"
    }
}
