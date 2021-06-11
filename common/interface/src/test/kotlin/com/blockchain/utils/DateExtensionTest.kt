package com.blockchain.utils

import org.amshove.kluent.`should equal`
import org.junit.Test
import java.time.ZonedDateTime

class DateExtensionTest {

    @Test
    fun `29th of Feb should be the last day of the month`() {
        val date = ZonedDateTime.parse("2020-02-29T12:00:00.000Z")
        date.isLastDayOfTheMonth() `should equal` true
    }

    @Test
    fun `28th of Feb should not be the last day of the month`() {
        val date = ZonedDateTime.parse("2020-02-28T00:12:00.000Z")
        date.isLastDayOfTheMonth() `should equal` false
    }

    @Test
    fun `28th of Feb should be the last day of the month`() {
        val date = ZonedDateTime.parse("2021-02-28T00:12:00.000Z")
        date.isLastDayOfTheMonth() `should equal` true
    }

    @Test
    fun `31st of Dec should be the last day of the month`() {
        val date = ZonedDateTime.parse("2020-12-31T12:00:00.000Z")
        date.isLastDayOfTheMonth() `should equal` true
    }

    @Test
    fun `30th of Dec should not be the last day of the month`() {
        val date = ZonedDateTime.parse("2020-12-30T12:00:00.000Z")
        date.isLastDayOfTheMonth() `should equal` false
    }

    @Test
    fun `30th of Nov should be the last day of the month`() {
        val date = ZonedDateTime.parse("2020-11-30T12:00:00.000Z")
        date.isLastDayOfTheMonth() `should equal` true
    }

    @Test
    fun `29th of Nov should not be the last day of the month`() {
        val date = ZonedDateTime.parse("2020-11-29T12:00:00.000Z")
        date.isLastDayOfTheMonth() `should equal` false
    }

    @Test
    fun `1st of June should not be the last day of the month`() {
        val date = ZonedDateTime.parse("2020-06-01T12:00:00.000Z")
        date.isLastDayOfTheMonth() `should equal` false
    }

    @Test
    fun `15th of January should not be the last day of the month`() {
        val date = ZonedDateTime.parse("2020-01-15T12:00:00.000Z")
        date.isLastDayOfTheMonth() `should equal` false
    }
}