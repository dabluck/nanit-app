package com.dustinbluck.nanit.ui.birthday

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.LocalDate

@RunWith(Parameterized::class)
internal class AgeCalculatorUtilCalendarTest(
    private val birthday: String,
    private val today: String,
    private val expectedAge: Int,
    private val expectedAgeUnit: AgeUnit
) {
    @Test
    fun ageMatchesCalendar() {
        val age = AgeCalculatorUtil.calculateAge(
            birthday = LocalDate.parse(birthday),
            today = LocalDate.parse(today)
        )

        assertThat(age).isEqualTo(
            Age(
                value = expectedAge,
                unit = expectedAgeUnit
            )
        )
    }

    private companion object {
        private val MONTHS = AgeUnit.MONTHS
        private val YEARS = AgeUnit.YEARS

        @JvmStatic
        @Parameterized.Parameters(name = "born {0}, on {1} is {2} {3}")
        fun parameters(): List<Array<Any>> {
            return listOf(
                row("2025-01-31", "2025-02-27", 0, MONTHS),
                row("2025-01-31", "2025-02-28", 0, MONTHS),
                row("2025-01-31", "2025-03-01", 1, MONTHS),
                row("2025-01-31", "2025-03-30", 1, MONTHS),
                row("2025-01-31", "2025-03-31", 2, MONTHS),
                row("2025-01-31", "2025-04-30", 2, MONTHS),
                row("2025-01-31", "2025-05-01", 3, MONTHS),
                row("2025-01-31", "2025-12-31", 11, MONTHS),
                row("2025-01-31", "2026-01-30", 11, MONTHS),
                row("2025-01-31", "2026-01-31", 1, YEARS),
                row("2025-03-31", "2025-04-30", 0, MONTHS),
                row("2025-03-31", "2025-05-01", 1, MONTHS),
                row("2025-01-30", "2025-02-28", 0, MONTHS),
                row("2025-01-30", "2025-03-01", 1, MONTHS),
                row("2025-01-29", "2025-02-28", 0, MONTHS),
                row("2024-01-29", "2024-02-29", 1, MONTHS),
                row("2024-01-30", "2024-02-29", 0, MONTHS),
                row("2025-02-28", "2025-03-27", 0, MONTHS),
                row("2025-02-28", "2025-03-28", 1, MONTHS),
                row("2024-02-29", "2024-03-28", 0, MONTHS),
                row("2024-02-29", "2024-03-29", 1, MONTHS),
                row("2024-02-29", "2025-01-29", 11, MONTHS),
                row("2024-02-29", "2025-02-28", 11, MONTHS),
                row("2024-02-29", "2025-03-01", 1, YEARS),
                row("2024-02-29", "2027-02-28", 2, YEARS),
                row("2024-02-29", "2027-03-01", 3, YEARS),
                row("2024-02-29", "2028-02-28", 3, YEARS),
                row("2024-02-29", "2028-02-29", 4, YEARS),
                row("2000-02-29", "2001-02-28", 11, MONTHS),
                row("2000-02-29", "2001-03-01", 1, YEARS),
                row("2096-02-29", "2100-02-28", 3, YEARS),
                row("2096-02-29", "2100-03-01", 4, YEARS),
                row("2023-02-28", "2024-02-27", 11, MONTHS),
                row("2023-02-28", "2024-02-28", 1, YEARS),
                row("2023-02-28", "2024-02-29", 1, YEARS),
                row("2023-03-01", "2024-02-29", 11, MONTHS),
                row("2023-03-01", "2024-03-01", 1, YEARS),
                row("2024-12-31", "2025-01-01", 0, MONTHS),
                row("2024-12-31", "2025-01-30", 0, MONTHS),
                row("2024-12-31", "2025-01-31", 1, MONTHS),
                row("2024-12-31", "2025-12-30", 11, MONTHS),
                row("2024-12-31", "2025-12-31", 1, YEARS),
                row("2025-01-01", "2025-12-31", 11, MONTHS),
                row("2025-01-01", "2026-01-01", 1, YEARS),
                row("2025-12-01", "2025-12-31", 0, MONTHS),
                row("2025-12-01", "2026-01-01", 1, MONTHS),
                row("2025-12-15", "2026-01-14", 0, MONTHS),
                row("2025-12-15", "2026-01-15", 1, MONTHS),
                row("2026-01-01", "2026-01-01", 0, MONTHS),
                row("2026-01-15", "2026-02-14", 0, MONTHS),
                row("2026-01-15", "2026-02-15", 1, MONTHS),
                row("2026-01-31", "2026-02-28", 0, MONTHS),
                row("2026-01-31", "2026-03-01", 1, MONTHS),
                row("2026-02-28", "2027-02-28", 1, YEARS),
                row("2026-02-28", "2028-02-29", 2, YEARS),
                row("2026-03-01", "2028-02-29", 1, YEARS),
                row("2026-03-08", "2026-11-01", 7, MONTHS),
                row("2026-03-29", "2026-04-28", 0, MONTHS),
                row("2026-03-29", "2026-04-29", 1, MONTHS),
                row("2026-05-31", "2026-06-30", 0, MONTHS),
                row("2026-05-31", "2026-07-01", 1, MONTHS),
                row("2026-09-15", "2026-09-15", 0, MONTHS),
                row("2026-09-15", "2027-09-14", 11, MONTHS),
                row("2026-09-15", "2027-09-15", 1, YEARS),
                row("2026-12-31", "2027-01-31", 1, MONTHS),
                row("2026-12-31", "2027-12-31", 1, YEARS),
                row("2025-09-15", "2026-09-15", 1, YEARS),
                row("2025-09-16", "2026-09-15", 11, MONTHS),
                row("2024-02-29", "2026-02-28", 1, YEARS),
                row("2024-02-29", "2026-03-01", 2, YEARS),
                row("2014-03-14", "2026-03-13", 11, YEARS),
                row("2014-03-14", "2026-03-14", 12, YEARS),
                row("2013-03-14", "2026-03-14", 13, YEARS),
                row("1926-09-15", "2026-09-14", 99, YEARS),
                row("1926-09-15", "2026-09-15", 100, YEARS),
                row("1969-12-31", "1970-01-01", 0, MONTHS),
                row("1969-12-31", "1970-01-31", 1, MONTHS),
                row("1969-12-31", "1970-12-31", 1, YEARS)
            )
        }

        private fun row(
            birthday: String,
            today: String,
            expectedAge: Int,
            expectedAgeUnit: AgeUnit
        ): Array<Any> {
            return arrayOf(
                birthday,
                today,
                expectedAge,
                expectedAgeUnit
            )
        }
    }
}
