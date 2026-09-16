package com.dustinbluck.nanit.ui.photo

import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.dustinbluck.nanit.data.PhotoManager
import com.dustinbluck.nanit.deps.NanitDeps
import java.io.FileNotFoundException
import java.io.InputStream

class PhotoPicker(
    val canTakePhoto: Boolean,
    val choosePhoto: () -> Unit,
    val takePhoto: () -> Unit
)

@Composable
fun rememberPhotoPicker(
    onPhotoPicked: (openPhoto: () -> InputStream) -> Unit,
    photoManager: PhotoManager = NanitDeps.instance.photoManager
): PhotoPicker {
    val context = LocalContext.current
    val contentResolver = context.applicationContext.contentResolver
    val canTakePhoto = remember(context) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null
    }
    val choosePhotoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                onPhotoPicked {
                    contentResolver.openInputStream(uri)
                        ?: throw FileNotFoundException(uri.toString())
                }
            }
        }
    val takePhotoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isPhotoTaken ->
            if (isPhotoTaken) {
                onPhotoPicked(photoManager::openCameraPhoto)
            }
        }
    return remember(
        canTakePhoto,
        choosePhotoLauncher,
        takePhotoLauncher,
        photoManager
    ) {
        PhotoPicker(
            canTakePhoto = canTakePhoto,
            choosePhoto = {
                choosePhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            takePhoto = {
                photoManager.prepareCameraPhoto()
                takePhotoLauncher.launch(photoManager.cameraPhotoUri)
            }
        )
    }
}
