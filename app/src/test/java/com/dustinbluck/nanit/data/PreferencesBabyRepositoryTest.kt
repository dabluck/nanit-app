package com.dustinbluck.nanit.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
internal class PreferencesBabyRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var photoDirectory: File

    private lateinit var subject: PreferencesBabyRepository

    @Before
    fun setUp() {
        val dataStore = PreferenceDataStoreFactory.create {
            File(
                folder.root,
                FILE_NAME
            )
        }
        photoDirectory = File(
            folder.root,
            PHOTO_DIRECTORY_NAME
        )
        subject = PreferencesBabyRepository(
            dataStore = dataStore,
            photoDirectory = photoDirectory,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun babyIsEmptyWhenNothingIsStored() = runTest {
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
    fun setNameStoresName() = runTest {
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
    fun setBirthdayStoresBirthday() = runTest {
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
    fun setPhotoStoresPhotoBytes() = runTest {
        subject.setPhoto(PHOTO.inputStream())

        val photo = subject.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun setPhotoDeletesPreviousPhoto() = runTest {
        subject.setPhoto(PHOTO.inputStream())
        subject.setPhoto(OTHER_PHOTO.inputStream())

        val photo = subject.baby.first().photo

        assertThat(photoDirectory.list()).asList().containsExactly(photo?.name)
    }

    @Test
    fun photoIsNullWhenPhotoFileIsMissing() = runTest {
        subject.setPhoto(PHOTO.inputStream())
        photoDirectory.deleteRecursively()

        val baby = subject.baby.first()

        assertThat(baby.photo).isNull()
    }

    @Test
    fun setNameReturnsTrue() = runTest {
        val result = subject.setName(NAME)

        assertThat(result).isTrue()
    }

    @Test
    fun setBirthdayReturnsTrue() = runTest {
        val result = subject.setBirthday(BIRTHDAY)

        assertThat(result).isTrue()
    }

    @Test
    fun setPhotoReturnsTrue() = runTest {
        val result = subject.setPhoto(PHOTO.inputStream())

        assertThat(result).isTrue()
    }

    @Test
    fun setPhotoReturnsFalseWhenPhotoCannotBeRead() = runTest {
        val result = subject.setPhoto(FAILING_PHOTO)

        assertThat(result).isFalse()
    }

    @Test
    fun failedSetPhotoKeepsPreviousPhoto() = runTest {
        subject.setPhoto(PHOTO.inputStream())
        subject.setPhoto(FAILING_PHOTO)

        val photo = subject.baby.first().photo

        assertThat(photo?.readBytes()).isEqualTo(PHOTO)
    }

    @Test
    fun failedSetPhotoDeletesPartialPhoto() = runTest {
        subject.setPhoto(FAILING_PHOTO)

        val photos = photoDirectory.list()

        assertThat(photos).isEmpty()
    }

    @Test
    fun babyEmitsWhenValueChanges() = runTest {
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
    fun babyEmitsWhenPhotoIsReplaced() = runTest {
        subject.setPhoto(PHOTO.inputStream())

        subject.baby.test {
            skipItems(1)

            subject.setPhoto(OTHER_PHOTO.inputStream())

            assertThat(awaitItem().photo?.readBytes()).isEqualTo(OTHER_PHOTO)
        }
    }

    private companion object {
        private const val FILE_NAME = "baby.preferences_pb"
        private const val PHOTO_DIRECTORY_NAME = "baby_photo"
        private const val NAME = "Dustin"
        private const val READ_FAILURE_MESSAGE = "read failed"
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
        private val FAILING_PHOTO = object : InputStream() {
            override fun read(): Int {
                throw IOException(READ_FAILURE_MESSAGE)
            }
        }
    }
}
