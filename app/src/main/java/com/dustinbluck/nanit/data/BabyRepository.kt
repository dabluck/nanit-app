package com.dustinbluck.nanit.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface BabyRepository {
    val baby: Flow<Baby>

    suspend fun setName(name: String)

    suspend fun setBirthday(birthday: LocalDate)
}
