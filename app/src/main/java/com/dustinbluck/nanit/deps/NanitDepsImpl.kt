package com.dustinbluck.nanit.deps

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.core.content.FileProvider
import androidx.datastore.preferences.preferencesDataStoreFile
import com.dustinbluck.nanit.data.BabyRepository
import com.dustinbluck.nanit.data.PhotoManager
import com.dustinbluck.nanit.data.PreferencesBabyRepository
import com.dustinbluck.nanit.logging.AndroidLogger
import com.dustinbluck.nanit.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * for a relatively small app without activity-scoped dependencies or other complexities,
 * we can use a form of pure DI and avoid overhead of Dagger, Hilt, etc.
 */
class NanitDepsImpl(
    private val application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NanitDeps {

    private val babyDataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create {
            application.preferencesDataStoreFile(BABY_DATA_STORE_NAME)
        }
    }

    override val babyRepository: BabyRepository by lazy {
        PreferencesBabyRepository(
            dataStore = babyDataStore,
            produceFilesDirectory = {
                application.filesDir
            },
            ioDispatcher = ioDispatcher
        )
    }

    override val photoManager: PhotoManager by lazy {
        PhotoManager(
            produceCacheDirectory = {
                application.cacheDir
            },
            uriForFile = { file ->
                FileProvider.getUriForFile(
                    application,
                    application.packageName + CAMERA_PHOTO_AUTHORITY_SUFFIX,
                    file
                )
            }
        )
    }

    override val logger: Logger by lazy {
        AndroidLogger()
    }

    private companion object {
        private const val BABY_DATA_STORE_NAME = "baby"
        private const val CAMERA_PHOTO_AUTHORITY_SUFFIX = ".cameraphoto"
    }
}
