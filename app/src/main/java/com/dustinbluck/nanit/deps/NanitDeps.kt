package com.dustinbluck.nanit.deps

import com.dustinbluck.nanit.data.BabyRepository

interface NanitDeps {
    val babyRepository: BabyRepository

    companion object {
        lateinit var instance: NanitDeps
    }
}
