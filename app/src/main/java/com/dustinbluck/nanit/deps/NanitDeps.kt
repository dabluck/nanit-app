package com.dustinbluck.nanit.deps

import com.dustinbluck.nanit.data.BabyRepository
import com.dustinbluck.nanit.data.PhotoManager
import com.dustinbluck.nanit.logging.Logger
import java.time.Clock

interface NanitDeps {
    val babyRepository: BabyRepository

    val photoManager: PhotoManager

    val logger: Logger

    val clock: Clock

    companion object {
        lateinit var instance: NanitDeps
    }
}
