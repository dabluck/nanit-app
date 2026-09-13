package com.dustinbluck.nanit.deps

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.dustinbluck.nanit.data.BabyRepository
import com.dustinbluck.nanit.data.PreferencesBabyRepository

/**
 * for a relatively small app without activity-scoped dependencies or other complexities,
 * we can use a form of pure DI and avoid overhead of Dagger, Hilt, etc.
 */
class NanitDepsImpl(
    private val application: Application
) : NanitDeps {

    private val babyDataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create {
            application.preferencesDataStoreFile(BABY_DATA_STORE_NAME)
        }
    }

    override val babyRepository: BabyRepository by lazy {
        PreferencesBabyRepository(babyDataStore)
    }

    companion object {
        private const val BABY_DATA_STORE_NAME = "baby"
    }
}
