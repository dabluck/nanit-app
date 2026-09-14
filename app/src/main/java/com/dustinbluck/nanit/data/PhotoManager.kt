package com.dustinbluck.nanit.data

import android.net.Uri
import java.io.File
import java.io.InputStream

class PhotoManager(
    private val produceCacheDirectory: () -> File,
    private val uriForFile: (File) -> Uri
) {
    private val cameraPhotoFile: File by lazy {
        produceCacheDirectory()
            .resolve(CAMERA_PHOTO_DIRECTORY_NAME)
            .resolve(CAMERA_PHOTO_FILE_NAME)
    }

    val cameraPhotoUri: Uri by lazy {
        uriForFile(cameraPhotoFile)
    }

    fun prepareCameraPhoto() {
        cameraPhotoFile.parentFile?.mkdirs()
        cameraPhotoFile.delete()
    }

    fun openCameraPhoto(): InputStream {
        return cameraPhotoFile.inputStream()
    }

    private companion object {
        private const val CAMERA_PHOTO_DIRECTORY_NAME = "camera_photo"
        private const val CAMERA_PHOTO_FILE_NAME = "photo.jpg"
    }
}
