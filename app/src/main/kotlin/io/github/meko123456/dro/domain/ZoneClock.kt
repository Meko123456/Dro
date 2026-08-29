package io.github.meko123456.dro.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Reads a target zone's clock relative to a home zone at one instant.
 *
 * Everything is computed for the given [Instant], so offsets follow daylight-saving rules
 * on that date rather than the zone's standard offset — London is 4 h behind Tbilisi in
 * January and 3 h behind in July, and the answer flips at the exact transition instant.
 */
object ZoneClock {

    /**
     * @property localTime wall-clock time in the target zone
     * @property localDate calendar date in the target zone
     * @property dayShift target date minus home date in days: `-1` yesterday, `0` same day, `+1` tomorrow
     * @property offsetMinutes target UTC offset minus home UTC offset, in minutes (positive = ahead of home)
     */
    data class Reading(
        val localTime: LocalTime,
        val localDate: LocalDate,
        val dayShift: Int,
        val offsetMinutes: Int,
    )

    fun read(instant: Instant, home: ZoneId, target: ZoneId): Reading {
        val homeDateTime = instant.atZone(home)
        val targetDateTime = instant.atZone(target)
        val dayShift = ChronoUnit.DAYS.between(homeDateTime.toLocalDate(), targetDateTime.toLocalDate()).toInt()
        val offsetMinutes = (targetDateTime.offset.totalSeconds - homeDateTime.offset.totalSeconds) / 60
        return Reading(
            localTime = targetDateTime.toLocalTime(),
            localDate = targetDateTime.toLocalDate(),
            dayShift = dayShift,
            offsetMinutes = offsetMinutes,
        )
    }

    /**
     * Human label for an offset relative to home: `same time`, `+3h`, `-1h 30m`, `+45m`.
     * Whole hours drop the minutes; sub-hour offsets drop the hours.
     */
    fun offsetLabel(offsetMinutes: Int): String {
        if (offsetMinutes == 0) return "same time"
        val sign = if (offsetMinutes > 0) "+" else "-"
        val hours = abs(offsetMinutes) / 60
        val minutes = abs(offsetMinutes) % 60
        return buildString {
            append(sign)
            if (hours > 0) append(hours).append('h')
            if (minutes > 0) {
                if (hours > 0) append(' ')
                append(minutes).append('m')
            }
        }
    }

    /** `yesterday` / `tomorrow` for a one-day shift, null when the dates match. */
    fun dayShiftLabel(dayShift: Int): String? = when {
        dayShift < 0 -> "yesterday"
        dayShift > 0 -> "tomorrow"
        else -> null
    }

    /**
     * Spoken form of a whole reading for accessibility, e.g. `14:05, 3 hours behind, yesterday`.
     * Uses full words because screen readers mangle `+3h`.
     */
    fun spokenOffset(offsetMinutes: Int): String {
        if (offsetMinutes == 0) return "same time as home"
        val hours = abs(offsetMinutes) / 60
        val minutes = abs(offsetMinutes) % 60
        val parts = buildList {
            if (hours > 0) add(plural(hours, "hour", "hours"))
            if (minutes > 0) add(plural(minutes, "minute", "minutes"))
        }
        val direction = if (offsetMinutes > 0) "ahead" else "behind"
        return "${parts.joinToString(" ")} $direction"
    }

    private fun plural(count: Int, one: String, many: String): String =
        "$count ${if (count == 1) one else many}"
}
