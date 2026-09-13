package com.dustinbluck.nanit.data

import java.io.File
import java.time.LocalDate

data class Baby(
    val name: String?,
    val birthday: LocalDate?,
    val photo: File?
)
