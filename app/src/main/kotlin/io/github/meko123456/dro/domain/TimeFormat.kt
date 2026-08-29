package io.github.meko123456.dro.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Clock and date text. Honours the device's 12/24-hour preference; English-only otherwise. */
object TimeFormat {

    private val twentyFour = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    private val twelve = DateTimeFormatter.ofPattern("h:mm", Locale.ROOT)
    private val date = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

    /** `14:05` or `2:05` (the am/pm marker is separate, see [amPm], so it can be styled small). */
    fun time(value: LocalTime, twentyFourHour: Boolean): String =
        (if (twentyFourHour) twentyFour else twelve).format(value)

    /** `am`/`pm` in 12-hour mode, null in 24-hour mode. */
    fun amPm(value: LocalTime, twentyFourHour: Boolean): String? =
        if (twentyFourHour) null else if (value.hour < 12) "am" else "pm"

    /** `Sat, 29 Aug` */
    fun date(value: LocalDate): String = date.format(value)

    /** Spoken time for accessibility: always the 12/24 text plus marker, e.g. `2:05 pm`. */
    fun spoken(value: LocalTime, twentyFourHour: Boolean): String =
        listOfNotNull(time(value, twentyFourHour), amPm(value, twentyFourHour)).joinToString(" ")
}
