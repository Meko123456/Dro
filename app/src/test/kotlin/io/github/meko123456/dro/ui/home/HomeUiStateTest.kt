package io.github.meko123456.dro.ui.home

import io.github.meko123456.dro.domain.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class HomeUiStateTest {

    private val tbilisi = ZoneId.of("Asia/Tbilisi")
    private val london = ZoneId.of("Europe/London")
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val settings = Settings(home = tbilisi, cities = listOf(london, tokyo))

    /** 14:00 Saturday in Tbilisi. */
    private val now = Instant.parse("2026-08-29T10:00:00Z")

    @Test
    fun `without a preview the rows read now`() {
        val ui = HomeUiState.from(settings, now)
        assertFalse(ui.previewing)
        assertNull(ui.previewMinute)
        assertEquals(LocalTime.of(14, 0), ui.home.reading.localTime)
        assertEquals(LocalTime.of(11, 0), ui.cities[0].reading.localTime)
        assertEquals(LocalDate.of(2026, 8, 29), ui.date)
        assertEquals(840, ui.nowMinute)
        assertEquals(1440, ui.dayLengthMinutes)
    }

    @Test
    fun `a preview re-reads every row at the scrubbed home minute but keeps now`() {
        val ui = HomeUiState.from(settings, now, previewMinute = 22 * 60)
        assertTrue(ui.previewing)
        assertEquals(LocalTime.of(22, 0), ui.home.reading.localTime)
        assertEquals(LocalTime.of(19, 0), ui.cities[0].reading.localTime)
        // Tokyo at 22:00 Tbilisi is 03:00 the next day.
        assertEquals(LocalTime.of(3, 0), ui.cities[1].reading.localTime)
        assertEquals(1, ui.cities[1].reading.dayShift)
        assertEquals(now, ui.now)
        assertEquals(840, ui.nowMinute)
    }

    @Test
    fun `preview minute is clamped to the home day`() {
        assertEquals(1439, HomeUiState.from(settings, now, previewMinute = 5000).previewMinute)
        assertEquals(0, HomeUiState.from(settings, now, previewMinute = -3).previewMinute)
    }

    @Test
    fun `bars follow home first then cities and shared uses everyone`() {
        val ui = HomeUiState.from(settings, now)
        assertEquals(listOf(tbilisi, london, tokyo), ui.bars.map { it.row.zone })
        // Tbilisi 9–18, London 12–21, Tokyo 4–13 → shared 12:00–13:00.
        assertEquals(1, ui.shared.size)
        assertEquals(720, ui.shared[0].startMinute)
        assertEquals(780, ui.shared[0].endMinute)
    }
}
