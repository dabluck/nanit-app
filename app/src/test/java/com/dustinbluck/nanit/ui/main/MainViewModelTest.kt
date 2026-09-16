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
        factory.babyDataStoreFile().mkdirs()

        val uiState = awaitLoadResult()

        assertThat(uiState).isEqualTo(MainUiState.Error)
    }

    @Test
    fun readFailureIsLoggedWithException() = testScope.runTest {
        factory.babyDataStoreFile().mkdirs()
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
    fun saveBirthdayStoresTodaysBirthday() = testScope.runTest {
        subject.editBirthday()
        subject.saveBirthday(TODAY)

        val baby = babyRepository.baby.first()

        assertThat(baby.birthday).isEqualTo(TODAY)
    }

    @Test
    fun futureBirthdayIsNotStored() = testScope.runTest {
        subject.editBirthday()
        subject.saveBirthday(FUTURE_BIRTHDAY)

        val baby = babyRepository.baby.first()

        assertThat(baby.birthday).isNull()
    }

    @Test
    fun futureBirthdayKeepsEditOpen() = testScope.runTest {
        subject.editBirthday()
        subject.saveBirthday(FUTURE_BIRTHDAY)

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

    @Test
    fun editPhotoOpensPhotoEdit() = testScope.runTest {
        subject.editPhoto()

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(
            EditState.Open(
                field = EditField.PHOTO,
                isSaving = false,
                saveFailed = false
            )
        )
    }

    @Test
    fun savePhotoStoresPhoto() = testScope.runTest {
        subject.editPhoto()
        subject.savePhoto(PHOTO::inputStream)

        val photo = babyRepository.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun savePhotoClosesEdit() = testScope.runTest {
        subject.editPhoto()
        subject.savePhoto(PHOTO::inputStream)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(EditState.Closed)
    }

    @Test
    fun savePhotoStoresPhotoWhenEditIsClosed() = testScope.runTest {
        subject.savePhoto(PHOTO::inputStream)

        val photo = babyRepository.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun savePhotoIsIgnoredWhenNameEditIsOpen() = testScope.runTest {
        subject.editName()
        subject.savePhoto(PHOTO::inputStream)

        val baby = babyRepository.baby.first()

        assertThat(baby.photo).isNull()
    }

    @Test
    fun savePhotoFromCameraStoresPhoto() = testScope.runTest {
        val photoManager = factory.photoManager()
        photoManager.prepareCameraPhoto()
        factory.cameraPhotoFile().writeBytes(PHOTO)
        subject.editPhoto()
        subject.savePhoto(photoManager::openCameraPhoto)

        val photo = babyRepository.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun savePhotoFromCameraFailsWhenCameraWroteNoPhoto() = testScope.runTest {
        val photoManager = factory.photoManager()
        photoManager.prepareCameraPhoto()
        factory.cameraPhotoFile().writeBytes(PHOTO)
        photoManager.prepareCameraPhoto()
        subject.editPhoto()
        subject.savePhoto(photoManager::openCameraPhoto)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(
            EditState.Open(
                field = EditField.PHOTO,
                isSaving = false,
                saveFailed = true
            )
        )
    }

    @Test
    fun clearPhotoRemovesPhoto() = testScope.runTest {
        babyRepository.setPhoto(PHOTO::inputStream)
        subject.editPhoto()
        subject.clearPhoto()

        val baby = babyRepository.baby.first()

        assertThat(baby.photo).isNull()
    }

    @Test
    fun clearPhotoClosesEdit() = testScope.runTest {
        babyRepository.setPhoto(PHOTO::inputStream)
        subject.editPhoto()
        subject.clearPhoto()

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(EditState.Closed)
    }

    @Test
    fun clearPhotoRemovesPhotoWhenEditIsClosed() = testScope.runTest {
        babyRepository.setPhoto(PHOTO::inputStream)
        subject.clearPhoto()

        val baby = babyRepository.baby.first()

        assertThat(baby.photo).isNull()
    }

    @Test
    fun clearPhotoIsIgnoredWhenNameEditIsOpen() = testScope.runTest {
        babyRepository.setPhoto(PHOTO::inputStream)
        subject.editName()
        subject.clearPhoto()

        val photo = babyRepository.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun failedClearPhotoKeepsPhotoEditOpenWithError() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.editPhoto()
        subject.clearPhoto()

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(
            EditState.Open(
                field = EditField.PHOTO,
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
    fun failedSavePhotoKeepsPhotoEditOpenWithError() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.editPhoto()
        subject.savePhoto(PHOTO::inputStream)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(
            EditState.Open(
                field = EditField.PHOTO,
                isSaving = false,
                saveFailed = true
            )
        )
    }

    @Test
    fun failedSavePhotoWhenEditIsClosedOpensPhotoEditWithError() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.savePhoto(PHOTO::inputStream)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(
            EditState.Open(
                field = EditField.PHOTO,
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
    fun finishedSaveKeepsNewlyOpenedEditOpen() = testScope.runTest {
        val repository = factory.pausableBabyRepository(babyRepository)
        subject = createSubject(repository)
        subject.editPhoto()
        subject.savePhoto(PHOTO::inputStream)
        subject.cancelEdit()
        subject.editName()
        repository.resumeWrites(success = true)

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
    fun failedSaveAfterCancelEditKeepsEditClosed() = testScope.runTest {
        val repository = factory.pausableBabyRepository(babyRepository)
        subject = createSubject(repository)
        subject.editName()
        subject.saveName(NAME)
        subject.cancelEdit()
        repository.resumeWrites(success = false)

        val uiState = awaitLoadResult()

        assertThat((uiState as? MainUiState.Loaded)?.editState).isEqualTo(EditState.Closed)
    }

    @Test
    fun saveIsIgnoredWhileSaving() = testScope.runTest {
        val repository = factory.pausableBabyRepository(babyRepository)
        subject = createSubject(repository)
        subject.editName()
        subject.saveName(NAME)
        subject.saveName(NAME)

        val writeCount = repository.writeCount

        assertThat(writeCount).isEqualTo(1)
    }

    private fun createSubject(repository: BabyRepository): MainViewModel {
        return MainViewModel(
            babyRepository = repository,
            clock = factory.clock(TODAY),
            logger = logger
        )
    }

    private suspend fun awaitLoadResult(): MainUiState {
        return subject.uiState.first { state ->
            state != MainUiState.Loading
        }
    }

    private companion object {
        private const val NAME = "Dustin"
        private const val UNTRIMMED_NAME = "  Dustin  "
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
    }
}
