package com.example.taski

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.taski.reminder.NotificationPermissionHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var notificationPromptShown = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* App continues normally whether granted or denied. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.main)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val navHost = findViewById<View>(R.id.nav_host_fragment)
        bottomNav.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomNav = MAIN_DESTINATIONS.contains(destination.id)
            applyBottomNavVisibility(navHost, bottomNav, showBottomNav)
            if (showBottomNav && !notificationPromptShown) {
                notificationPromptShown = true
                NotificationPermissionHelper.maybePrompt(this, notificationPermissionLauncher)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }
    }

    private fun applyBottomNavVisibility(
        navHost: View,
        bottomNav: BottomNavigationView,
        show: Boolean
    ) {
        bottomNav.visibility = if (show) View.VISIBLE else View.GONE
        val params = navHost.layoutParams as ConstraintLayout.LayoutParams
        if (show) {
            params.bottomToTop = R.id.bottom_nav
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
        } else {
            params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        navHost.layoutParams = params
    }

    private companion object {
        val MAIN_DESTINATIONS = setOf(
            R.id.homeFragment,
            R.id.tasksFragment,
            R.id.planFragment,
            R.id.progressFragment,
            R.id.profileFragment
        )
    }
}
