package io.github.meko123456.dro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId

class SettingsTest {

    private val tbilisi = ZoneId.of("Asia/Tbilisi")
    private val london = ZoneId.of("Europe/London")
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val newYork = ZoneId.of("America/New_York")
    private val base = Settings(home = tbilisi, cities = listOf(london, tokyo))
    private val early = WorkingHours(LocalTime.of(8, 30), LocalTime.of(17, 0))

    @Test
    fun `making a city home moves the old home into the list at the front`() {
        val next = base.withHome(london)
        assertEquals(london, next.home)
        assertEquals(listOf(tbilisi, tokyo), next.cities)
        assertSame(base, base.withHome(tbilisi))
    }

    @Test
    fun `adding ignores duplicates and the home zone`() {
        assertEquals(listOf(london, tokyo, newYork), base.withCity(newYork).cities)
        assertSame(base, base.withCity(london))
        assertSame(base, base.withCity(tbilisi))
    }

    @Test
    fun `removing a city also forgets its working hours`() {
        val withHours = base.withHours(london, early)
        assertEquals(early, withHours.hoursFor(london))
        val removed = withHours.withoutCity(london)
        assertEquals(listOf(tokyo), removed.cities)
        assertEquals(WorkingHours.DEFAULT, removed.hoursFor(london))
        assertTrue(removed.hours.isEmpty())
    }

    @Test
    fun `setting default hours drops the override instead of storing it`() {
        val custom = base.withHours(tokyo, early)
        assertEquals(1, custom.hours.size)
        assertTrue(custom.withHours(tokyo, WorkingHours.DEFAULT).hours.isEmpty())
    }

    @Test
    fun `moving reorders within bounds and ignores nonsense`() {
        val three = base.withCity(newYork)
        assertEquals(listOf(tokyo, london, newYork), three.moved(1, 0).cities)
        assertEquals(listOf(london, newYork, tokyo), three.moved(1, 2).cities)
        assertSame(three, three.moved(0, 0))
        assertSame(three, three.moved(0, 7))
        assertSame(three, three.moved(-1, 0))
    }

    @Test
    fun `schedules list home first with its hours`() {
        val schedules = base.withHours(tbilisi, early).schedules
        assertEquals(listOf(tbilisi, london, tokyo), schedules.map { it.zone })
        assertEquals(early, schedules[0].hours)
        assertEquals(WorkingHours.DEFAULT, schedules[1].hours)
    }

    @Test
    fun `cities round-trip through the codec and bad ids are dropped`() {
        val text = SettingsCodec.encodeCities(listOf(london, tokyo))
        assertEquals("Europe/London,Asia/Tokyo", text)
        assertEquals(listOf(london, tokyo), SettingsCodec.decodeCities(text))
        assertEquals(listOf(london), SettingsCodec.decodeCities("Europe/London,Mars/Olympus,,Europe/London"))
        assertTrue(SettingsCodec.decodeCities(null).isEmpty())
        assertTrue(SettingsCodec.decodeCities("").isEmpty())
    }

    @Test
    fun `hours round-trip through the codec`() {
        val hours = mapOf(london to early, tokyo to WorkingHours(LocalTime.of(22, 0), LocalTime.of(6, 0)))
        val text = SettingsCodec.encodeHours(hours)
        assertEquals("Europe/London=08:30-17:00;Asia/Tokyo=22:00-06:00", text)
        assertEquals(hours, SettingsCodec.decodeHours(text))
    }

    @Test
    fun `malformed hour entries are skipped individually`() {
        val decoded = SettingsCodec.decodeHours("Europe/London=08:30-17:00;garbage;Asia/Tokyo=25:00-06:00;Mars/X=09:00-10:00")
        assertEquals(mapOf(london to early), decoded)
        assertTrue(SettingsCodec.decodeHours(null).isEmpty())
        assertTrue(SettingsCodec.decodeHours("  ").isEmpty())
    }

    @Test
    fun `zone parsing tolerates whitespace and rejects junk`() {
        assertEquals(london, SettingsCodec.zoneOrNull(" Europe/London "))
        assertNull(SettingsCodec.zoneOrNull("Nowhere/Land"))
        assertNull(SettingsCodec.zoneOrNull(null))
        assertNull(SettingsCodec.zoneOrNull(""))
    }
}
