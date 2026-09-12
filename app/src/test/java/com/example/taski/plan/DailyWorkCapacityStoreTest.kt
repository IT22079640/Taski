package com.example.taski.plan

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyWorkCapacityStoreTest {

    private lateinit var store: DailyWorkCapacityStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(DailyWorkCapacityStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        store = DailyWorkCapacityStore.from(context)
    }

    @Test
    fun defaultCapacity_isTwoHoursWhenNeverConfigured() {
        assertEquals(120, store.getMinutes())
        assertEquals(FocusPlanBuilder.DEFAULT_AVAILABLE_MINUTES, store.getMinutes())
    }

    @Test
    fun savedCapacity_isLoadedCorrectly() {
        store.setMinutes(180)
        val reloaded = DailyWorkCapacityStore.from(
            ApplicationProvider.getApplicationContext()
        )
        assertEquals(180, reloaded.getMinutes())
    }

    @Test
    fun sanitize_clampsOutOfRangeValues() {
        store.setMinutes(0)
        assertEquals(DailyWorkCapacityStore.MIN_MINUTES, store.getMinutes())
        store.setMinutes(10_000)
        assertEquals(DailyWorkCapacityStore.MAX_MINUTES, store.getMinutes())
    }
}
