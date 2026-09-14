package com.dustinbluck.nanit.data

import com.dustinbluck.nanit.deps.TestDependencyFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.InputStream

internal class PhotoManagerTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val testScope = TestScope()

    private lateinit var factory: TestDependencyFactory

    private lateinit var subject: PhotoManager

    @Before
    fun setUp() {
        factory = TestDependencyFactory(
            root = folder.root,
            testScope = testScope
        )
        subject = factory.photoManager()
    }

    @Test
    fun prepareCameraPhotoCreatesPhotoDirectory() {
        subject.prepareCameraPhoto()

        val isDirectory = factory.cameraPhotoFile().parentFile?.isDirectory

        assertThat(isDirectory).isTrue()
    }

    @Test
    fun prepareCameraPhotoDeletesPreviousPhoto() {
        subject.prepareCameraPhoto()
        factory.cameraPhotoFile().writeBytes(PHOTO)
        subject.prepareCameraPhoto()

        val exists = factory.cameraPhotoFile().exists()

        assertThat(exists).isFalse()
    }

    @Test
    fun openCameraPhotoReadsPhoto() {
        subject.prepareCameraPhoto()
        factory.cameraPhotoFile().writeBytes(PHOTO)

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
