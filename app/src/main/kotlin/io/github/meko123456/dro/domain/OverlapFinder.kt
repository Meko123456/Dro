package io.github.meko123456.dro.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

/**
 * Half-open interval `[startMinute, endMinute)` on the home-day axis: elapsed minutes since
 * the home zone's midnight. The axis is elapsed time, not wall-clock, so it stays linear on a
 * DST day (23 or 25 hours long) — use [OverlapFinder.wallTime] to label a point on it.
 */
data class Segment(val startMinute: Int, val endMinute: Int) {
    init {
        require(endMinute > startMinute) { "empty segment $startMinute..$endMinute" }
    }

    val lengthMinutes: Int get() = endMinute - startMinute
}

/** A zone together with the hours its people are at work, in that zone's local time. */
data class ZoneSchedule(val zone: ZoneId, val hours: WorkingHours = WorkingHours.DEFAULT)

/**
 * Projects every zone's working hours onto one home day and intersects them.
 *
 * "Home day" is the interval between two consecutive local midnights in the home zone, so
 * on a DST transition it is 23 or 25 hours long and [dayLengthMinutes] says which. A zone
 * whose working day straddles the home midnight contributes two segments (Los Angeles
 * seen from Tbilisi works 20:00–05:00), which is why results are lists, not single ranges.
 */
object OverlapFinder {

    /** Instant of local midnight starting [date] in [home]. */
    fun dayStart(date: LocalDate, home: ZoneId): Instant = date.atStartOfDay(home).toInstant()

    /** Length of [date] in [home] in minutes: 1440 normally, 1380 or 1500 across a DST change. */
    fun dayLengthMinutes(date: LocalDate, home: ZoneId): Int =
        Duration.between(dayStart(date, home), dayStart(date.plusDays(1), home)).toMinutes().toInt()

    /** Wall-clock time in [home] at [minute] on the axis of [date]. */
    fun wallTime(date: LocalDate, home: ZoneId, minute: Int): LocalTime =
        dayStart(date, home).plusSeconds(minute * 60L).atZone(home).toLocalTime()

    /** Axis minute of [instant] on the home day [date]; may fall outside 0..dayLength. */
    fun minuteOf(instant: Instant, date: LocalDate, home: ZoneId): Int =
        Duration.between(dayStart(date, home), instant).toMinutes().toInt()

    /**
     * Where [schedule]'s working hours land on the home day [date], as zero, one or two
     * segments clipped to the day.
     */
    fun project(date: LocalDate, home: ZoneId, schedule: ZoneSchedule): List<Segment> {
        val dayStart = dayStart(date, home)
        val dayEnd = dayStart(date.plusDays(1), home)
        val dayLength = Duration.between(dayStart, dayEnd).toMinutes().toInt()

        // The zone's local dates that can touch this home day: yesterday, today, tomorrow.
        val zoneToday = dayStart.atZone(schedule.zone).toLocalDate()
        val result = ArrayList<Segment>(2)
        for (offset in -1L..1L) {
            val localDate = zoneToday.plusDays(offset)
            val start = localDate.atTime(schedule.hours.start).atZoneLenient(schedule.zone)
            val endDate = if (schedule.hours.crossesMidnight) localDate.plusDays(1) else localDate
            val end = endDate.atTime(schedule.hours.end).atZoneLenient(schedule.zone)
            val from = max(Duration.between(dayStart, start.toInstant()).toMinutes().toInt(), 0)
            val to = min(Duration.between(dayStart, end.toInstant()).toMinutes().toInt(), dayLength)
            if (to > from) result += Segment(from, to)
        }
        return merge(result)
    }

    /**
     * Minutes of the home day when every schedule is at work. Schedules with no working hours
     * on that day make the result empty. An empty [schedules] list is "everyone", i.e. the
     * whole day.
     */
    fun sharedWindows(date: LocalDate, home: ZoneId, schedules: List<ZoneSchedule>): List<Segment> {
        var shared: List<Segment> = listOf(Segment(0, dayLengthMinutes(date, home)))
        for (schedule in schedules) {
            shared = intersect(shared, project(date, home, schedule))
            if (shared.isEmpty()) break
        }
        return shared
    }

    fun totalMinutes(segments: List<Segment>): Int = segments.sumOf { it.lengthMinutes }

    /** `09:00–16:30` per segment joined with ", ", or null when there is nothing shared. */
    fun label(date: LocalDate, home: ZoneId, segments: List<Segment>): String? {
        if (segments.isEmpty()) return null
        return segments.joinToString(", ") {
            "${wallTime(date, home, it.startMinute)}–${wallTime(date, home, it.endMinute)}"
        }
    }

    internal fun intersect(a: List<Segment>, b: List<Segment>): List<Segment> {
        val out = ArrayList<Segment>()
        for (x in a) for (y in b) {
            val from = max(x.startMinute, y.startMinute)
            val to = min(x.endMinute, y.endMinute)
            if (to > from) out += Segment(from, to)
        }
        return merge(out)
    }

    /** Sorts and joins touching or overlapping segments. */
    internal fun merge(segments: List<Segment>): List<Segment> {
        if (segments.size < 2) return segments
        val sorted = segments.sortedBy { it.startMinute }
        val out = ArrayList<Segment>()
        var current = sorted.first()
        for (next in sorted.drop(1)) {
            current = if (next.startMinute <= current.endMinute) {
                Segment(current.startMinute, max(current.endMinute, next.endMinute))
            } else {
                out += current
                next
            }
        }
        out += current
        return out
    }

    /**
     * Like [java.time.LocalDateTime.atZone] but explicit about the two DST corner cases:
     * a time inside a spring-forward gap is shifted later by the gap; a time inside a
     * fall-back overlap takes the earlier offset. Both are java.time's defaults — named here
     * so the choice is visible.
     */
    private fun java.time.LocalDateTime.atZoneLenient(zone: ZoneId): ZonedDateTime = atZone(zone)
}
