package io.github.meko123456.dro.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.meko123456.dro.data.SettingsRepository
import io.github.meko123456.dro.domain.OverlapFinder
import io.github.meko123456.dro.domain.Segment
import io.github.meko123456.dro.domain.Settings
import io.github.meko123456.dro.domain.WorkingHours
import io.github.meko123456.dro.domain.ZoneCatalog
import io.github.meko123456.dro.domain.ZoneClock
import io.github.meko123456.dro.domain.ZoneEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One clock on screen: the zone, its catalogue entry and its reading relative to home. */
data class ClockRow(
    val zone: ZoneId,
    val entry: ZoneEntry,
    val reading: ZoneClock.Reading,
    val hours: WorkingHours,
)

/**
 * @property date the home zone's calendar date at [now]; the overlap strip's axis is this day
 * @property dayLengthMinutes 1440, or 1380/1500 across a DST change in the home zone
 * @property nowMinute elapsed minutes since home midnight
 * @property bars home first, then the cities, each with its working hours on the home axis
 * @property shared minutes of the day when every zone is at work
 * @property previewMinute axis minute the user is scrubbing to, or null when showing now; the
 *   clock rows read at this moment when set
 */
data class HomeUiState(
    val now: Instant,
    val home: ClockRow,
    val cities: List<ClockRow>,
    val date: LocalDate,
    val dayLengthMinutes: Int,
    val nowMinute: Int,
    val bars: List<OverlapBar>,
    val shared: List<Segment>,
    val previewMinute: Int? = null,
) {
    val previewing: Boolean get() = previewMinute != null

    companion object {
        fun from(settings: Settings, now: Instant, previewMinute: Int? = null): HomeUiState {
            val date = now.atZone(settings.home).toLocalDate()
            val dayLength = OverlapFinder.dayLengthMinutes(date, settings.home)
            val preview = previewMinute?.coerceIn(0, dayLength - 1)
            val shown = if (preview != null) OverlapFinder.dayStart(date, settings.home).plusSeconds(preview * 60L) else now
            fun row(zone: ZoneId) = ClockRow(
                zone = zone,
                entry = ZoneCatalog.entryFor(zone),
                reading = ZoneClock.read(shown, settings.home, zone),
                hours = settings.hoursFor(zone),
            )
            val home = row(settings.home)
            val cities = settings.cities.map(::row)
            val schedules = settings.schedules
            return HomeUiState(
                now = now,
                home = home,
                cities = cities,
                date = date,
                dayLengthMinutes = dayLength,
                nowMinute = OverlapFinder.minuteOf(now, date, settings.home),
                bars = (listOf(home) + cities).zip(schedules) { r, s -> OverlapBar(r, OverlapFinder.project(date, settings.home, s)) },
                shared = OverlapFinder.sharedWindows(date, settings.home, schedules),
                previewMinute = preview,
            )
        }
    }
}

class HomeViewModel(private val repository: SettingsRepository) : ViewModel() {

    /**
     * Emits immediately, then on every wall-clock minute boundary — the delay is "until the
     * next :00", not a fixed 60 s, so the displayed minute never lags the status bar. Cold: it
     * only runs while [uiState] has a subscriber.
     */
    private val ticker: Flow<Instant> = flow {
        while (true) {
            emit(Instant.now())
            val millis = System.currentTimeMillis()
            delay(MINUTE_MS - millis % MINUTE_MS + SLACK_MS)
        }
    }

    /** Axis minute being scrubbed to; survives rotation with the ViewModel, cleared on reset. */
    private val previewMinute = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<HomeUiState?> =
        combine(repository.settings, ticker, previewMinute) { settings, now, preview ->
            HomeUiState.from(settings, now, preview)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun preview(minute: Int?) {
        previewMinute.value = minute
    }

    fun addCity(zone: ZoneId) = viewModelScope.launch { repository.addCity(zone) }
    fun removeCity(zone: ZoneId) = viewModelScope.launch { repository.removeCity(zone) }
    fun makeHome(zone: ZoneId) = viewModelScope.launch { repository.setHome(zone) }
    fun moveCity(from: Int, to: Int) = viewModelScope.launch { repository.moveCity(from, to) }
    fun setHours(zone: ZoneId, hours: WorkingHours) = viewModelScope.launch { repository.setHours(zone, hours) }

    private companion object {
        const val MINUTE_MS = 60_000L
        /** Fire just after the boundary so the new minute is definitely readable. */
        const val SLACK_MS = 20L
        /** Survive a rotation without restarting the ticker. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
