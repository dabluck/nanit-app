package com.dustinbluck.nanit.data

import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.IOException
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

    // nothing cleans this up but it overwrites each share
    // could be a good idea to delete if it's older than a week or something
    fun writeBirthdayCard(bitmap: Bitmap): Uri? {
        val cardFile = produceCacheDirectory()
            .resolve(BIRTHDAY_CARD_DIRECTORY_NAME)
            .resolve(BIRTHDAY_CARD_FILE_NAME)
        cardFile.parentFile?.mkdirs()
        return try {
            cardFile.outputStream().use { output ->
                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    BIRTHDAY_CARD_QUALITY,
                    output
                )
            }
            uriForFile(cardFile)
        } catch (_: IOException) {
            cardFile.delete()
            null
        }
    }

    private companion object {
        private const val CAMERA_PHOTO_DIRECTORY_NAME = "camera_photo"
        private const val CAMERA_PHOTO_FILE_NAME = "photo.jpg"
        private const val BIRTHDAY_CARD_DIRECTORY_NAME = "nanit_birthday_share"
        private const val BIRTHDAY_CARD_FILE_NAME = "nanit_birthday_share.png"
        private const val BIRTHDAY_CARD_QUALITY = 100
    }
}
