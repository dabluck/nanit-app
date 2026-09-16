package com.dustinbluck.nanit.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID

class PreferencesBabyRepository(
    private val dataStore: DataStore<Preferences>,
    private val produceFilesDirectory: () -> File,
    private val ioDispatcher: CoroutineDispatcher
) : BabyRepository {

    private val photoDirectory: File by lazy {
        produceFilesDirectory().resolve(PHOTO_DIRECTORY_NAME)
    }

    private val photoMutex = Mutex()

    override val baby: Flow<Baby> = dataStore.data
        .map { preferences ->
            Baby(
                name = preferences[NAME_PREFERENCE],
                birthday = preferences[BIRTHDAY_PREFERENCE]?.let(LocalDate::ofEpochDay),
                photo = preferences[PHOTO_PREFERENCE]?.let(::existingPhoto)
            )
        }
        .distinctUntilChanged()
        .flowOn(ioDispatcher)

    override suspend fun setName(name: String): Boolean {
        return editPreferences { preferences ->
            preferences[NAME_PREFERENCE] = name
        }
    }

    override suspend fun setBirthday(birthday: LocalDate): Boolean {
        return editPreferences { preferences ->
            preferences[BIRTHDAY_PREFERENCE] = birthday.toEpochDay()
        }
    }

    override suspend fun setPhoto(openPhoto: () -> InputStream): Boolean {
        // this does several operations that can take a long time so in theories coroutines could
        // interleave without the mutex
        return photoMutex.withLock {
            withContext(ioDispatcher) {
                val file = File(
                    photoDirectory,
                    UUID.randomUUID().toString()
                )
                try {
                    openPhoto().use { photo ->
                        photoDirectory.mkdirs()
                        file.outputStream().use { output ->
                            photo.copyTo(output)
                        }
                    }
                    dataStore.edit { preferences ->
                        preferences[PHOTO_PREFERENCE] = file.name
                    }
                    deletePhotosExcept(file)
                    true
                } catch (_: IOException) {
                    file.delete()
                    false
                } catch (_: SecurityException) {
                    file.delete()
                    false
                }
            }
        }
    }

    override suspend fun clearPhoto(): Boolean {
        return photoMutex.withLock {
            withContext(ioDispatcher) {
                val cleared = editPreferences { preferences ->
                    preferences.remove(PHOTO_PREFERENCE)
                }
                if (cleared) {
                    deletePhotosExcept(null)
                }
                cleared
            }
        }
    }

    private suspend fun editPreferences(transform: (MutablePreferences) -> Unit): Boolean {
        return try {
            dataStore.edit(transform)
            true
        } catch (_: IOException) {
            false
        }
    }

    private fun existingPhoto(fileName: String): File? {
        return File(
            photoDirectory,
            fileName
        ).takeIf(File::exists)
    }

    private fun deletePhotosExcept(photo: File?) {
        photoDirectory.listFiles()
            ?.filter { file ->
                file != photo
            }
            ?.forEach(File::delete)
    }

    private companion object {
        private const val KEY_NAME = "name"
        private const val KEY_BIRTHDAY = "birthday"
        private const val KEY_PHOTO = "photo"
        private const val PHOTO_DIRECTORY_NAME = "baby_photo"
        private val NAME_PREFERENCE = stringPreferencesKey(KEY_NAME)
        private val BIRTHDAY_PREFERENCE = longPreferencesKey(KEY_BIRTHDAY)
        private val PHOTO_PREFERENCE = stringPreferencesKey(KEY_PHOTO)
    }
}
