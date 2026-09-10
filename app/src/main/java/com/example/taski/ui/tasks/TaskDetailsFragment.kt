package com.example.taski.ui.tasks

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
import com.example.taski.priority.PriorityLevel
import com.example.taski.progress.FocusTimeFormatter
import com.example.taski.progress.FocusTimeParts
import com.example.taski.ui.focus.FocusFragment
import com.example.taski.utils.DateUtils
import com.example.taski.utils.ImportanceLabels
import com.example.taski.viewmodel.TaskDetailsViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator

class TaskDetailsFragment : Fragment() {

    private val viewModel: TaskDetailsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_task_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.details_toolbar)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_delete_task) {
                confirmDelete()
                true
            } else {
                false
            }
        }

        view.findViewById<MaterialButton>(R.id.btn_edit).setOnClickListener { openEditor() }
        view.findViewById<MaterialButton>(R.id.btn_start_focus).setOnClickListener { openFocus() }
        view.findViewById<MaterialButton>(R.id.btn_complete).setOnClickListener {
            val ready = viewModel.uiState.value as? TaskDetailsViewModel.UiState.Ready ?: return@setOnClickListener
            viewModel.setCompleted(!ready.task.completed)
        }

        val loading = view.findViewById<View>(R.id.details_loading)
        val notFound = view.findViewById<View>(R.id.details_not_found)
        val content = view.findViewById<View>(R.id.details_content)
        val deleteItem = toolbar.menu.findItem(R.id.action_delete_task)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            loading.isVisible = state is TaskDetailsViewModel.UiState.Loading
            notFound.isVisible = state is TaskDetailsViewModel.UiState.NotFound
            content.isVisible = state is TaskDetailsViewModel.UiState.Ready
            deleteItem?.isVisible = state is TaskDetailsViewModel.UiState.Ready
            if (state is TaskDetailsViewModel.UiState.Ready) {
                bindDetails(view, state)
            }
        }

        viewModel.deleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted) {
                navigateToTasks()
            }
        }
    }

    private fun bindDetails(view: View, state: TaskDetailsViewModel.UiState.Ready) {
        val task = state.task
        val priority = state.priority
        val level = PriorityLevel.fromScore(task.priorityScore)
        val completed = task.completed

        view.findViewById<TextView>(R.id.text_title).text = task.title
        val description = view.findViewById<TextView>(R.id.text_description)
        description.isVisible = task.description.isNotBlank()
        description.text = task.description

        val status = view.findViewById<TextView>(R.id.text_status)
        status.text = if (completed) {
            getString(R.string.task_completed_label)
        } else {
            getString(R.string.details_status_pending)
        }
        status.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (completed) R.color.accent else R.color.primary
            )
        )

        view.findViewById<TextView>(R.id.text_deadline).text =
            getString(R.string.priority_deadline_value, DateUtils.formatDisplay(task.deadline))
        view.findViewById<TextView>(R.id.text_importance).text =
            getString(R.string.priority_importance_value, ImportanceLabels.toLabel(task.importance))
        view.findViewById<TextView>(R.id.text_effort).text =
            getString(R.string.priority_effort_value, DateUtils.formatEffortHours(task.estimatedEffort))
        view.findViewById<TextView>(R.id.text_category).text =
            getString(R.string.details_category, task.category)
        view.findViewById<TextView>(R.id.text_created).text =
            getString(R.string.details_created, DateUtils.formatDisplay(task.createdAt))

        val completedAt = view.findViewById<TextView>(R.id.text_completed_at)
        val completedAtMillis = task.completedAt
        completedAt.isVisible = completed && completedAtMillis != null
        if (completedAtMillis != null) {
            completedAt.text = getString(
                R.string.progress_completed_at,
                DateUtils.formatDisplay(completedAtMillis)
            )
        }

        val headline = view.findViewById<TextView>(R.id.text_ai_headline)
        headline.text = getString(
            R.string.details_ai_headline,
            task.priorityScore,
            levelLabel(level)
        )
        headline.setTextColor(ContextCompat.getColor(requireContext(), levelColor(level)))
        headline.contentDescription = getString(
            R.string.cd_priority_score,
            task.priorityScore,
            levelLabel(level)
        )

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
            val reasonView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.spacing_sm)
                }
                text = getString(R.string.priority_reason_item, reason)
                setTextAppearance(R.style.TextAppearance_Taski_Body)
            }
            reasonsContainer.addView(reasonView)
        }

        val hasSessions = state.sessionCount > 0
        view.findViewById<View>(R.id.text_focus_empty).isVisible = !hasSessions
        val focusTime = view.findViewById<TextView>(R.id.text_focus_time)
        val focusCount = view.findViewById<TextView>(R.id.text_focus_count)
        focusTime.isVisible = hasSessions
        focusCount.isVisible = hasSessions
        if (hasSessions) {
            focusTime.text = getString(
                R.string.details_focus_time,
                formatFocusTime(state.totalFocusMillis)
            )
            focusCount.text = resources.getQuantityString(
                R.plurals.details_focus_sessions,
                state.sessionCount,
                state.sessionCount
            )
        }

        view.findViewById<MaterialButton>(R.id.btn_complete).apply {
            setText(if (completed) R.string.task_incomplete else R.string.task_complete)
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
        view.findViewById<TextView>(labelId).text = "$name · $weight"
        view.findViewById<LinearProgressIndicator>(barId).apply {
            max = 100
            setProgressCompat(score, false)
            contentDescription = getString(R.string.cd_priority_factor, name, weight, score)
        }
        view.findViewById<TextView>(valueId).text = getString(R.string.priority_factor_value, score)
    }

    private fun openEditor() {
        val args = Bundle().apply {
            putLong(TaskEditorFragment.ARG_TASK_ID, viewModel.taskId)
            putBoolean(TaskEditorFragment.ARG_RETURN_TO_DETAILS, true)
        }
        findNavController().navigate(R.id.taskEditorFragment, args)
    }

    private fun openFocus() {
        val args = Bundle().apply { putLong(FocusFragment.ARG_TASK_ID, viewModel.taskId) }
        findNavController().navigate(R.id.focusFragment, args)
    }

    private fun confirmDelete() {
        val task = (viewModel.uiState.value as? TaskDetailsViewModel.UiState.Ready)?.task ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_task_title)
            .setMessage(getString(R.string.delete_task_message, task.title))
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.delete() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun navigateToTasks() {
        val popped = findNavController().popBackStack(R.id.tasksFragment, false)
        if (!popped) {
            findNavController().navigate(
                R.id.tasksFragment,
                null,
                navOptions {
                    popUpTo(R.id.taskDetailsFragment) { inclusive = true }
                    launchSingleTop = true
                }
            )
        }
    }

    private fun formatFocusTime(durationMillis: Long): String {
        val parts = FocusTimeFormatter.parts(durationMillis)
        return parts.toDisplayString()
    }

    private fun FocusTimeParts.toDisplayString(): String = when {
        underOneMinute -> getString(R.string.progress_focus_under_minute)
        hours == 0 && minutes == 0 -> getString(R.string.progress_focus_zero)
        hours == 0 -> getString(R.string.progress_focus_minutes, minutes)
        minutes == 0 -> getString(R.string.progress_focus_hours, hours)
        else -> getString(R.string.progress_focus_hours_minutes, hours, minutes)
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
