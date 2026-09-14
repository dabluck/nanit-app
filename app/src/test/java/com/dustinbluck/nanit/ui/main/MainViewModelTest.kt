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
                editState = EditState.Closed
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
    fun editIsClosedInitially() = testScope.runTest {
        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(EditState.Closed)
    }

    @Test
    fun editNameOpensNameEdit() = testScope.runTest {
        subject.editName()

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(
            EditState.Open(
                field = EditField.NAME,
                isSaving = false,
                saveFailed = false
            )
        )
    }

    @Test
    fun cancelEditClosesEdit() = testScope.runTest {
        subject.editName()
        subject.cancelEdit()

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(EditState.Closed)
    }

    @Test
    fun saveNameStoresTrimmedName() = testScope.runTest {
        subject.editName()
        subject.saveName(UNTRIMMED_NAME)

        val baby = babyRepository.baby.first()

        assertThat(baby.name).isEqualTo(NAME)
    }

    @Test
    fun saveNameClosesEdit() = testScope.runTest {
        subject.editName()
        subject.saveName(NAME)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(EditState.Closed)
    }

    @Test
    fun saveNameWithUnchangedNameClosesEdit() = testScope.runTest {
        babyRepository.setName(NAME)
        subject.editName()
        subject.saveName(NAME)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(EditState.Closed)
    }

    @Test
    fun saveNameIgnoresBlankName() = testScope.runTest {
        subject.editName()
        subject.saveName(BLANK_NAME)

        val baby = babyRepository.baby.first()

        assertThat(baby.name).isNull()
    }

    @Test
    fun saveNameIsIgnoredWhenEditIsClosed() = testScope.runTest {
        subject.saveName(NAME)

        val baby = babyRepository.baby.first()

        assertThat(baby.name).isNull()
    }

    @Test
    fun saveNameIsIgnoredWhenBirthdayEditIsOpen() = testScope.runTest {
        subject.editBirthday()
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

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(
            EditState.Open(
                field = EditField.NAME,
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

    @Test
    fun editBirthdayOpensBirthdayEdit() = testScope.runTest {
        subject.editBirthday()

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(
            EditState.Open(
                field = EditField.BIRTHDAY,
                isSaving = false,
                saveFailed = false
            )
        )
    }

    @Test
    fun saveBirthdayStoresBirthday() = testScope.runTest {
        subject.editBirthday()
        subject.saveBirthday(BIRTHDAY)

        val baby = babyRepository.baby.first()

        assertThat(baby.birthday).isEqualTo(BIRTHDAY)
    }

    @Test
    fun saveBirthdayClosesEdit() = testScope.runTest {
        subject.editBirthday()
        subject.saveBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(EditState.Closed)
    }

    @Test
    fun saveBirthdayWithUnchangedBirthdayClosesEdit() = testScope.runTest {
        babyRepository.setBirthday(BIRTHDAY)
        subject.editBirthday()
        subject.saveBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(EditState.Closed)
    }

    @Test
    fun saveBirthdayIsIgnoredWhenEditIsClosed() = testScope.runTest {
        subject.saveBirthday(BIRTHDAY)

        val baby = babyRepository.baby.first()

        assertThat(baby.birthday).isNull()
    }

    @Test
    fun saveBirthdayIsIgnoredWhenNameEditIsOpen() = testScope.runTest {
        subject.editName()
        subject.saveBirthday(BIRTHDAY)

        val baby = babyRepository.baby.first()

        assertThat(baby.birthday).isNull()
    }

    @Test
    fun failedSaveBirthdayKeepsBirthdayEditOpenWithError() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.editBirthday()
        subject.saveBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(
            EditState.Open(
                field = EditField.BIRTHDAY,
                isSaving = false,
                saveFailed = true
            )
        )
    }

    @Test
    fun failedSaveBirthdayIsLogged() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.editBirthday()
        subject.saveBirthday(BIRTHDAY)

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
