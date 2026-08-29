package io.github.meko123456.dro.domain

import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Everything the user has chosen: the home zone, the ordered list of other cities and any
 * working hours that differ from [WorkingHours.DEFAULT]. [cities] never contains [home].
 */
data class Settings(
    val home: ZoneId,
    val cities: List<ZoneId>,
    val hours: Map<ZoneId, WorkingHours> = emptyMap(),
) {
    fun hoursFor(zone: ZoneId): WorkingHours = hours[zone] ?: WorkingHours.DEFAULT

    /** Home first, then the cities, each with its working hours. */
    val schedules: List<ZoneSchedule>
        get() = (listOf(home) + cities).map { ZoneSchedule(it, hoursFor(it)) }

    fun withHome(newHome: ZoneId): Settings {
        if (newHome == home) return this
        val rest = (listOf(home) + cities).filter { it != newHome }
        return copy(home = newHome, cities = rest)
    }

    fun withCity(zone: ZoneId): Settings =
        if (zone == home || zone in cities) this else copy(cities = cities + zone)

    fun withoutCity(zone: ZoneId): Settings = copy(cities = cities - zone, hours = hours - zone)

    fun moved(from: Int, to: Int): Settings {
        if (from == to || from !in cities.indices || to !in cities.indices) return this
        val list = cities.toMutableList()
        list.add(to, list.removeAt(from))
        return copy(cities = list)
    }

    fun withHours(zone: ZoneId, newHours: WorkingHours): Settings =
        copy(hours = if (newHours == WorkingHours.DEFAULT) hours - zone else hours + (zone to newHours))
}

/**
 * Plain-string encoding for preferences storage — no serialization library for three
 * fields. Unknown or invalid zone ids are dropped rather than failing the whole read, so a
 * tz database that loses a zone can't brick the app.
 */
object SettingsCodec {

    private val time = DateTimeFormatter.ofPattern("HH:mm")

    fun encodeCities(cities: List<ZoneId>): String = cities.joinToString(",") { it.id }

    fun decodeCities(text: String?): List<ZoneId> =
        text.orEmpty().split(',').mapNotNull(::zoneOrNull).distinct()

    /** `Europe/London=08:30-17:00;Asia/Tokyo=10:00-19:00` */
    fun encodeHours(hours: Map<ZoneId, WorkingHours>): String =
        hours.entries.joinToString(";") { (zone, h) -> "${zone.id}=${time.format(h.start)}-${time.format(h.end)}" }

    fun decodeHours(text: String?): Map<ZoneId, WorkingHours> {
        if (text.isNullOrBlank()) return emptyMap()
        val out = LinkedHashMap<ZoneId, WorkingHours>()
        for (item in text.split(';')) {
            val eq = item.indexOf('=')
            val dash = item.indexOf('-', eq + 1)
            if (eq <= 0 || dash <= eq) continue
            val zone = zoneOrNull(item.substring(0, eq)) ?: continue
            val start = timeOrNull(item.substring(eq + 1, dash)) ?: continue
            val end = timeOrNull(item.substring(dash + 1)) ?: continue
            out[zone] = WorkingHours(start, end)
        }
        return out
    }

    fun zoneOrNull(id: String?): ZoneId? {
        if (id.isNullOrBlank()) return null
        return try {
            ZoneId.of(id.trim())
        } catch (_: java.time.DateTimeException) {
            null
        }
    }

    private fun timeOrNull(text: String): LocalTime? = try {
        LocalTime.parse(text.trim(), time)
    } catch (_: java.time.DateTimeException) {
        null
    }
}
