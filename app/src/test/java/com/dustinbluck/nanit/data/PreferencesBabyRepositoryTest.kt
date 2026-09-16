package com.dustinbluck.nanit.data

import app.cash.turbine.test
import com.dustinbluck.nanit.deps.TestDependencyFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate

internal class PreferencesBabyRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val testScope = TestScope()

    private lateinit var factory: TestDependencyFactory

    private lateinit var subject: PreferencesBabyRepository

    @Before
    fun setUp() {
        factory = TestDependencyFactory(
            root = folder.root,
            testScope = testScope
        )
        subject = factory.babyRepository()
    }

    @Test
    fun babyIsEmptyWhenNothingIsStored() = testScope.runTest {
        val baby = subject.baby.first()

        assertThat(baby).isEqualTo(
            Baby(
                name = null,
                birthday = null,
                photo = null
            )
        )
    }

    @Test
    fun setNameStoresName() = testScope.runTest {
        subject.setName(NAME)

        val baby = subject.baby.first()

        assertThat(baby).isEqualTo(
            Baby(
                name = NAME,
                birthday = null,
                photo = null
            )
        )
    }

    @Test
    fun setBirthdayStoresBirthday() = testScope.runTest {
        subject.setBirthday(BIRTHDAY)

        val baby = subject.baby.first()

        assertThat(baby).isEqualTo(
            Baby(
                name = null,
                birthday = BIRTHDAY,
                photo = null
            )
        )
    }

    @Test
    fun storedDetailsSurviveRestart() = testScope.runTest {
        subject.setName(NAME)
        subject.setBirthday(BIRTHDAY)
        val restartedRepository = factory.restartedBabyRepository()

        val baby = restartedRepository.baby.first()

        assertThat(baby).isEqualTo(
            Baby(
                name = NAME,
                birthday = BIRTHDAY,
                photo = null
            )
        )
    }

    @Test
    fun setPhotoStoresPhotoBytes() = testScope.runTest {
        subject.setPhoto(PHOTO::inputStream)

        val photo = subject.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun setPhotoDeletesPreviousPhoto() = testScope.runTest {
        subject.setPhoto(PHOTO::inputStream)
        subject.setPhoto(OTHER_PHOTO::inputStream)

        val photo = subject.baby.first().photo

        assertThat(factory.babyPhotoDirectory().list()).asList().containsExactly(photo?.name)
    }

    @Test
    fun photoIsNullWhenPhotoFileIsMissing() = testScope.runTest {
        subject.setPhoto(PHOTO::inputStream)
        factory.babyPhotoDirectory().deleteRecursively()

        val baby = subject.baby.first()

        assertThat(baby.photo).isNull()
    }

    @Test
    fun clearPhotoRemovesPhoto() = testScope.runTest {
        subject.setPhoto(PHOTO::inputStream)
        subject.clearPhoto()

        val baby = subject.baby.first()

        assertThat(baby.photo).isNull()
    }

    @Test
    fun clearPhotoDeletesPhotoFile() = testScope.runTest {
        subject.setPhoto(PHOTO::inputStream)
        subject.clearPhoto()

        val photos = factory.babyPhotoDirectory().list()

        assertThat(photos).isEmpty()
    }

    @Test
    fun clearPhotoKeepsOtherDetails() = testScope.runTest {
        subject.setName(NAME)
        subject.setPhoto(PHOTO::inputStream)
        subject.clearPhoto()

        val baby = subject.baby.first()

        assertThat(baby).isEqualTo(
            Baby(
                name = NAME,
                birthday = null,
                photo = null
            )
        )
    }

    @Test
    fun setPhotoReturnsFalseWhenPhotoCannotBeRead() = testScope.runTest {
        val result = subject.setPhoto(::FailingInputStream)

        assertThat(result).isFalse()
    }

    @Test
    fun setPhotoClosesPhoto() = testScope.runTest {
        val photo = TrackingInputStream(PHOTO.inputStream())
        subject.setPhoto {
            photo
        }

        val isClosed = photo.isClosed

        assertThat(isClosed).isTrue()
    }

    @Test
    fun failedSetPhotoClosesPhoto() = testScope.runTest {
        val photo = TrackingInputStream(FailingInputStream())
        subject.setPhoto {
            photo
        }

        val isClosed = photo.isClosed

        assertThat(isClosed).isTrue()
    }

    @Test
    fun failedSetPhotoKeepsPreviousPhoto() = testScope.runTest {
        subject.setPhoto(PHOTO::inputStream)
        subject.setPhoto(::FailingInputStream)

        val photo = subject.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun failedSetPhotoDeletesPartialPhoto() = testScope.runTest {
        subject.setPhoto(::FailingInputStream)

        val photos = factory.babyPhotoDirectory().list()

        assertThat(photos).isEmpty()
    }

    @Test
    fun setPhotoReturnsFalseWhenPhotoAccessIsDenied() = testScope.runTest {
        val result = subject.setPhoto(::openDeniedPhoto)

        assertThat(result).isFalse()
    }

    @Test
    fun concurrentSetPhotoStoresLastPhoto() = testScope.runTest {
        listOf(
            launch {
                subject.setPhoto(PHOTO::inputStream)
            },
            launch {
                subject.setPhoto(OTHER_PHOTO::inputStream)
            }
        ).joinAll()

        val photo = subject.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(OTHER_PHOTO)
    }

    @Test
    fun concurrentSetPhotoLeavesOnlyTheStoredPhoto() = testScope.runTest {
        listOf(
            launch {
                subject.setPhoto(PHOTO::inputStream)
            },
            launch {
                subject.setPhoto(OTHER_PHOTO::inputStream)
            }
        ).joinAll()
        val photo = subject.baby.first().photo

        val photos = factory.babyPhotoDirectory().list()

        assertThat(photos).asList().containsExactly(photo?.name)
    }

    @Test
    fun setNameReturnsFalseWhenPreferencesCannotBeWritten() = testScope.runTest {
        subject = factory.babyRepository(dataStore = factory.unwritableBabyDataStore())

        val result = subject.setName(NAME)

        assertThat(result).isFalse()
    }

    @Test
    fun setBirthdayReturnsFalseWhenPreferencesCannotBeWritten() = testScope.runTest {
        subject = factory.babyRepository(dataStore = factory.unwritableBabyDataStore())

        val result = subject.setBirthday(BIRTHDAY)

        assertThat(result).isFalse()
    }

    @Test
    fun setPhotoReturnsFalseWhenPreferencesCannotBeWritten() = testScope.runTest {
        subject = factory.babyRepository(dataStore = factory.unwritableBabyDataStore())

        val result = subject.setPhoto(PHOTO::inputStream)

        assertThat(result).isFalse()
    }

    @Test
    fun setPhotoDeletesPhotoWhenPreferencesCannotBeWritten() = testScope.runTest {
        subject = factory.babyRepository(dataStore = factory.unwritableBabyDataStore())
        subject.setPhoto(PHOTO::inputStream)

        val photos = factory.babyPhotoDirectory().list()

        assertThat(photos).isEmpty()
    }

    @Test
    fun clearPhotoReturnsFalseWhenPreferencesCannotBeWritten() = testScope.runTest {
        val dataStore = factory.failingWritesBabyDataStore()
        subject = factory.babyRepository(dataStore = dataStore)
        subject.setPhoto(PHOTO::inputStream)
        dataStore.failWrites()

        val result = subject.clearPhoto()

        assertThat(result).isFalse()
    }

    @Test
    fun clearPhotoKeepsPhotoWhenPreferencesCannotBeWritten() = testScope.runTest {
        val dataStore = factory.failingWritesBabyDataStore()
        subject = factory.babyRepository(dataStore = dataStore)
        subject.setPhoto(PHOTO::inputStream)
        dataStore.failWrites()
        subject.clearPhoto()

        val photo = subject.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun babyIsEmptyWhenPreferencesAreCorrupt() = testScope.runTest {
        factory.babyDataStoreFile().writeText(CORRUPT_DATA_STORE_CONTENTS)

        val baby = subject.baby.first()

        assertThat(baby).isEqualTo(
            Baby(
                name = null,
                birthday = null,
                photo = null
            )
        )
    }

    @Test
    fun setNameStoresNameWhenPreferencesAreCorrupt() = testScope.runTest {
        factory.babyDataStoreFile().writeText(CORRUPT_DATA_STORE_CONTENTS)
        subject.setName(NAME)

        val baby = subject.baby.first()

        assertThat(baby.name).isEqualTo(NAME)
    }

    @Test
    fun babyEmitsWhenValueChanges() = testScope.runTest {
        subject.baby.test {
            skipItems(1)

            subject.setName(NAME)

            assertThat(awaitItem()).isEqualTo(
                Baby(
                    name = NAME,
                    birthday = null,
                    photo = null
                )
            )
        }
    }

    @Test
    fun babyEmitsWhenPhotoIsReplaced() = testScope.runTest {
        subject.setPhoto(PHOTO::inputStream)

        subject.baby.test {
            skipItems(1)

            subject.setPhoto(OTHER_PHOTO::inputStream)

            assertThat(awaitItem().photo?.readBytes()).isEqualTo(OTHER_PHOTO)
        }
    }

    private fun openDeniedPhoto(): InputStream {
        throw SecurityException(ACCESS_DENIED_MESSAGE)
    }

    private companion object {
        private const val NAME = "Dustin"
        private const val READ_FAILURE_MESSAGE = "read failed"
        private const val ACCESS_DENIED_MESSAGE = "access denied"
        private const val CORRUPT_DATA_STORE_CONTENTS = "not a preferences file"
        private val BIRTHDAY = LocalDate.of(
            2025,
            3,
            14
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

    private class FailingInputStream : InputStream() {
        override fun read(): Int {
            throw IOException(READ_FAILURE_MESSAGE)
        }
    }

    private class TrackingInputStream(photo: InputStream) : FilterInputStream(photo) {
        var isClosed = false
            private set

        override fun close() {
            isClosed = true
            super.close()
        }
    }
}
