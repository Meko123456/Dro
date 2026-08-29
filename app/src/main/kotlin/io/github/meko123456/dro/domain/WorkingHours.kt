package io.github.meko123456.dro.domain

import java.time.LocalTime

/**
 * A zone's working day in its own local time. [end] at or before [start] means the shift
 * crosses midnight (22:00–06:00). The interval is half-open: 18:00 is already off work.
 */
data class WorkingHours(val start: LocalTime, val end: LocalTime) {

    val crossesMidnight: Boolean get() = !end.isAfter(start)

    /** Length of the working day in minutes; 1440 for a start == end "always on" pair. */
    val durationMinutes: Int
        get() {
            val startMin = start.toSecondOfDay() / 60
            val endMin = end.toSecondOfDay() / 60
            return if (crossesMidnight) MINUTES_PER_DAY - startMin + endMin else endMin - startMin
        }

    companion object {
        const val MINUTES_PER_DAY: Int = 24 * 60
        val DEFAULT: WorkingHours = WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0))
    }
}
