package io.github.meko123456.dro.domain

import java.text.Normalizer
import java.time.ZoneId

/** One pickable time zone: the tz ID plus a human city/region split. */
data class ZoneEntry(val id: ZoneId, val city: String, val region: String) {
    val label: String get() = "$city, $region"
}

/**
 * Turns the platform tz database into a searchable city list.
 *
 * Only `Region/City` IDs from the geographic regions are kept: `Etc/GMT+3`, `SystemV/EST5`,
 * bare abbreviations (`CET`, `EST5EDT`) and the legacy country aliases (`US/Pacific`,
 * `Canada/Eastern`) are not places a person picks. A short deny-list drops the deprecated
 * spellings that still live inside real regions (`Asia/Calcutta` → `Asia/Kolkata`,
 * `Europe/Kiev` → `Europe/Kyiv`) so a search never shows the same city twice.
 */
object ZoneCatalog {

    private val regions = setOf(
        "Africa", "America", "Antarctica", "Arctic", "Asia", "Atlantic",
        "Australia", "Europe", "Indian", "Pacific",
    )

    /** Deprecated IDs whose canonical replacement is also in the database. */
    private val deprecated = setOf(
        "Africa/Asmera", "Africa/Timbuktu",
        "America/Atka", "America/Buenos_Aires", "America/Catamarca", "America/Coral_Harbour",
        "America/Cordoba", "America/Ensenada", "America/Fort_Wayne", "America/Godthab",
        "America/Indianapolis", "America/Jujuy", "America/Knox_IN", "America/Louisville",
        "America/Mendoza", "America/Montreal", "America/Porto_Acre", "America/Rosario",
        "America/Santa_Isabel", "America/Shiprock", "America/Virgin",
        "Antarctica/South_Pole",
        "Asia/Ashkhabad", "Asia/Calcutta", "Asia/Chongqing", "Asia/Chungking", "Asia/Dacca",
        "Asia/Harbin", "Asia/Istanbul", "Asia/Kashgar", "Asia/Katmandu", "Asia/Macao",
        "Asia/Rangoon", "Asia/Saigon", "Asia/Tel_Aviv", "Asia/Thimbu", "Asia/Ujung_Pandang",
        "Asia/Ulan_Bator",
        "Atlantic/Faeroe", "Atlantic/Jan_Mayen",
        "Australia/ACT", "Australia/Canberra", "Australia/LHI", "Australia/NSW", "Australia/North",
        "Australia/Queensland", "Australia/South", "Australia/Tasmania", "Australia/Victoria",
        "Australia/West", "Australia/Yancowinna",
        "Europe/Belfast", "Europe/Kiev", "Europe/Nicosia", "Europe/Tiraspol", "Europe/Uzhgorod",
        "Europe/Zaporozhye",
        "Pacific/Enderbury", "Pacific/Johnston", "Pacific/Ponape", "Pacific/Samoa", "Pacific/Truk",
        "Pacific/Yap",
    )

    /** Every pickable zone, sorted by city name. */
    fun entries(ids: Set<String> = ZoneId.getAvailableZoneIds()): List<ZoneEntry> =
        ids.filter(::isPickable).map { entryFor(ZoneId.of(it)) }.sortedBy { fold(it.city) }

    fun isPickable(id: String): Boolean {
        if (id in deprecated) return false
        val slash = id.indexOf('/')
        return slash > 0 && id.substring(0, slash) in regions
    }

    /** `America/Argentina/Buenos_Aires` → city "Buenos Aires", region "Argentina, America". */
    fun entryFor(id: ZoneId): ZoneEntry {
        val parts = id.id.split('/')
        val city = parts.last().replace('_', ' ')
        val region = parts.dropLast(1).asReversed().joinToString(", ") { it.replace('_', ' ') }
        return ZoneEntry(id, city, region.ifEmpty { id.id })
    }

    /**
     * Case- and accent-insensitive search. Cities whose name starts with [query] come first,
     * then cities/regions containing it; each group keeps the catalogue's city order. A blank
     * query returns everything.
     */
    fun search(query: String, entries: List<ZoneEntry>): List<ZoneEntry> {
        val q = fold(query.trim())
        if (q.isEmpty()) return entries
        val prefix = ArrayList<ZoneEntry>()
        val contains = ArrayList<ZoneEntry>()
        for (entry in entries) {
            val city = fold(entry.city)
            when {
                city.startsWith(q) -> prefix += entry
                city.contains(q) || fold(entry.region).contains(q) -> contains += entry
            }
        }
        return prefix + contains
    }

    /** Lower-case, accents stripped: "São Paulo" → "sao paulo". */
    fun fold(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
}
