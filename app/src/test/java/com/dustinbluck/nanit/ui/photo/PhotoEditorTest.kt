package com.dustinbluck.nanit.ui.photo

import com.dustinbluck.nanit.data.BabyRepository
import com.dustinbluck.nanit.data.PreferencesBabyRepository
import com.dustinbluck.nanit.deps.TestDependencyFactory
import com.dustinbluck.nanit.logging.FakeLogger
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
internal class PhotoEditorTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private val editorScope = CoroutineScope(UnconfinedTestDispatcher(testScope.testScheduler))

    private lateinit var factory: TestDependencyFactory

    private lateinit var babyRepository: PreferencesBabyRepository

    private lateinit var logger: FakeLogger

    private lateinit var subject: PhotoEditor

    @Before
    fun setUp() {
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
        editorScope.cancel()
    }

    @Test
    fun stateIsClosedBeforeEditing() {
        val state = subject.state.value

        assertThat(state).isEqualTo(PhotoEditState.Closed)
    }

    @Test
    fun openOpensPhotoEdit() {
        subject.open()

        val state = subject.state.value

        assertThat(state).isEqualTo(
            PhotoEditState.Open(
                isSaving = false,
                saveFailed = false
            )
        )
    }

    @Test
    fun cancelClosesPhotoEdit() {
        subject.open()
        subject.cancel()

        val state = subject.state.value

        assertThat(state).isEqualTo(PhotoEditState.Closed)
    }

    @Test
    fun savePhotoStoresPhoto() = testScope.runTest {
        subject.open()
        subject.savePhoto(PHOTO::inputStream)

        val photo = babyRepository.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun savePhotoClosesPhotoEdit() = testScope.runTest {
        subject.open()
        subject.savePhoto(PHOTO::inputStream)

        val state = subject.state.value

        assertThat(state).isEqualTo(PhotoEditState.Closed)
    }

    @Test
    fun savePhotoStoresPhotoWhenPhotoEditIsClosed() = testScope.runTest {
        subject.savePhoto(PHOTO::inputStream)

        val photo = babyRepository.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun savePhotoFromCameraStoresPhoto() = testScope.runTest {
        val photoManager = factory.photoManager()
        photoManager.prepareCameraPhoto()
        factory.cameraPhotoFile().writeBytes(PHOTO)
        subject.open()
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
        subject.open()
        subject.savePhoto(photoManager::openCameraPhoto)

        val state = subject.state.value

        assertThat(state).isEqualTo(
            PhotoEditState.Open(
                isSaving = false,
                saveFailed = true
            )
        )
    }

    @Test
    fun clearPhotoRemovesPhoto() = testScope.runTest {
        babyRepository.setPhoto(PHOTO::inputStream)
        subject.open()
        subject.clearPhoto()

        val photo = babyRepository.baby.first().photo

        assertThat(photo).isNull()
    }

    @Test
    fun clearPhotoClosesPhotoEdit() = testScope.runTest {
        babyRepository.setPhoto(PHOTO::inputStream)
        subject.open()
        subject.clearPhoto()

        val state = subject.state.value

        assertThat(state).isEqualTo(PhotoEditState.Closed)
    }

    @Test
    fun clearPhotoRemovesPhotoWhenPhotoEditIsClosed() = testScope.runTest {
        babyRepository.setPhoto(PHOTO::inputStream)
        subject.clearPhoto()

        val photo = babyRepository.baby.first().photo

        assertThat(photo).isNull()
    }

    @Test
    fun failedSavePhotoKeepsPhotoEditOpenWithError() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.open()
        subject.savePhoto(PHOTO::inputStream)

        val state = subject.state.value

        assertThat(state).isEqualTo(
            PhotoEditState.Open(
                isSaving = false,
                saveFailed = true
            )
        )
    }

    @Test
    fun failedSavePhotoWhenPhotoEditIsClosedOpensPhotoEditWithError() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.savePhoto(PHOTO::inputStream)

        val state = subject.state.value

        assertThat(state).isEqualTo(
            PhotoEditState.Open(
                isSaving = false,
                saveFailed = true
            )
        )
    }

    @Test
    fun failedSavePhotoIsLogged() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.open()
        subject.savePhoto(PHOTO::inputStream)

        val entry = logger.entries.single()

        assertThat(entry.level).isEqualTo(FakeLogger.Level.ERROR)
    }

    @Test
    fun failedClearPhotoKeepsPhotoEditOpenWithError() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.open()
        subject.clearPhoto()

        val state = subject.state.value

        assertThat(state).isEqualTo(
            PhotoEditState.Open(
                isSaving = false,
                saveFailed = true
            )
        )
    }

    @Test
    fun failedClearPhotoIsLogged() = testScope.runTest {
        subject = createSubject(factory.failingBabyRepository(babyRepository))
        subject.open()
        subject.clearPhoto()

        val entry = logger.entries.single()

        assertThat(entry.level).isEqualTo(FakeLogger.Level.ERROR)
    }

    @Test
    fun photoEditIsSavingWhilePhotoIsWritten() = testScope.runTest {
        subject = createSubject(factory.pausableBabyRepository(babyRepository))
        subject.open()
        subject.savePhoto(PHOTO::inputStream)

        val state = subject.state.value

        assertThat(state).isEqualTo(
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
        subject.open()
        subject.savePhoto(PHOTO::inputStream)
        subject.savePhoto(PHOTO::inputStream)

        val writeCount = repository.writeCount

        assertThat(writeCount).isEqualTo(1)
    }

    private fun createSubject(repository: BabyRepository): PhotoEditor {
        return PhotoEditor(
            babyRepository = repository,
            logger = logger,
            scope = editorScope
        )
    }

    private companion object {
        private val PHOTO = byteArrayOf(
            1,
            2,
            3
        )
    }
}
