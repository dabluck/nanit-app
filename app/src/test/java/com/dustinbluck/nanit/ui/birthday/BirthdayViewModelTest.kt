package com.dustinbluck.nanit.ui.birthday

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.dustinbluck.nanit.data.BabyRepository
import com.dustinbluck.nanit.data.PreferencesBabyRepository
import com.dustinbluck.nanit.deps.TestDependencyFactory
import com.dustinbluck.nanit.logging.FakeLogger
import com.dustinbluck.nanit.ui.photo.PhotoEditState
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
import java.io.File
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
internal class BirthdayViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var factory: TestDependencyFactory

    private lateinit var babyRepository: PreferencesBabyRepository

    private lateinit var logger: FakeLogger

    private lateinit var subject: BirthdayViewModel

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

        assertThat(uiState).isEqualTo(BirthdayUiState.Loading)
    }

    @Test
    fun uiStateIsErrorWhenBabyCannotBeRead() = testScope.runTest {
        factory.babyDataStoreFile().mkdirs()

        val uiState = awaitLoadResult()

        assertThat(uiState).isEqualTo(BirthdayUiState.Error)
    }

    @Test
    fun readFailureIsLoggedWithException() = testScope.runTest {
        factory.babyDataStoreFile().mkdirs()
        awaitLoadResult()

        val entry = logger.entries.single()

        assertThat(entry.tr).isInstanceOf(IOException::class.java)
    }

    @Test
    fun uiStateIsErrorWhenNameIsMissing() = testScope.runTest {
        babyRepository.setBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat(uiState).isEqualTo(BirthdayUiState.Error)
    }

    @Test
    fun uiStateIsErrorWhenNameIsBlank() = testScope.runTest {
        babyRepository.setName(BLANK_NAME)
        babyRepository.setBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat(uiState).isEqualTo(BirthdayUiState.Error)
    }

    @Test
    fun uiStateIsErrorWhenBirthdayIsMissing() = testScope.runTest {
        babyRepository.setName(NAME)

        val uiState = awaitLoadResult()

        assertThat(uiState).isEqualTo(BirthdayUiState.Error)
    }

    @Test
    fun missingDetailsAreLogged() = testScope.runTest {
        babyRepository.setName(NAME)
        awaitLoadResult()

        val entry = logger.entries.single()

        assertThat(entry.level).isEqualTo(FakeLogger.Level.ERROR)
    }

    @Test
    fun uiStateIsErrorWhenBirthdayIsInFuture() = testScope.runTest {
        babyRepository.setName(NAME)
        babyRepository.setBirthday(FUTURE_BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat(uiState).isEqualTo(BirthdayUiState.Error)
    }

    @Test
    fun futureBirthdayIsLogged() = testScope.runTest {
        babyRepository.setName(NAME)
        babyRepository.setBirthday(FUTURE_BIRTHDAY)
        awaitLoadResult()

        val entry = logger.entries.single()

        assertThat(entry.level).isEqualTo(FakeLogger.Level.ERROR)
    }

    @Test
    fun uiStateContainsNameAndAgeOnClockDate() = testScope.runTest {
        babyRepository.setName(NAME)
        babyRepository.setBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat(uiState).isEqualTo(
            BirthdayUiState.Loaded(
                name = NAME,
                age = Age(
                    value = 5,
                    unit = AgeUnit.MONTHS
                ),
                photo = null
            )
        )
    }

    @Test
    fun uiStateContainsPhotoBytes() = testScope.runTest {
        babyRepository.setName(NAME)
        babyRepository.setBirthday(BIRTHDAY)
        babyRepository.setPhoto(PHOTO::inputStream)

        val uiState = awaitLoadResult()

        assertThat((uiState as? BirthdayUiState.Loaded)?.photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun photoIsNullWhenPhotoIsNotSet() = testScope.runTest {
        babyRepository.setName(NAME)
        babyRepository.setBirthday(BIRTHDAY)

        val uiState = awaitLoadResult()

        assertThat((uiState as? BirthdayUiState.Loaded)?.photo).isNull()
    }

    @Test
    fun photoIsNullWhenPhotoFileIsMissing() = testScope.runTest {
        babyRepository.setName(NAME)
        babyRepository.setBirthday(BIRTHDAY)
        babyRepository.setPhoto(PHOTO::inputStream)
        factory.babyPhotoDirectory().deleteRecursively()

        val uiState = awaitLoadResult()

        assertThat((uiState as? BirthdayUiState.Loaded)?.photo).isNull()
    }

    @Test
    fun photoUpdatesWhenPhotoChanges() = testScope.runTest {
        babyRepository.setName(NAME)
        babyRepository.setBirthday(BIRTHDAY)
        babyRepository.setPhoto(PHOTO::inputStream)

        var photo: File? = null
        subject.uiState.test {
            awaitLoaded()
            babyRepository.setPhoto(OTHER_PHOTO::inputStream)
            photo = awaitLoaded().photo
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(photo?.readBytes()).isEqualTo(OTHER_PHOTO)
    }

    @Test
    fun photoEditIsClosedBeforeEditing() {
        val photoEditState = subject.photoEditState.value

        assertThat(photoEditState).isEqualTo(PhotoEditState.Closed)
    }

    @Test
    fun editPhotoOpensPhotoEdit() {
        subject.editPhoto()

        val photoEditState = subject.photoEditState.value

        assertThat(photoEditState).isEqualTo(
            PhotoEditState.Open(
                isSaving = false,
                saveFailed = false
            )
        )
    }

    @Test
    fun cancelPhotoEditClosesPhotoEdit() {
        subject.editPhoto()
        subject.cancelPhotoEdit()

        val photoEditState = subject.photoEditState.value

        assertThat(photoEditState).isEqualTo(PhotoEditState.Closed)
    }

    @Test
    fun savePhotoStoresPhoto() = testScope.runTest {
        subject.editPhoto()
        subject.savePhoto(PHOTO::inputStream)

        val photo = babyRepository.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun savePhotoClosesPhotoEdit() = testScope.runTest {
        subject.editPhoto()
        subject.savePhoto(PHOTO::inputStream)

        val photoEditState = subject.photoEditState.value

        assertThat(photoEditState).isEqualTo(PhotoEditState.Closed)
    }

    @Test
    fun savePhotoStoresPhotoWhenPhotoEditIsClosed() = testScope.runTest {
        subject.savePhoto(PHOTO::inputStream)

        val photo = babyRepository.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun clearPhotoRemovesPhoto() = testScope.runTest {
        babyRepository.setPhoto(PHOTO::inputStream)
        subject.editPhoto()
        subject.clearPhoto()

        val photo = babyRepository.baby.first().photo

        assertThat(photo).isNull()
    }

    @Test
    fun clearPhotoClosesPhotoEdit() = testScope.runTest {
        babyRepository.setPhoto(PHOTO::inputStream)
        subject.editPhoto()
        subject.clearPhoto()

        val photoEditState = subject.photoEditState.value

        assertThat(photoEditState).isEqualTo(PhotoEditState.Closed)
    }

    @Test
    fun failedSavePhotoKeepsPhotoEditOpenWithError() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.editPhoto()
        subject.savePhoto(PHOTO::inputStream)

        val photoEditState = subject.photoEditState.value

        assertThat(photoEditState).isEqualTo(
            PhotoEditState.Open(
                isSaving = false,
                saveFailed = true
            )
        )
    }

    @Test
    fun failedSavePhotoIsLogged() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.editPhoto()
        subject.savePhoto(PHOTO::inputStream)

        val entry = logger.entries.single()

        assertThat(entry.level).isEqualTo(FakeLogger.Level.ERROR)
    }

    @Test
    fun failedClearPhotoKeepsPhotoEditOpenWithError() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.editPhoto()
        subject.clearPhoto()

        val photoEditState = subject.photoEditState.value

        assertThat(photoEditState).isEqualTo(
            PhotoEditState.Open(
                isSaving = false,
                saveFailed = true
            )
        )
    }

    @Test
    fun failedClearPhotoIsLogged() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.editPhoto()
        subject.clearPhoto()

        val entry = logger.entries.single()

        assertThat(entry.level).isEqualTo(FakeLogger.Level.ERROR)
    }

    @Test
    fun photoEditIsSavingWhilePhotoIsWritten() = testScope.runTest {
        subject = createSubject(factory.pausableBabyRepository(babyRepository))
        subject.editPhoto()
        subject.savePhoto(PHOTO::inputStream)

        val photoEditState = subject.photoEditState.value

        assertThat(photoEditState).isEqualTo(
            PhotoEditState.Open(
                isSaving = true,
                saveFailed = false
            )
        )
    }

    @Test
    fun savePhotoIsIgnoredWhileSaving() = testScope.runTest {
        val repository = factory.pausableBabyRepository(babyRepository)
        subject = createSubject(repository)
        subject.editPhoto()
        subject.savePhoto(PHOTO::inputStream)
        subject.savePhoto(PHOTO::inputStream)

        val writeCount = repository.writeCount

        assertThat(writeCount).isEqualTo(1)
    }

    @Test
    fun savePhotoIsIgnoredWhileSavingAfterReopeningPhotoEdit() = testScope.runTest {
        val repository = factory.pausableBabyRepository(babyRepository)
        subject = createSubject(repository)
        subject.editPhoto()
        subject.savePhoto(PHOTO::inputStream)
        subject.editPhoto()
        subject.savePhoto(PHOTO::inputStream)

        val writeCount = repository.writeCount

        assertThat(writeCount).isEqualTo(1)
    }

    private fun createSubject(repository: BabyRepository): BirthdayViewModel {
        return BirthdayViewModel(
            babyRepository = repository,
            clock = factory.clock(TODAY),
            logger = logger
        )
    }

    private suspend fun ReceiveTurbine<BirthdayUiState>.awaitLoaded(): BirthdayUiState.Loaded {
        var uiState = awaitItem()
        while (uiState !is BirthdayUiState.Loaded) {
            uiState = awaitItem()
        }
        return uiState
    }

    private suspend fun awaitLoadResult(): BirthdayUiState {
        return subject.uiState.first { state ->
            state != BirthdayUiState.Loading
        }
    }

    private companion object {
        private const val NAME = "Dustin"
        private const val BLANK_NAME = " "
        private val BIRTHDAY = LocalDate.of(
            2025,
            3,
            14
        )
        private val TODAY = LocalDate.of(
            2025,
            8,
            20
        )
        private val FUTURE_BIRTHDAY = LocalDate.of(
            2025,
            8,
            21
        )
        private val PHOTO = byteArrayOf(
            1,
            2,
            3
        )
        private val OTHER_PHOTO = byteArrayOf(
            4,
            5,
            6
        )
    }
}
