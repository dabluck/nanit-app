package com.dustinbluck.nanit.ui.birthday

import java.time.LocalDate
import java.time.Period

object AgeCalculatorUtil {
    private const val MONTHS_PER_YEAR = 12

    /**
     * You're a month old when the calendar reaches the same day of the month.
     * If born on a leap day, it tilts over to March 1 on non leap years.
     *
     * @return null if your birthday is in the future
     *
     */
    fun calculateAge(
        birthday: LocalDate,
        today: LocalDate
    ): Age? {
        if (birthday.isAfter(today)) {
            return null
        }
        val totalMonths = Period.between(
            birthday,
            today
        ).toTotalMonths().toInt()
        return if (totalMonths < MONTHS_PER_YEAR) {
            Age(
                value = totalMonths,
                unit = AgeUnit.MONTHS
            )
        } else {
            Age(
                value = totalMonths / MONTHS_PER_YEAR,
                unit = AgeUnit.YEARS
            )
        }
    }
}
