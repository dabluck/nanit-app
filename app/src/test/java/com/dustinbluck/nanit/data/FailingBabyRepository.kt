package com.dustinbluck.nanit.data

import java.io.InputStream
import java.time.LocalDate

internal class FailingBabyRepository(delegate: BabyRepository) : BabyRepository by delegate {
    override suspend fun setName(name: String): Boolean {
        return false
    }

    override suspend fun setBirthday(birthday: LocalDate): Boolean {
        return false
    }

    override suspend fun setPhoto(openPhoto: () -> InputStream): Boolean {
        return false
    }
}
