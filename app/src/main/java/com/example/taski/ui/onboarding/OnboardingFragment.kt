package com.example.taski.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.taski.R
import com.google.android.material.button.MaterialButton

class OnboardingFragment : Fragment() {

    private var pageIndex = 0

    private val pages = listOf(
        Page(
            titleRes = R.string.onboarding_page_1_title,
            bodyRes = R.string.onboarding_page_1_body,
            artRes = R.drawable.ic_feature_organize
        ),
        Page(
            titleRes = R.string.onboarding_page_2_title,
            bodyRes = R.string.onboarding_page_2_body,
            artRes = R.drawable.ic_feature_priority
        ),
        Page(
            titleRes = R.string.onboarding_page_3_title,
            bodyRes = R.string.onboarding_page_3_body,
            artRes = R.drawable.ic_feature_focus
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_onboarding, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pageIndex = savedInstanceState?.getInt(STATE_PAGE) ?: 0

        view.findViewById<MaterialButton>(R.id.btn_skip).setOnClickListener { completeOnboarding() }
        view.findViewById<MaterialButton>(R.id.btn_get_started).setOnClickListener { completeOnboarding() }
        view.findViewById<MaterialButton>(R.id.btn_next).setOnClickListener {
            if (pageIndex < pages.lastIndex) {
                showPage(view, pageIndex + 1)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (pageIndex > 0) {
                        showPage(view, pageIndex - 1)
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        showPage(view, pageIndex)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_PAGE, pageIndex)
    }

    private fun showPage(view: View, index: Int) {
        pageIndex = index.coerceIn(0, pages.lastIndex)
        val page = pages[pageIndex]
        val isLast = pageIndex == pages.lastIndex

        view.findViewById<TextView>(R.id.text_onboarding_title).setText(page.titleRes)
        view.findViewById<TextView>(R.id.text_onboarding_body).setText(page.bodyRes)
        view.findViewById<ImageView>(R.id.image_onboarding_art).setImageResource(page.artRes)
        view.contentDescription = getString(R.string.cd_onboarding_page, pageIndex + 1)

        bindDots(view)
        view.findViewById<View>(R.id.btn_next).isVisible = !isLast
        view.findViewById<View>(R.id.btn_get_started).isVisible = isLast
    }

    private fun bindDots(view: View) {
        styleDot(view.findViewById(R.id.dot_page_1), selected = pageIndex == 0)
        styleDot(view.findViewById(R.id.dot_page_2), selected = pageIndex == 1)
        styleDot(view.findViewById(R.id.dot_page_3), selected = pageIndex == 2)
    }

    private fun styleDot(dot: View, selected: Boolean) {
        val params = dot.layoutParams
        params.width = resources.getDimensionPixelSize(
            if (selected) R.dimen.page_dot_active_width else R.dimen.page_dot_size
        )
        params.height = resources.getDimensionPixelSize(R.dimen.page_dot_size)
        dot.layoutParams = params
        dot.setBackgroundResource(
            if (selected) R.drawable.bg_page_dot_active else R.drawable.bg_page_dot
        )
    }

    private fun completeOnboarding() {
        OnboardingPreferences.markCompleted(requireContext())
        findNavController().navigate(R.id.action_onboarding_to_main)
    }

    private data class Page(
        val titleRes: Int,
        val bodyRes: Int,
        val artRes: Int
    )

    private companion object {
        const val STATE_PAGE = "onboarding_page"
    }
}
