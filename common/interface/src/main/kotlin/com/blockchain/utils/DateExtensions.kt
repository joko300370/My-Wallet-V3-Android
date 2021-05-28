package com.blockchain.utils

import org.apache.commons.lang3.time.DateUtils
import java.math.BigInteger
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

/**
 * Converts a [String] from an ISO 8601 date to a [Date] object. The receiving [String] can specify
 * both seconds AND seconds + milliseconds. This is necessary because the Coinify API seems to
 * return one buy specify the other in their documentation, and we can't be sure that we won't see
 * the documented format at some point. If the [String] is for some reason not parsable due to
 * otherwise incorrect formatting, the resulting [Date] will be null.
 *
 * The returned times will always be in UTC.
 *
 * @return A [Date] object or null if the [String] isn't formatted correctly.
 */
fun String.fromIso8601ToUtc(): Date? {
    val millisFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    val secondsFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    return try {
        DateUtils.parseDate(this, millisFormat, secondsFormat)
    } catch (e: ParseException) {
        e.printStackTrace()
        null
    }
}

fun Date.toUtcIso8601(): String {
    val s = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    s.timeZone = TimeZone.getTimeZone("UTC")
    return s.format(Date())
}

fun Date.toLocalTime(): Date {
    val calendar = Calendar.getInstance()
    val timeZone = calendar.timeZone
    val offset = timeZone.getOffset(this.time)

    return Date(this.time + offset)
}

fun ZonedDateTime.to12HourFormat(): String {
    val formatter = DateTimeFormatter.ofPattern("ha")
    return formatter.format(this).toString()
}

fun ZonedDateTime.isLastDayOfTheMonth(): Boolean {
    val nextDay = this.plusDays(1)
    return this.month != nextDay.month
}

/**
 * Takes a [Date] object and converts it to our standard date format, ie March 09, 2018 @11:47.
 *
 * @param locale The current [Locale].
 * @return A formatted [String] object.
 */
fun Date.toFormattedString(locale: Locale = Locale.getDefault()): String {
    val dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM)
    val timeFormat = SimpleDateFormat("hh:mm a", locale)
    val dateText = dateFormat.format(this)
    val timeText = timeFormat.format(this)

    return "$timeText on $dateText"
}

/**
 * Takes a [Date] object and converts it to our standard date format, ie March 09, 2018 @11:47.
 *
 * @param locale The current [Locale].
 * @return A formatted [String] object.
 */
fun Date.toFormattedDate(): String {
    val dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM)
    return dateFormat.format(this)
}

fun BigInteger.secondsToDays(): Long =
    ceil(this.toDouble() / SECONDS_OF_DAY).toLong()

fun Int.secondsToDays(): Int =
    ceil(this.toDouble() / SECONDS_OF_DAY).toInt()

private const val SECONDS_OF_DAY: Long = 86400
