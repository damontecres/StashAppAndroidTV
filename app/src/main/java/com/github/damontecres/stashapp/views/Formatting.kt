package com.github.damontecres.stashapp.views

import android.content.Context
import android.os.Build
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.util.StashServer
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Converts seconds into a Duration string where fractional seconds are removed
 */
fun durationToString(duration: Double): String =
    duration
        .times(100L)
        .toLong()
        .div(100L)
        .toDuration(DurationUnit.SECONDS)
        .toString()

fun getRatingAsDecimalString(
    rating100: Int,
    ratingsAsStars: Boolean,
): String =
    if (ratingsAsStars) {
        (rating100 / 20.0).toString()
    } else {
        if (rating100 % 10 == 0) {
            rating100 / 10
        } else {
            String.format(
                Locale.getDefault(),
                "%.1f",
                rating100 / 10.0,
            )
        }.toString()
    }

fun getRatingString(
    rating100: Int,
    ratingsAsStars: Boolean,
): String {
    val decimal = getRatingAsDecimalString(rating100, ratingsAsStars)
    return if (ratingsAsStars) {
        val starsStr =
            StashApplication
                .getApplication()
                .getString(R.string.stashapp_config_ui_editing_rating_system_type_options_stars)
        "$decimal $starsStr"
    } else {
        decimal
    }
}

fun parseTimeToString(ts: Any?): String? =
    if (ts == null) {
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

fun formatDate(date: String?) =
    if (date != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
            val localDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE)
            localDate.format(dateFormatter)
        } catch (ex: DateTimeParseException) {
            date
        }
    } else {
        date
    }

val String.fileNameFromPath
    get() = this.replace(Regex("""^.*[\\/]"""), "")

/**
 * Get the [String] representation for a [CriterionModifier]
 */
fun CriterionModifier.getString(context: Context): String =
    when (this) {
        CriterionModifier.EQUALS -> context.getString(R.string.stashapp_criterion_modifier_equals)
        CriterionModifier.NOT_EQUALS -> context.getString(R.string.stashapp_criterion_modifier_not_equals)
        CriterionModifier.LESS_THAN -> context.getString(R.string.stashapp_criterion_modifier_less_than)
        CriterionModifier.GREATER_THAN -> context.getString(R.string.stashapp_criterion_modifier_greater_than)
        CriterionModifier.IS_NULL -> context.getString(R.string.stashapp_criterion_modifier_is_null)
        CriterionModifier.NOT_NULL -> context.getString(R.string.stashapp_criterion_modifier_not_null)
        CriterionModifier.INCLUDES_ALL -> context.getString(R.string.stashapp_criterion_modifier_includes_all)
        CriterionModifier.INCLUDES -> context.getString(R.string.stashapp_criterion_modifier_includes)
        CriterionModifier.EXCLUDES -> context.getString(R.string.stashapp_criterion_modifier_excludes)
        CriterionModifier.MATCHES_REGEX -> context.getString(R.string.stashapp_criterion_modifier_matches_regex)
        CriterionModifier.NOT_MATCHES_REGEX -> context.getString(R.string.stashapp_criterion_modifier_not_matches_regex)
        CriterionModifier.BETWEEN -> context.getString(R.string.stashapp_criterion_modifier_between)
        CriterionModifier.NOT_BETWEEN -> context.getString(R.string.stashapp_criterion_modifier_not_between)
        CriterionModifier.UNKNOWN__ -> "Unknown"
    }

private val abbrevSuffixes = listOf("", "K", "M", "B")

/**
 * Format a number by abbreviation, eg 5533 => 5.5K
 */
fun abbreviateCounter(counter: Int): String {
    var unit = 0
    var count = counter.toDouble()
    while (count >= 1000 && unit + 1 < abbrevSuffixes.size) {
        count /= 1000
        unit++
    }
    return String.format(Locale.getDefault(), "%.1f%s", count, abbrevSuffixes[unit])
}

val byteSuffixes = listOf("B", "KB", "MB", "GB", "TB")
val byteRateSuffixes = listOf("bps", "kbps", "mbps", "gbps", "tbps")

/**
 * Format bytes
 */
fun formatBytes(
    bytes: Int,
    suffixes: List<String> = byteSuffixes,
) = formatBytes(bytes.toLong(), suffixes)

fun formatBytes(
    bytes: Long,
    suffixes: List<String> = byteSuffixes,
): String {
    var unit = 0
    var count = bytes.toDouble()
    while (count >= 1024 && unit + 1 < suffixes.size) {
        count /= 1024
        unit++
    }
    return String.format(Locale.getDefault(), "%.2f%s", count, suffixes[unit])
}

/**
 * Formats a number which may abbreviate it or add commas, etc
 *
 * @param number the number to format
 * @param abbreviateCounters whether to use the server-side abbreviateCounters settings
 */
fun formatNumber(
    number: Int,
    abbreviateCounters: Boolean = StashServer.requireCurrentServer().serverPreferences.abbreviateCounters,
): String =
    if (abbreviateCounters) {
        abbreviateCounter(number)
    } else {
        java.text.NumberFormat
            .getNumberInstance()
            .format(number)
    }
