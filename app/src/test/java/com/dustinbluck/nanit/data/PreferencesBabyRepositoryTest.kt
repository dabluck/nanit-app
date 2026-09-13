package com.dustinbluck.nanit.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

internal class PreferencesBabyRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var subject: PreferencesBabyRepository

    @Before
    fun setUp() {
        val dataStore = PreferenceDataStoreFactory.create {
            File(
                folder.root,
                FILE_NAME
            )
        }
        subject = PreferencesBabyRepository(dataStore)
    }

    @Test
    fun babyIsEmptyWhenNothingIsStored() = runTest {
        val baby = subject.baby.first()

        assertThat(baby).isEqualTo(
            Baby(
                name = null,
                birthday = null
            )
        )
    }

    @Test
    fun setNameStoresName() = runTest {
        subject.setName(NAME)

        val baby = subject.baby.first()

        assertThat(baby).isEqualTo(
            Baby(
                name = NAME,
                birthday = null
            )
        )
    }

    @Test
    fun setBirthdayStoresBirthday() = runTest {
        subject.setBirthday(BIRTHDAY)

        val baby = subject.baby.first()

        assertThat(baby).isEqualTo(
            Baby(
                name = null,
                birthday = BIRTHDAY
            )
        )
    }

    @Test
    fun babyEmitsWhenValueChanges() = runTest {
        subject.baby.test {
            skipItems(1)

            subject.setName(NAME)

            assertThat(awaitItem()).isEqualTo(
                Baby(
                    name = NAME,
                    birthday = null
                )
            )
        }
    }

    private companion object {
        private const val FILE_NAME = "baby.preferences_pb"
        private const val NAME = "Dustin"
        private val BIRTHDAY = LocalDate.of(
            2025,
            3,
            14
        )
    }
}
