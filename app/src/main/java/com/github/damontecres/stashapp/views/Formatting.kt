package com.github.damontecres.stashapp.views

import android.content.Context
import android.os.Build
import com.github.damontecres.stashapp.util.ServerPreferences
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Converts seconds into a Duration string where fractional seconds are removed
 */
fun durationToString(duration: Double): String {
    return duration
        .times(100L).toLong()
        .div(100L).toDuration(DurationUnit.SECONDS)
        .toString()
}

fun getRatingAsDecimalString(
    context: Context,
    rating100: Int,
    ratingsAsStars: Boolean? = null,
): String {
    val asStars = ratingsAsStars ?: ServerPreferences(context).ratingsAsStars
    return if (asStars) {
        (rating100 / 20.0).toString()
    } else {
        (rating100 / 10.0).toString()
    }
}

fun parseTimeToString(ts: Any?): String? {
    return if (ts == null) {
        null
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val dateTimeFormatter = DateTimeFormatter.ofPattern("eee, MMMM d, yyyy h:mm a")
            val dateTime =
                ZonedDateTime.parse(
                    ts.toString(),
                    DateTimeFormatter.ISO_DATE_TIME,
                )
            dateTime.format(dateTimeFormatter)
        } catch (ex: DateTimeParseException) {
            ts.toString()
        }
    } else {
        ts.toString()
    }
}
