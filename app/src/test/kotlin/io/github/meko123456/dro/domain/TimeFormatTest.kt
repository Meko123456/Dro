package io.github.meko123456.dro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TimeFormatTest {

    @Test
    fun `twenty four hour clock pads the hour and has no marker`() {
        assertEquals("14:05", TimeFormat.time(LocalTime.of(14, 5), twentyFourHour = true))
        assertEquals("00:00", TimeFormat.time(LocalTime.MIDNIGHT, twentyFourHour = true))
        assertNull(TimeFormat.amPm(LocalTime.of(14, 5), twentyFourHour = true))
    }

    @Test
    fun `twelve hour clock uses a marker and midnight is 12 am`() {
        assertEquals("2:05", TimeFormat.time(LocalTime.of(14, 5), twentyFourHour = false))
        assertEquals("pm", TimeFormat.amPm(LocalTime.of(14, 5), twentyFourHour = false))
        assertEquals("12:00", TimeFormat.time(LocalTime.MIDNIGHT, twentyFourHour = false))
        assertEquals("am", TimeFormat.amPm(LocalTime.MIDNIGHT, twentyFourHour = false))
        assertEquals("12:30", TimeFormat.time(LocalTime.of(12, 30), twentyFourHour = false))
        assertEquals("pm", TimeFormat.amPm(LocalTime.of(12, 30), twentyFourHour = false))
    }

    @Test
    fun `compact form drops zero minutes in twelve hour mode only`() {
        assertEquals("9am", TimeFormat.compact(LocalTime.of(9, 0), twentyFourHour = false))
        assertEquals("6pm", TimeFormat.compact(LocalTime.of(18, 0), twentyFourHour = false))
        assertEquals("9:30am", TimeFormat.compact(LocalTime.of(9, 30), twentyFourHour = false))
        assertEquals("12am", TimeFormat.compact(LocalTime.MIDNIGHT, twentyFourHour = false))
        assertEquals("12pm", TimeFormat.compact(LocalTime.NOON, twentyFourHour = false))
        assertEquals("09:00", TimeFormat.compact(LocalTime.of(9, 0), twentyFourHour = true))
        assertEquals("18:30", TimeFormat.compact(LocalTime.of(18, 30), twentyFourHour = true))
    }

    @Test
    fun `date and spoken forms`() {
        assertEquals("Sat, 29 Aug", TimeFormat.date(LocalDate.of(2026, 8, 29)))
        assertEquals("2:05 pm", TimeFormat.spoken(LocalTime.of(14, 5), twentyFourHour = false))
        assertEquals("14:05", TimeFormat.spoken(LocalTime.of(14, 5), twentyFourHour = true))
    }
}
