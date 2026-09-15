package com.dustinbluck.nanit.ui.birthday

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

internal class AgeCalculatorUtilTest {
    @Test
    fun ageIsZeroMonthsOnDayOfBirth() {
        val age = ageOn("2025-03-14")

        assertThat(age).isEqualTo(months(0))
    }

    @Test
    fun ageIsZeroMonthsOnDayAfterBirth() {
        val age = ageOn("2025-03-15")

        assertThat(age).isEqualTo(months(0))
    }

    @Test
    fun ageIsZeroMonthsOnDayBeforeFirstMonth() {
        val age = ageOn("2025-04-13")

        assertThat(age).isEqualTo(months(0))
    }

    @Test
    fun ageIsOneMonthOnFirstMonth() {
        val age = ageOn("2025-04-14")

        assertThat(age).isEqualTo(months(1))
    }

    @Test
    fun ageIsSixMonthsOnSixthMonth() {
        val age = ageOn("2025-09-14")

        assertThat(age).isEqualTo(months(6))
    }

    @Test
    fun ageIsElevenMonthsOnDayBeforeFirstBirthday() {
        val age = ageOn("2026-03-13")

        assertThat(age).isEqualTo(months(11))
    }

    @Test
    fun ageIsOneYearOnFirstBirthday() {
        val age = ageOn("2026-03-14")

        assertThat(age).isEqualTo(years(1))
    }

    @Test
    fun ageIsOneYearThirteenMonthsAfterBirth() {
        val age = ageOn("2026-04-14")

        assertThat(age).isEqualTo(years(1))
    }

    @Test
    fun ageIsOneYearOnDayBeforeSecondBirthday() {
        val age = ageOn("2027-03-13")

        assertThat(age).isEqualTo(years(1))
    }

    @Test
    fun ageIsTwoYearsOnSecondBirthday() {
        val age = ageOn("2027-03-14")

        assertThat(age).isEqualTo(years(2))
    }

    @Test
    fun ageIsTwelveYearsOnTwelfthBirthday() {
        val age = ageOn("2037-03-14")

        assertThat(age).isEqualTo(years(12))
    }

    @Test
    fun ageKeepsCountingYearsPastTwelve() {
        val age = ageOn("2038-03-14")

        assertThat(age).isEqualTo(years(13))
    }

    @Test
    fun birthdayTomorrowHasNoAge() {
        val age = ageOn("2025-03-13")

        assertThat(age).isNull()
    }

    @Test
    fun birthdayTwoMonthsAwayHasNoAge() {
        val age = ageOn("2025-01-14")

        assertThat(age).isNull()
    }

    @Test
    fun birthdayTwoYearsAwayHasNoAge() {
        val age = ageOn("2023-03-14")

        assertThat(age).isNull()
    }

    private fun ageOn(today: String): Age? {
        return AgeCalculatorUtil.calculateAge(
            birthday = BIRTHDAY,
            today = LocalDate.parse(today)
        )
    }

    private fun months(value: Int): Age {
        return Age(
            value = value,
            unit = AgeUnit.MONTHS
        )
    }

    private fun years(value: Int): Age {
        return Age(
            value = value,
            unit = AgeUnit.YEARS
        )
    }

    private companion object {
        private val BIRTHDAY = LocalDate.parse("2025-03-14")
    }
}
