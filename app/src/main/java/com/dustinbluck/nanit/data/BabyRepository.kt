package com.dustinbluck.nanit.data

import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.time.LocalDate

interface BabyRepository {
    val baby: Flow<Baby>

    suspend fun setName(name: String): Boolean

    suspend fun setBirthday(birthday: LocalDate): Boolean

    suspend fun setPhoto(openPhoto: () -> InputStream): Boolean
}
