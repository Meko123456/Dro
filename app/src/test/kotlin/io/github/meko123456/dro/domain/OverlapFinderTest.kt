package io.github.meko123456.dro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class OverlapFinderTest {

    private val tbilisi = ZoneId.of("Asia/Tbilisi")
    private val dubai = ZoneId.of("Asia/Dubai")
    private val london = ZoneId.of("Europe/London")
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val losAngeles = ZoneId.of("America/Los_Angeles")
    private val honolulu = ZoneId.of("Pacific/Honolulu")

    private val saturday = LocalDate.of(2026, 8, 29)
    private val nineToSix = WorkingHours.DEFAULT

    private fun seg(from: String, to: String) =
        Segment(LocalTime.parse(from).toSecondOfDay() / 60, LocalTime.parse(to).toSecondOfDay() / 60)

    @Test
    fun `working hours know their length and whether they cross midnight`() {
        assertEquals(540, nineToSix.durationMinutes)
        assertTrue(!nineToSix.crossesMidnight)
        val night = WorkingHours(LocalTime.of(22, 0), LocalTime.of(6, 0))
        assertTrue(night.crossesMidnight)
        assertEquals(480, night.durationMinutes)
    }

    @Test
    fun `a zone with the same offset projects onto the same minutes`() {
        assertEquals(listOf(seg("09:00", "18:00")), OverlapFinder.project(saturday, tbilisi, ZoneSchedule(dubai)))
    }

    @Test
    fun `London working day lands three hours later on the Tbilisi axis in summer`() {
        assertEquals(listOf(seg("12:00", "21:00")), OverlapFinder.project(saturday, tbilisi, ZoneSchedule(london)))
        val winter = LocalDate.of(2026, 1, 15)
        assertEquals(listOf(seg("13:00", "22:00")), OverlapFinder.project(winter, tbilisi, ZoneSchedule(london)))
    }

    @Test
    fun `half hour zones keep half hour boundaries`() {
        assertEquals(listOf(seg("07:30", "16:30")), OverlapFinder.project(saturday, tbilisi, ZoneSchedule(kolkata)))
    }

    @Test
    fun `a working day that straddles home midnight becomes two segments`() {
        // 09:00–18:00 in Los Angeles is 20:00–05:00 in Tbilisi.
        assertEquals(
            listOf(seg("00:00", "05:00"), Segment(1200, 1440)),
            OverlapFinder.project(saturday, tbilisi, ZoneSchedule(losAngeles)),
        )
    }

    @Test
    fun `a night shift in the home zone itself wraps too`() {
        val night = WorkingHours(LocalTime.of(22, 0), LocalTime.of(6, 0))
        assertEquals(
            listOf(Segment(0, 360), Segment(1320, 1440)),
            OverlapFinder.project(saturday, tbilisi, ZoneSchedule(tbilisi, night)),
        )
    }

    @Test
    fun `shared window of home and London is noon to six`() {
        val shared = OverlapFinder.sharedWindows(saturday, tbilisi, listOf(ZoneSchedule(tbilisi), ZoneSchedule(london)))
        assertEquals(listOf(seg("12:00", "18:00")), shared)
        assertEquals(360, OverlapFinder.totalMinutes(shared))
        assertEquals("12:00–18:00", OverlapFinder.label(saturday, tbilisi, shared))
    }

    @Test
    fun `shared window with Kolkata ends on the half hour`() {
        val shared = OverlapFinder.sharedWindows(saturday, tbilisi, listOf(ZoneSchedule(tbilisi), ZoneSchedule(kolkata)))
        assertEquals("09:00–16:30", OverlapFinder.label(saturday, tbilisi, shared))
    }

    @Test
    fun `Tokyo and Los Angeles only share the small hours`() {
        val shared = OverlapFinder.sharedWindows(saturday, tbilisi, listOf(ZoneSchedule(tokyo), ZoneSchedule(losAngeles)))
        assertEquals("04:00–05:00", OverlapFinder.label(saturday, tbilisi, shared))
    }

    @Test
    fun `no shared hours gives an empty list and a null label`() {
        val shared = OverlapFinder.sharedWindows(saturday, tbilisi, listOf(ZoneSchedule(tbilisi), ZoneSchedule(honolulu)))
        assertTrue(shared.isEmpty())
        assertNull(OverlapFinder.label(saturday, tbilisi, shared))
        assertEquals(0, OverlapFinder.totalMinutes(shared))
    }

    @Test
    fun `no schedules means the whole day is shared`() {
        assertEquals(listOf(Segment(0, 1440)), OverlapFinder.sharedWindows(saturday, tbilisi, emptyList()))
    }

    @Test
    fun `a DST day in the home zone is 23 hours long and wall times skip the gap`() {
        // Europe/London springs forward at 01:00 GMT on 2026-03-29.
        val springForward = LocalDate.of(2026, 3, 29)
        assertEquals(1380, OverlapFinder.dayLengthMinutes(springForward, london))
        assertEquals(1440, OverlapFinder.dayLengthMinutes(saturday, london))
        assertEquals(LocalTime.of(0, 30), OverlapFinder.wallTime(springForward, london, 30))
        // 60 elapsed minutes after midnight the wall clock already says 02:00.
        assertEquals(LocalTime.of(2, 0), OverlapFinder.wallTime(springForward, london, 60))
        // Tbilisi 09:00 is 05:00Z is 06:00 BST, 300 elapsed minutes into London's day.
        assertEquals(listOf(Segment(300, 840)), OverlapFinder.project(springForward, london, ZoneSchedule(tbilisi)))
        assertEquals(LocalTime.of(6, 0), OverlapFinder.wallTime(springForward, london, 300))
    }

    @Test
    fun `fall back day in the home zone is 25 hours long`() {
        // Europe/London falls back at 01:00 UTC on 2026-10-25.
        assertEquals(1500, OverlapFinder.dayLengthMinutes(LocalDate.of(2026, 10, 25), london))
    }

    @Test
    fun `minuteOf maps an instant onto the home axis`() {
        // 14:00 Tbilisi on the Saturday.
        val instant = Instant.parse("2026-08-29T10:00:00Z")
        assertEquals(840, OverlapFinder.minuteOf(instant, saturday, tbilisi))
        assertEquals(-600, OverlapFinder.minuteOf(instant, saturday.plusDays(1), tbilisi))
    }

    @Test
    fun `merge joins touching and overlapping segments`() {
        assertEquals(
            listOf(Segment(0, 300), Segment(600, 900)),
            OverlapFinder.merge(listOf(Segment(600, 800), Segment(0, 100), Segment(100, 300), Segment(700, 900))),
        )
    }

    @Test
    fun `intersect handles multi segment inputs`() {
        val a = listOf(Segment(0, 300), Segment(1200, 1440))
        val b = listOf(Segment(240, 780))
        assertEquals(listOf(Segment(240, 300)), OverlapFinder.intersect(a, b))
        assertTrue(OverlapFinder.intersect(a, listOf(Segment(300, 1200))).isEmpty())
    }
}
