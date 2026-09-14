package com.dustinbluck.nanit.deps

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.dustinbluck.nanit.data.BabyRepository
import com.dustinbluck.nanit.data.FailingBabyRepository
import com.dustinbluck.nanit.data.PhotoManager
import com.dustinbluck.nanit.data.PreferencesBabyRepository
import com.dustinbluck.nanit.logging.FakeLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
internal class TestDependencyFactory(
    private val root: File,
    private val testScope: TestScope
) {
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

    fun cameraPhotoFile(): File {
        return root
            .resolve(CAMERA_PHOTO_DIRECTORY_NAME)
            .resolve(CAMERA_PHOTO_FILE_NAME)
    }

    fun babyDataStore(
        file: File = babyDataStoreFile(),
        scope: CoroutineScope = testScope.backgroundScope
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = {
                file
            }
        )
    }

    fun babyRepository(
        dataStore: DataStore<Preferences> = babyDataStore(),
        filesDirectory: File = root,
        ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScope.testScheduler)
    ): PreferencesBabyRepository {
        return PreferencesBabyRepository(
            dataStore = dataStore,
            produceFilesDirectory = {
                filesDirectory
            },
            ioDispatcher = ioDispatcher
        )
    }

    fun failingBabyRepository(delegate: BabyRepository = babyRepository()): FailingBabyRepository {
        return FailingBabyRepository(delegate)
    }

    fun photoManager(cacheDirectory: File = root): PhotoManager {
        return PhotoManager(
            produceCacheDirectory = {
                cacheDirectory
            },
            uriForFile = Uri::fromFile
        )
    }

    fun logger(): FakeLogger {
        return FakeLogger()
    }

    private companion object {
        private const val BABY_DATA_STORE_FILE_NAME = "baby.preferences_pb"
        private const val BABY_PHOTO_DIRECTORY_NAME = "baby_photo"
        private const val CAMERA_PHOTO_DIRECTORY_NAME = "camera_photo"
        private const val CAMERA_PHOTO_FILE_NAME = "photo.jpg"
    }
}
