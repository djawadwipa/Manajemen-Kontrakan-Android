package id.djawadwipa.manajemenkontrakan.data

import id.djawadwipa.manajemenkontrakan.data.repository.SeedData
import org.junit.Assert.assertEquals
import org.junit.Test

class SeedDataTest {
    @Test
    fun januaryPotential_matchesWorkbook() {
        val invoices = SeedData.invoices(2026, SeedData.units)
        assertEquals(29_900_000L, invoices.filter { it.period == "2026-01" }.sumOf { it.amount })
        assertEquals(12_700_000L, invoices.filter { it.period == "2026-02" }.sumOf { it.amount })
        assertEquals(16, SeedData.units.size)
    }
}
