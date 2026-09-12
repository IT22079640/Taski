package com.example.taski.viewmodel

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.taski.plan.DailyWorkCapacityStore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences(DailyWorkCapacityStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        viewModel = ProfileViewModel(app)
    }

    @Test
    fun defaultDailyWorkMinutes_isTwoHours() {
        assertEquals(DailyWorkCapacityStore.DEFAULT_MINUTES, viewModel.currentDailyWorkMinutes())
    }

    @Test
    fun setDailyWorkMinutes_persistsAndExposesValue() {
        val observed = mutableListOf<Int>()
        viewModel.dailyWorkMinutes.observeForever { observed += it }
        viewModel.setDailyWorkMinutes(180)
        assertEquals(180, viewModel.currentDailyWorkMinutes())
        assertEquals(180, observed.last())
    }
}
