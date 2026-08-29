package io.github.meko123456.dro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ZoneClockTest {

    private val tbilisi = ZoneId.of("Asia/Tbilisi")
    private val dubai = ZoneId.of("Asia/Dubai")
    private val london = ZoneId.of("Europe/London")
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val kathmandu = ZoneId.of("Asia/Kathmandu")
    private val losAngeles = ZoneId.of("America/Los_Angeles")
    private val auckland = ZoneId.of("Pacific/Auckland")

    /** A Saturday afternoon in Tbilisi (14:00 local). */
    private val summerAfternoon = Instant.parse("2026-08-29T10:00:00Z")

    @Test
    fun `Dubai and Tbilisi share an offset all year`() {
        val summer = ZoneClock.read(summerAfternoon, tbilisi, dubai)
        val winter = ZoneClock.read(Instant.parse("2026-01-15T10:00:00Z"), tbilisi, dubai)
        assertEquals(0, summer.offsetMinutes)
        assertEquals(0, winter.offsetMinutes)
        assertEquals(LocalTime.of(14, 0), summer.localTime)
        assertEquals(0, summer.dayShift)
    }

    @Test
    fun `London is three hours behind Tbilisi in summer and four in winter`() {
        assertEquals(-180, ZoneClock.read(summerAfternoon, tbilisi, london).offsetMinutes)
        assertEquals(-240, ZoneClock.read(Instant.parse("2026-01-15T10:00:00Z"), tbilisi, london).offsetMinutes)
    }

    @Test
    fun `offset is computed for the exact instant across a DST transition`() {
        // Europe/London springs forward at 01:00 UTC on 2026-03-29.
        val before = Instant.parse("2026-03-29T00:30:00Z")
        val after = Instant.parse("2026-03-29T01:30:00Z")
        assertEquals(-240, ZoneClock.read(before, tbilisi, london).offsetMinutes)
        assertEquals(-180, ZoneClock.read(after, tbilisi, london).offsetMinutes)
        assertEquals(LocalTime.of(0, 30), ZoneClock.read(before, tbilisi, london).localTime)
        assertEquals(LocalTime.of(2, 30), ZoneClock.read(after, tbilisi, london).localTime)
    }

    @Test
    fun `half hour and quarter hour zones keep their minutes`() {
        assertEquals(90, ZoneClock.read(summerAfternoon, tbilisi, kolkata).offsetMinutes)
        assertEquals(105, ZoneClock.read(summerAfternoon, tbilisi, kathmandu).offsetMinutes)
        assertEquals(LocalTime.of(15, 30), ZoneClock.read(summerAfternoon, tbilisi, kolkata).localTime)
        assertEquals(LocalTime.of(15, 45), ZoneClock.read(summerAfternoon, tbilisi, kathmandu).localTime)
    }

    @Test
    fun `a city still on the previous date reads as yesterday`() {
        // 00:30 on 30 Aug in Tbilisi; 13:30 on 29 Aug in Los Angeles.
        val justAfterMidnight = Instant.parse("2026-08-29T20:30:00Z")
        val reading = ZoneClock.read(justAfterMidnight, tbilisi, losAngeles)
        assertEquals(-1, reading.dayShift)
        assertEquals(LocalDate.of(2026, 8, 29), reading.localDate)
        assertEquals(LocalTime.of(13, 30), reading.localTime)
        assertEquals(-660, reading.offsetMinutes)
    }

    @Test
    fun `a city already on the next date reads as tomorrow`() {
        // 22:00 on 29 Aug in Tbilisi; 06:00 on 30 Aug in Auckland (NZST, UTC+12).
        val lateEvening = Instant.parse("2026-08-29T18:00:00Z")
        val reading = ZoneClock.read(lateEvening, tbilisi, auckland)
        assertEquals(1, reading.dayShift)
        assertEquals(LocalDate.of(2026, 8, 30), reading.localDate)
        assertEquals(LocalTime.of(6, 0), reading.localTime)
        assertEquals(480, reading.offsetMinutes)
    }

    @Test
    fun `reading a zone against itself is the identity`() {
        val reading = ZoneClock.read(summerAfternoon, london, london)
        assertEquals(0, reading.offsetMinutes)
        assertEquals(0, reading.dayShift)
        assertEquals(LocalTime.of(11, 0), reading.localTime)
    }

    @Test
    fun `offset labels`() {
        assertEquals("same time", ZoneClock.offsetLabel(0))
        assertEquals("+3h", ZoneClock.offsetLabel(180))
        assertEquals("-4h", ZoneClock.offsetLabel(-240))
        assertEquals("+1h 30m", ZoneClock.offsetLabel(90))
        assertEquals("-1h 30m", ZoneClock.offsetLabel(-90))
        assertEquals("+45m", ZoneClock.offsetLabel(45))
        assertEquals("-11h", ZoneClock.offsetLabel(-660))
    }

    @Test
    fun `day shift labels`() {
        assertNull(ZoneClock.dayShiftLabel(0))
        assertEquals("yesterday", ZoneClock.dayShiftLabel(-1))
        assertEquals("tomorrow", ZoneClock.dayShiftLabel(1))
    }

    @Test
    fun `spoken offsets use whole words and singular forms`() {
        assertEquals("same time as home", ZoneClock.spokenOffset(0))
        assertEquals("1 hour ahead", ZoneClock.spokenOffset(60))
        assertEquals("3 hours behind", ZoneClock.spokenOffset(-180))
        assertEquals("1 hour 30 minutes ahead", ZoneClock.spokenOffset(90))
        assertEquals("45 minutes ahead", ZoneClock.spokenOffset(45))
        assertEquals("1 hour 1 minute behind", ZoneClock.spokenOffset(-61))
    }
}
