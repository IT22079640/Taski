package com.example.taski.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.taski.R
import com.example.taski.ui.onboarding.OnboardingPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_splash, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            if (savedInstanceState == null) {
                delay(SPLASH_DELAY_MS)
            }
            if (!isAdded) return@launch
            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.splashFragment) return@launch
            val destination = if (OnboardingPreferences.isCompleted(requireContext())) {
                R.id.action_splash_to_main
            } else {
                R.id.action_splash_to_onboarding
            }
            navController.navigate(destination)
        }
    }

    private companion object {
        const val SPLASH_DELAY_MS = 1600L
    }
}
