package com.dustinbluck.nanit.ui.main

import com.dustinbluck.nanit.data.Baby
import com.dustinbluck.nanit.data.BabyRepository
import com.dustinbluck.nanit.data.PreferencesBabyRepository
import com.dustinbluck.nanit.deps.TestDependencyFactory
import com.dustinbluck.nanit.logging.FakeLogger
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
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

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var factory: TestDependencyFactory

    private lateinit var babyRepository: PreferencesBabyRepository

    private lateinit var logger: FakeLogger

    private lateinit var subject: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScope.testScheduler))
        factory = TestDependencyFactory(
            root = folder.root,
            testScope = testScope
        )
        babyRepository = factory.babyRepository()
        logger = factory.logger()
        subject = createSubject(babyRepository)
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
    fun uiStateContainsStoredBaby() = testScope.runTest {
        babyRepository.setName(NAME)

        val uiState = awaitLoadResult()

        assertThat(uiState).isEqualTo(
            MainUiState.Loaded(
                baby = Baby(
                    name = NAME,
                    birthday = null,
                    photo = null
                ),
                nameEditState = NameEditState.Closed
            )
        )
    }

    @Test
    fun uiStateIsErrorWhenBabyCannotBeRead() = testScope.runTest {
        factory.babyDataStoreFile().writeText(CORRUPT_DATA_STORE_CONTENTS)

        val uiState = awaitLoadResult()

        assertThat(uiState).isEqualTo(MainUiState.Error)
    }

    @Test
    fun readFailureIsLoggedWithException() = testScope.runTest {
        factory.babyDataStoreFile().writeText(CORRUPT_DATA_STORE_CONTENTS)
        awaitLoadResult()

        val entry = logger.entries.single()

        assertThat(entry.tr).isInstanceOf(IOException::class.java)
    }

    @Test
    fun birthdayIsEnabledWhenNameAndBirthdayAreSet() = testScope.runTest {
        babyRepository.setName(NAME)
        babyRepository.setBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.isBirthdayEnabled).isTrue()
    }

    @Test
    fun birthdayIsDisabledWhenNothingIsSet() = testScope.runTest {
        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.isBirthdayEnabled).isFalse()
    }

    @Test
    fun birthdayIsDisabledWhenNameIsMissing() = testScope.runTest {
        babyRepository.setBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.isBirthdayEnabled).isFalse()
    }

    @Test
    fun birthdayIsDisabledWhenNameIsBlank() = testScope.runTest {
        babyRepository.setName(BLANK_NAME)
        babyRepository.setBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.isBirthdayEnabled).isFalse()
    }

    @Test
    fun birthdayIsDisabledWhenBirthdayIsMissing() = testScope.runTest {
        babyRepository.setName(NAME)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.isBirthdayEnabled).isFalse()
    }

    @Test
    fun nameEditIsClosedInitially() = testScope.runTest {
        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.nameEditState).isEqualTo(NameEditState.Closed)
    }

    @Test
    fun editNameOpensNameEdit() = testScope.runTest {
        subject.editName()

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.nameEditState).isEqualTo(
            NameEditState.Open(
                isSaving = false,
                saveFailed = false
            )
        )
    }

    @Test
    fun cancelNameEditClosesNameEdit() = testScope.runTest {
        subject.editName()
        subject.cancelNameEdit()

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.nameEditState).isEqualTo(NameEditState.Closed)
    }

    @Test
    fun saveNameStoresTrimmedName() = testScope.runTest {
        subject.editName()
        subject.saveName(UNTRIMMED_NAME)

        val baby = babyRepository.baby.first()

        assertThat(baby.name).isEqualTo(NAME)
    }

    @Test
    fun saveNameClosesNameEdit() = testScope.runTest {
        subject.editName()
        subject.saveName(NAME)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.nameEditState).isEqualTo(NameEditState.Closed)
    }

    @Test
    fun saveNameWithUnchangedNameClosesNameEdit() = testScope.runTest {
        babyRepository.setName(NAME)
        subject.editName()
        subject.saveName(NAME)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.nameEditState).isEqualTo(NameEditState.Closed)
    }

    @Test
    fun saveNameIgnoresBlankName() = testScope.runTest {
        subject.editName()
        subject.saveName(BLANK_NAME)

        val baby = babyRepository.baby.first()

        assertThat(baby.name).isNull()
    }

    @Test
    fun saveNameIsIgnoredWhenNameEditIsClosed() = testScope.runTest {
        subject.saveName(NAME)

        val baby = babyRepository.baby.first()

        assertThat(baby.name).isNull()
    }

    @Test
    fun failedSaveNameKeepsNameEditOpenWithError() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.editName()
        subject.saveName(NAME)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.nameEditState).isEqualTo(
            NameEditState.Open(
                isSaving = false,
                saveFailed = true
            )
        )
    }

    @Test
    fun failedSaveNameIsLogged() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.editName()
        subject.saveName(NAME)

        val entry = logger.entries.single()

        assertThat(entry.level).isEqualTo(FakeLogger.Level.ERROR)
    }

    private fun createSubject(repository: BabyRepository): MainViewModel {
        return MainViewModel(
            babyRepository = repository,
            logger = logger
        )
    }

    private suspend fun awaitLoadResult(): MainUiState {
        return subject.uiState.first { state ->
            state != MainUiState.Loading
        }
    }

    private companion object {
        private const val CORRUPT_DATA_STORE_CONTENTS = "not a preferences file"
        private const val NAME = "Dustin"
        private const val UNTRIMMED_NAME = "  Dustin  "
        private const val BLANK_NAME = " "
        private val BIRTHDAY = LocalDate.of(
            2025,
            3,
            14
        )
    }
}
