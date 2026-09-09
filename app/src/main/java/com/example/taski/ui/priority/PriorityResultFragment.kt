package com.example.taski.ui.priority

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.taski.R
import com.example.taski.data.entity.Task
import com.example.taski.priority.PriorityLevel
import com.example.taski.priority.PriorityResult
import com.example.taski.utils.DateUtils
import com.example.taski.utils.ImportanceLabels
import com.example.taski.viewmodel.PriorityResultViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class PriorityResultFragment : Fragment() {

    private val viewModel: PriorityResultViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_priority_result, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val taskId = arguments?.getLong(ARG_TASK_ID, 0L) ?: 0L
        view.findViewById<MaterialToolbar>(R.id.priority_toolbar)
            .setNavigationOnClickListener { findNavController().popBackStack() }
        view.findViewById<MaterialButton>(R.id.btn_continue).setOnClickListener {
            continueToTasks()
        }

        val loading = view.findViewById<View>(R.id.priority_loading)
        val notFound = view.findViewById<View>(R.id.priority_not_found)
        val content = view.findViewById<View>(R.id.priority_content)
        val continueButton = view.findViewById<View>(R.id.btn_continue)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            loading.isVisible = state is PriorityResultViewModel.UiState.Loading
            notFound.isVisible = state is PriorityResultViewModel.UiState.NotFound
            content.isVisible = state is PriorityResultViewModel.UiState.Ready
            continueButton.isVisible = state is PriorityResultViewModel.UiState.Ready
            if (state is PriorityResultViewModel.UiState.Ready) {
                bindResult(view, state.task, state.priority)
            }
        }

        viewModel.load(taskId)
    }

    private fun bindResult(view: View, task: Task, priority: PriorityResult) {
        val score = task.priorityScore
        val level = PriorityLevel.fromScore(score)

        view.findViewById<TextView>(R.id.text_task_title).text = task.title
        view.findViewById<TextView>(R.id.text_score).text = score.toString()
        view.findViewById<TextView>(R.id.text_score_caption).text =
            getString(R.string.priority_score_out_of, score)
        view.findViewById<TextView>(R.id.text_deadline).text =
            getString(R.string.priority_deadline_value, DateUtils.formatDisplay(task.deadline))
        view.findViewById<TextView>(R.id.text_importance).text =
            getString(R.string.priority_importance_value, ImportanceLabels.toLabel(task.importance))
        view.findViewById<TextView>(R.id.text_effort).text =
            getString(R.string.priority_effort_value, DateUtils.formatEffortHours(task.estimatedEffort))

        val levelView = view.findViewById<TextView>(R.id.text_priority_level)
        levelView.text = levelLabel(level)
        levelView.setTextColor(ContextCompat.getColor(requireContext(), levelColor(level)))
        view.findViewById<View>(R.id.score_circle).contentDescription =
            getString(R.string.cd_priority_score, score, levelLabel(level))
        view.findViewById<TextView>(R.id.text_score).importantForAccessibility =
            View.IMPORTANT_FOR_ACCESSIBILITY_NO

        view.findViewById<TextView>(R.id.label_urgency).text =
            getString(R.string.priority_factor_urgency) + " · " + getString(R.string.priority_factor_weight_urgency)
        view.findViewById<TextView>(R.id.label_importance_factor).text =
            getString(R.string.priority_factor_importance) + " · " + getString(R.string.priority_factor_weight_importance)
        view.findViewById<TextView>(R.id.label_effort_factor).text =
            getString(R.string.priority_factor_effort) + " · " + getString(R.string.priority_factor_weight_effort)

        bindFactor(
            view,
            R.id.bar_urgency,
            R.id.value_urgency,
            R.id.label_urgency,
            getString(R.string.priority_factor_urgency),
            getString(R.string.priority_factor_weight_urgency),
            priority.urgencyScore
        )
        bindFactor(
            view,
            R.id.bar_importance,
            R.id.value_importance,
            R.id.label_importance_factor,
            getString(R.string.priority_factor_importance),
            getString(R.string.priority_factor_weight_importance),
            priority.importanceScore
        )
        bindFactor(
            view,
            R.id.bar_effort,
            R.id.value_effort,
            R.id.label_effort_factor,
            getString(R.string.priority_factor_effort),
            getString(R.string.priority_factor_weight_effort),
            priority.effortScore
        )

        val reasonsContainer = view.findViewById<LinearLayout>(R.id.container_reasons)
        reasonsContainer.removeAllViews()
        priority.reasons.forEach { reason ->
            val reasonView = TextView(requireContext())
            reasonView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.spacing_sm)
            }
            reasonView.text = getString(R.string.priority_reason_item, reason)
            reasonView.setTextAppearance(R.style.TextAppearance_Taski_Body)
            reasonsContainer.addView(reasonView)
        }
    }

    private fun bindFactor(
        view: View,
        barId: Int,
        valueId: Int,
        labelId: Int,
        name: String,
        weight: String,
        score: Int
    ) {
        view.findViewById<LinearProgressIndicator>(barId).apply {
            max = 100
            setProgressCompat(score, false)
            contentDescription = getString(R.string.cd_priority_factor, name, weight, score)
        }
        view.findViewById<TextView>(valueId).text = getString(R.string.priority_factor_value, score)
        view.findViewById<TextView>(labelId).contentDescription =
            getString(R.string.cd_priority_factor, name, weight, score)
    }

    private fun continueToTasks() {
        val popped = findNavController().popBackStack(R.id.tasksFragment, false)
        if (!popped) {
            findNavController().navigate(
                R.id.tasksFragment,
                null,
                navOptions { popUpTo(R.id.homeFragment) { inclusive = false } }
            )
        }
    }

    private fun levelLabel(level: PriorityLevel): String = when (level) {
        PriorityLevel.HIGH -> getString(R.string.priority_level_high)
        PriorityLevel.MEDIUM -> getString(R.string.priority_level_medium)
        PriorityLevel.LOW -> getString(R.string.priority_level_low)
    }

    private fun levelColor(level: PriorityLevel): Int = when (level) {
        PriorityLevel.HIGH -> R.color.priority_high
        PriorityLevel.MEDIUM -> R.color.priority_medium
        PriorityLevel.LOW -> R.color.accent
    }

    companion object {
        const val ARG_TASK_ID = "taskId"
    }
}
