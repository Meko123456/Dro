package io.github.meko123456.dro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.meko123456.dro.domain.Settings
import io.github.meko123456.dro.domain.SettingsCodec
import io.github.meko123456.dro.domain.WorkingHours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dro")

/**
 * Preferences-backed store for [Settings]. Reads never fail: a missing or unparsable value
 * falls back to the seed (home = device zone, two example cities) so the screen is never empty.
 */
class SettingsRepository(
    private val store: DataStore<Preferences>,
    private val deviceZone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    constructor(context: Context) : this(context.dataStore)

    val settings: Flow<Settings> = store.data.map { prefs -> decode(prefs) }

    suspend fun update(transform: (Settings) -> Settings) {
        store.edit { prefs ->
            val next = transform(decode(prefs))
            prefs[HOME] = next.home.id
            prefs[CITIES] = SettingsCodec.encodeCities(next.cities)
            prefs[HOURS] = SettingsCodec.encodeHours(next.hours)
        }
    }

    suspend fun setHome(zone: ZoneId) = update { it.withHome(zone) }
    suspend fun addCity(zone: ZoneId) = update { it.withCity(zone) }
    suspend fun removeCity(zone: ZoneId) = update { it.withoutCity(zone) }
    suspend fun moveCity(from: Int, to: Int) = update { it.moved(from, to) }
    suspend fun setHours(zone: ZoneId, hours: WorkingHours) = update { it.withHours(zone, hours) }

    private fun decode(prefs: Preferences): Settings {
        val home = SettingsCodec.zoneOrNull(prefs[HOME])
        if (home == null) return seed(deviceZone())
        return Settings(
            home = home,
            cities = SettingsCodec.decodeCities(prefs[CITIES]).filter { it != home },
            hours = SettingsCodec.decodeHours(prefs[HOURS]),
        )
    }

    companion object {
        private val HOME = stringPreferencesKey("home")
        private val CITIES = stringPreferencesKey("cities")
        private val HOURS = stringPreferencesKey("hours")

        /** First-launch settings: the device's zone plus two well-known cities it isn't already. */
        fun seed(deviceZone: ZoneId): Settings {
            val examples = listOf(ZoneId.of("Europe/London"), ZoneId.of("America/New_York"), ZoneId.of("Asia/Tokyo"))
            return Settings(home = deviceZone, cities = examples.filter { it != deviceZone }.take(2))
        }
    }
}
