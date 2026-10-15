package com.ahmoodiy.prayer.domain.prayer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class EgyptPrayerEngineTest {

    @Test
    fun dstStartsOnTheLastFridayOfApril() {
        assertFalse(EgyptPrayerEngine.isEgyptianDst(date(2025, Calendar.APRIL, 24)))
        assertTrue(EgyptPrayerEngine.isEgyptianDst(date(2025, Calendar.APRIL, 25)))
        assertTrue(EgyptPrayerEngine.isEgyptianDst(date(2025, Calendar.APRIL, 30)))
    }

    @Test
    fun dstEndsOnTheLastThursdayOfOctober() {
        assertTrue(EgyptPrayerEngine.isEgyptianDst(date(2025, Calendar.OCTOBER, 30)))
        assertFalse(EgyptPrayerEngine.isEgyptianDst(date(2025, Calendar.OCTOBER, 31)))
        assertTrue(EgyptPrayerEngine.isEgyptianDst(date(2026, Calendar.OCTOBER, 29)))
        assertFalse(EgyptPrayerEngine.isEgyptianDst(date(2026, Calendar.OCTOBER, 30)))
    }

    @Test
    fun dstIsInactiveOutsideTheSeason() {
        assertFalse(EgyptPrayerEngine.isEgyptianDst(date(2025, Calendar.JANUARY, 15)))
        assertTrue(EgyptPrayerEngine.isEgyptianDst(date(2025, Calendar.JULY, 15)))
        assertFalse(EgyptPrayerEngine.isEgyptianDst(date(2025, Calendar.NOVEMBER, 1)))
    }

    private fun date(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("Africa/Cairo")).apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }
}
