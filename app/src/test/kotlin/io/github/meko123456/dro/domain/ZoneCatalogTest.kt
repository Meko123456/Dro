package io.github.meko123456.dro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ZoneCatalogTest {

    private val all = ZoneCatalog.entries()

    @Test
    fun `only geographic Region slash City ids are pickable`() {
        assertTrue(ZoneCatalog.isPickable("Asia/Tbilisi"))
        assertTrue(ZoneCatalog.isPickable("America/Argentina/Buenos_Aires"))
        assertFalse(ZoneCatalog.isPickable("Etc/GMT+3"))
        assertFalse(ZoneCatalog.isPickable("SystemV/EST5"))
        assertFalse(ZoneCatalog.isPickable("US/Pacific"))
        assertFalse(ZoneCatalog.isPickable("Canada/Eastern"))
        assertFalse(ZoneCatalog.isPickable("CET"))
        assertFalse(ZoneCatalog.isPickable("EST5EDT"))
        assertFalse(ZoneCatalog.isPickable("UTC"))
    }

    @Test
    fun `deprecated spellings are dropped so a city appears once`() {
        val ids = all.map { it.id.id }
        assertTrue("Asia/Kolkata" in ids)
        assertFalse("Asia/Calcutta" in ids)
        assertTrue("Europe/Kyiv" in ids)
        assertFalse("Europe/Kiev" in ids)
        assertEquals(1, all.count { it.city == "Kolkata" })
    }

    @Test
    fun `entry splits city and region and replaces underscores`() {
        val la = ZoneCatalog.entryFor(ZoneId.of("America/Los_Angeles"))
        assertEquals("Los Angeles", la.city)
        assertEquals("America", la.region)
        assertEquals("Los Angeles, America", la.label)

        val ba = ZoneCatalog.entryFor(ZoneId.of("America/Argentina/Buenos_Aires"))
        assertEquals("Buenos Aires", ba.city)
        assertEquals("Argentina, America", ba.region)
    }

    @Test
    fun `catalogue is sorted by city and contains the usual suspects`() {
        val cities = all.map { ZoneCatalog.fold(it.city) }
        assertEquals(cities.sorted(), cities)
        for (id in listOf("Asia/Tbilisi", "Asia/Dubai", "Europe/London", "America/New_York", "Asia/Tokyo")) {
            assertTrue(id, all.any { it.id.id == id })
        }
        assertTrue(all.size > 300)
    }

    @Test
    fun `search puts prefix matches before substring matches`() {
        val cities = ZoneCatalog.search("ang", all).map { it.city }
        assertEquals("Anguilla", cities.first())
        assertTrue("Bangkok" in cities)
        assertTrue("Los Angeles" in cities)
        assertTrue(cities.indexOf("Anguilla") < cities.indexOf("Bangkok"))
        assertTrue(cities.indexOf("Anguilla") < cities.indexOf("Los Angeles"))
        // Within a group the catalogue's city order is kept.
        assertTrue(cities.indexOf("Bangkok") < cities.indexOf("Los Angeles"))
    }

    @Test
    fun `search is case and accent insensitive`() {
        assertEquals("Sao Paulo", ZoneCatalog.search("SÃO", all).first().city)
        assertEquals("Tbilisi", ZoneCatalog.search("tBiL", all).first().city)
        assertEquals("Zurich", ZoneCatalog.search("Zürich", all).first().city)
    }

    @Test
    fun `search also matches on region`() {
        val results = ZoneCatalog.search("argentina", all)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.region.contains("Argentina") })
    }

    @Test
    fun `blank query returns the whole catalogue`() {
        assertEquals(all, ZoneCatalog.search("", all))
        assertEquals(all, ZoneCatalog.search("   ", all))
    }

    @Test
    fun `no match returns an empty list`() {
        assertTrue(ZoneCatalog.search("xyzzy", all).isEmpty())
    }

    @Test
    fun `fold strips accents and case`() {
        assertEquals("sao paulo", ZoneCatalog.fold("São Paulo"))
        assertEquals("zurich", ZoneCatalog.fold("Zürich"))
        assertEquals("reykjavik", ZoneCatalog.fold("Reykjavík"))
    }
}
