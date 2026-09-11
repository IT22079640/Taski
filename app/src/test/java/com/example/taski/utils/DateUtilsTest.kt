package com.example.taski.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateUtilsTest {

    private val colombo: TimeZone = TimeZone.getTimeZone("Asia/Colombo")
    private val pacific: TimeZone = TimeZone.getTimeZone("America/Los_Angeles")

    @Test
    fun localDateTime_staysOnSelectedCalendarDayAndHour() {
        val millis = DateUtils.localDateTimeMillis(
            year = 2026,
            month = Calendar.SEPTEMBER,
            day = 20,
            hourOfDay = 18,
            minute = 0,
            timeZone = colombo
        )
        val calendar = Calendar.getInstance(colombo)
        calendar.timeInMillis = millis
        assertEquals(2026, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, calendar.get(Calendar.MONTH))
        assertEquals(20, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals("20 Sep 2026", DateUtils.formatDisplay(millis, colombo))
        assertEquals(18, DateUtils.hourOfDay(millis, colombo))
    }

    @Test
    fun utcPickerDateCombinedWithLocalTime_doesNotShiftTheCalendarDay() {
        val utc = TimeZone.getTimeZone("UTC")
        val utcMidnight = DateUtils.localDateTimeMillis(
            year = 2026,
            month = Calendar.SEPTEMBER,
            day = 20,
            hourOfDay = 0,
            minute = 0,
            timeZone = utc
        )
        val combined = DateUtils.combineUtcPickerDateWithLocalTime(
            utcMidnightMillis = utcMidnight,
            hourOfDay = 18,
            minute = 0,
            timeZone = pacific
        )
        val calendar = Calendar.getInstance(pacific)
        calendar.timeInMillis = combined
        assertEquals(20, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.SEPTEMBER, calendar.get(Calendar.MONTH))
        assertEquals(18, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(
            utcMidnight,
            DateUtils.localMillisToUtcPickerDate(combined, pacific)
        )
    }

    @Test
    fun dateOnlyMidnight_migratesToSixPmSameLocalDay() {
        val midnight = DateUtils.localDateTimeMillis(
            year = 2026,
            month = Calendar.SEPTEMBER,
            day = 11,
            hourOfDay = 0,
            minute = 0,
            timeZone = colombo
        )
        assertTrue(DateUtils.isLocalStartOfDay(midnight, colombo))
        val migrated = DateUtils.withDefaultEveningIfDateOnly(midnight, colombo)
        assertEquals(18, DateUtils.hourOfDay(migrated, colombo))
        assertEquals(11, Calendar.getInstance(colombo).apply { timeInMillis = migrated }.get(Calendar.DAY_OF_MONTH))
        assertEquals(migrated, DateUtils.withDefaultEveningIfDateOnly(migrated, colombo))
    }

    @Test
    fun existingAfternoonTime_isNotChangedByDateOnlyMigration() {
        val afternoon = DateUtils.localDateTimeMillis(
            year = 2026,
            month = Calendar.SEPTEMBER,
            day = 11,
            hourOfDay = 16,
            minute = 30,
            timeZone = colombo
        )
        assertFalse(DateUtils.isLocalStartOfDay(afternoon, colombo))
        assertEquals(afternoon, DateUtils.withDefaultEveningIfDateOnly(afternoon, colombo))
    }
}
