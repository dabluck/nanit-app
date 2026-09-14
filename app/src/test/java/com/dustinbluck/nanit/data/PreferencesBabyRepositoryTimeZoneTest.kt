package com.dustinbluck.nanit.data

import com.dustinbluck.nanit.deps.TestDependencyFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

@RunWith(Parameterized::class)
internal class PreferencesBabyRepositoryTimeZoneTest(
    private val saveTimeZone: String,
    private val readTimeZone: String,
    private val birthday: LocalDate
) {
    @get:Rule
    val folder = TemporaryFolder()

    private val testScope = TestScope()

    private lateinit var originalTimeZone: TimeZone

    private lateinit var factory: TestDependencyFactory

    private lateinit var dataStoreJob: Job

    private lateinit var subject: PreferencesBabyRepository

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        setDefaultTimeZone(saveTimeZone)
        factory = TestDependencyFactory(
            root = folder.root,
            testScope = testScope
        )
        dataStoreJob = Job(testScope.backgroundScope.coroutineContext[Job])
        subject = factory.babyRepository(
            dataStore = factory.babyDataStore(
                scope = CoroutineScope(testScope.backgroundScope.coroutineContext + dataStoreJob)
            )
        )
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun birthdayIsUnchangedAfterTimeZoneChanges() = testScope.runTest {
        subject.setBirthday(birthday)
        setDefaultTimeZone(readTimeZone)

        val baby = subject.baby.first()

        assertThat(baby.birthday).isEqualTo(birthday)
    }

    @Test
    fun birthdayIsUnchangedAfterRestartInNewTimeZone() = testScope.runTest {
        subject.setBirthday(birthday)
        dataStoreJob.cancelAndJoin()
        setDefaultTimeZone(readTimeZone)
        val restartedRepository = factory.babyRepository()

        val baby = restartedRepository.baby.first()

        assertThat(baby.birthday).isEqualTo(birthday)
    }

    private fun setDefaultTimeZone(id: String) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(id)))
    }

    private companion object {
        private const val JERUSALEM = "Asia/Jerusalem"
        private const val NEW_YORK = "America/New_York"
        private const val KIRITIMATI = "Pacific/Kiritimati"
        private const val PAGO_PAGO = "Pacific/Pago_Pago"
        private val TIME_ZONE_CHANGES = listOf(
            JERUSALEM to NEW_YORK,
            NEW_YORK to JERUSALEM,
            KIRITIMATI to PAGO_PAGO,
            PAGO_PAGO to KIRITIMATI
        )
        private val BIRTHDAYS = listOf(
            LocalDate.of(
                2025,
                3,
                14
            ),
            LocalDate.of(
                2026,
                3,
                8
            ),
            LocalDate.of(
                2026,
                3,
                20
            ),
            LocalDate.of(
                2026,
                3,
                27
            ),
            LocalDate.of(
                2026,
                10,
                25
            ),
            LocalDate.of(
                2026,
                10,
                28
            ),
            LocalDate.of(
                2026,
                11,
                1
            ),
            LocalDate.of(
                2012,
                9,
                23
            ),
            LocalDate.of(
                2024,
                2,
                29
            ),
            LocalDate.of(
                2025,
                12,
                31
            ),
            LocalDate.of(
                2026,
                1,
                1
            ),
            LocalDate.of(
                1969,
                12,
                31
            ),
            LocalDate.of(
                1970,
                1,
                1
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{2} saved in {0}, read in {1}")
        fun parameters(): List<Array<Any>> {
            return TIME_ZONE_CHANGES.flatMap { (saveTimeZone, readTimeZone) ->
                BIRTHDAYS.map { birthday ->
                    arrayOf(
                        saveTimeZone,
                        readTimeZone,
                        birthday
                    )
                }
            }
        }
    }
}
