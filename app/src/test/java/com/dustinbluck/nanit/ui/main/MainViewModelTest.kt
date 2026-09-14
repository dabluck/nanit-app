package com.dustinbluck.nanit.ui.main

import com.dustinbluck.nanit.data.Baby
import com.dustinbluck.nanit.data.PreferencesBabyRepository
import com.dustinbluck.nanit.deps.TestDependencyFactory
import com.dustinbluck.nanit.logging.FakeLogger
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
internal class MainViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var factory: TestDependencyFactory

    private lateinit var babyRepository: PreferencesBabyRepository

    private lateinit var logger: FakeLogger

    private lateinit var subject: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        factory = TestDependencyFactory(folder.root)
        babyRepository = factory.babyRepository()
        logger = factory.logger()
        subject = MainViewModel(
            babyRepository = babyRepository,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiStateIsLoadingBeforeCollection() {
        val uiState = subject.uiState.value

        assertThat(uiState).isEqualTo(MainUiState.Loading)
    }

    @Test
    fun uiStateContainsStoredBaby() = runTest {
        babyRepository.setName(NAME)

        val uiState = awaitLoadResult()

        assertThat(uiState).isEqualTo(
            MainUiState.Loaded(
                Baby(
                    name = NAME,
                    birthday = null,
                    photo = null
                )
            )
        )
    }

    @Test
    fun uiStateIsErrorWhenBabyCannotBeRead() = runTest {
        factory.babyDataStoreFile().writeText(CORRUPT_DATA_STORE_CONTENTS)

        val uiState = awaitLoadResult()

        assertThat(uiState).isEqualTo(MainUiState.Error)
    }

    @Test
    fun readFailureIsLoggedWithException() = runTest {
        factory.babyDataStoreFile().writeText(CORRUPT_DATA_STORE_CONTENTS)
        awaitLoadResult()

        val entry = logger.entries.single()

        assertThat(entry.tr).isInstanceOf(IOException::class.java)
    }

    @Test
    fun birthdayIsEnabledWhenNameAndBirthdayAreSet() = runTest {
        babyRepository.setName(NAME)
        babyRepository.setBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.isBirthdayEnabled).isTrue()
    }

    @Test
    fun birthdayIsDisabledWhenNothingIsSet() = runTest {
        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.isBirthdayEnabled).isFalse()
    }

    @Test
    fun birthdayIsDisabledWhenNameIsMissing() = runTest {
        babyRepository.setBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.isBirthdayEnabled).isFalse()
    }

    @Test
    fun birthdayIsDisabledWhenNameIsBlank() = runTest {
        babyRepository.setName(BLANK_NAME)
        babyRepository.setBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.isBirthdayEnabled).isFalse()
    }

    @Test
    fun birthdayIsDisabledWhenBirthdayIsMissing() = runTest {
        babyRepository.setName(NAME)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.isBirthdayEnabled).isFalse()
    }

    private suspend fun awaitLoadResult(): MainUiState {
        return subject.uiState.first { state ->
            state != MainUiState.Loading
        }
    }

    private companion object {
        private const val CORRUPT_DATA_STORE_CONTENTS = "not a preferences file"
        private const val NAME = "Dustin"
        private const val BLANK_NAME = " "
        private val BIRTHDAY = LocalDate.of(
            2025,
            3,
            14
        )
    }
}
