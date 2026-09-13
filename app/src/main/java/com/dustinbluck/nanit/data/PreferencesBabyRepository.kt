package com.dustinbluck.nanit.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class PreferencesBabyRepository(
    private val dataStore: DataStore<Preferences>
) : BabyRepository {

    override val baby: Flow<Baby> = dataStore.data
        .map { preferences ->
            Baby(
                name = preferences[NAME_PREFERENCE],
                birthday = preferences[BIRTHDAY_PREFERENCE]?.let(LocalDate::ofEpochDay)
            )
        }
        .distinctUntilChanged()

    override suspend fun setName(name: String) {
        dataStore.edit { preferences ->
            preferences[NAME_PREFERENCE] = name
        }
    }

    override suspend fun setBirthday(birthday: LocalDate) {
        dataStore.edit { preferences ->
            preferences[BIRTHDAY_PREFERENCE] = birthday.toEpochDay()
        }
    }

    companion object {
        private const val KEY_NAME = "name"
        private const val KEY_BIRTHDAY = "birthday"
        private val NAME_PREFERENCE = stringPreferencesKey(KEY_NAME)
        private val BIRTHDAY_PREFERENCE = longPreferencesKey(KEY_BIRTHDAY)
    }
}
