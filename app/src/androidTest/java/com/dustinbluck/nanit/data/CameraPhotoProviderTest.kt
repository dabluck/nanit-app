package com.dustinbluck.nanit.data

import android.app.Application
import android.content.ContentResolver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dustinbluck.nanit.deps.NanitDepsImpl
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
internal class CameraPhotoProviderTest {
    private lateinit var contentResolver: ContentResolver

    private lateinit var subject: PhotoManager

    @Before
    fun setUp() {
        val application =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        contentResolver = application.contentResolver
        subject = NanitDepsImpl(application).photoManager
    }

    @Test
    fun photoWrittenToCameraPhotoUriIsReadable() {
        subject.prepareCameraPhoto()
        contentResolver.openOutputStream(subject.cameraPhotoUri)?.use { output ->
            output.write(PHOTO)
        }

        val photo = subject.openCameraPhoto().use(InputStream::readBytes)

        assertThat(photo).isEqualTo(PHOTO)
    }

    private companion object {
        private val PHOTO = byteArrayOf(
            1,
            2,
            3
        )
    }
}
