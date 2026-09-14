package com.dustinbluck.nanit.deps

import com.dustinbluck.nanit.data.BabyRepository
import com.dustinbluck.nanit.data.PhotoManager
import com.dustinbluck.nanit.logging.Logger

interface NanitDeps {
    val babyRepository: BabyRepository

    val photoManager: PhotoManager

    val logger: Logger

    companion object {
        lateinit var instance: NanitDeps
    }
}
