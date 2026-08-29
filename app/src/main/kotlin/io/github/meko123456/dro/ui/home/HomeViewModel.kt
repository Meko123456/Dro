package io.github.meko123456.dro.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.meko123456.dro.data.SettingsRepository
import io.github.meko123456.dro.domain.Settings
import io.github.meko123456.dro.domain.WorkingHours
import io.github.meko123456.dro.domain.ZoneCatalog
import io.github.meko123456.dro.domain.ZoneClock
import io.github.meko123456.dro.domain.ZoneEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

/** One clock on screen: the zone, its catalogue entry and its reading relative to home. */
data class ClockRow(
    val zone: ZoneId,
    val entry: ZoneEntry,
    val reading: ZoneClock.Reading,
    val hours: WorkingHours,
)

data class HomeUiState(
    val now: Instant,
    val home: ClockRow,
    val cities: List<ClockRow>,
) {
    companion object {
        fun from(settings: Settings, now: Instant): HomeUiState {
            fun row(zone: ZoneId) = ClockRow(
                zone = zone,
                entry = ZoneCatalog.entryFor(zone),
                reading = ZoneClock.read(now, settings.home, zone),
                hours = settings.hoursFor(zone),
            )
            return HomeUiState(now = now, home = row(settings.home), cities = settings.cities.map(::row))
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

    val uiState: StateFlow<HomeUiState?> =
        combine(repository.settings, ticker) { settings, now -> HomeUiState.from(settings, now) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

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
