package com.example.taski.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.taski.R
import com.example.taski.data.entity.Task
import com.example.taski.progress.FocusTimeFormatter
import com.example.taski.progress.FocusTimeParts
import com.example.taski.ui.tasks.TaskDetailsFragment
import com.example.taski.utils.DateUtils
import com.example.taski.utils.ImportanceLabels
import com.example.taski.viewmodel.HomeViewModel
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.btn_quick_add).setOnClickListener {
            findNavController().navigate(R.id.taskEditorFragment)
        }
        view.findViewById<MaterialButton>(R.id.btn_view_tasks).setOnClickListener {
            navigateToTab(R.id.tasksFragment)
        }
        val openPlan = View.OnClickListener { navigateToTab(R.id.planFragment) }
        view.findViewById<View>(R.id.card_plan).setOnClickListener(openPlan)
        view.findViewById<MaterialButton>(R.id.btn_open_plan).setOnClickListener(openPlan)
        val openProgress = View.OnClickListener { navigateToTab(R.id.progressFragment) }
        view.findViewById<View>(R.id.card_progress).setOnClickListener(openProgress)
        view.findViewById<MaterialButton>(R.id.btn_view_progress).setOnClickListener(openProgress)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            bindHome(view, state)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDayBounds()
    }

    private fun bindHome(view: View, state: HomeViewModel.HomeUiState) {
        view.findViewById<TextView>(R.id.home_message).text = when {
            !state.hasPending && state.completedToday > 0 ->
                getString(R.string.home_status_caught_up)
            state.hasPending ->
                getString(R.string.home_status_pending, state.pendingCount, state.completedToday)
            else -> getString(R.string.home_message)
        }

        val emptyPriorities = view.findViewById<View>(R.id.home_priorities_empty)
        val container = view.findViewById<LinearLayout>(R.id.container_priorities)
        emptyPriorities.isVisible = state.topTasks.isEmpty()
        container.isVisible = state.topTasks.isNotEmpty()
        bindPriorityTasks(container, state.topTasks)

        view.findViewById<TextView>(R.id.text_plan_summary).text = if (state.hasPlan) {
            getString(
                R.string.home_plan_ready,
                state.planTaskCount,
                DateUtils.formatEffortHours(state.planMinutes)
            )
        } else {
            getString(R.string.home_plan_empty)
        }

        val streakLabel = streakLabel(state.streakDays)
        val focusLabel = formatFocusTime(state.todayFocusMillis)
        view.findViewById<TextView>(R.id.text_progress_summary).text = if (state.hasProgress) {
            getString(R.string.home_progress_summary, streakLabel, focusLabel)
        } else {
            getString(R.string.progress_empty_body)
        }
    }

    private fun bindPriorityTasks(container: LinearLayout, tasks: List<Task>) {
        container.removeAllViews()
        val inflater = layoutInflater
        tasks.forEachIndexed { index, task ->
            if (index > 0) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.divider_height)
                    )
                    setBackgroundResource(R.color.divider)
                }
                container.addView(divider)
            }
            val row = inflater.inflate(R.layout.item_home_priority, container, false)
            row.findViewById<TextView>(R.id.text_title).text = task.title
            row.findViewById<TextView>(R.id.text_meta).text = getString(
                R.string.home_priority_meta,
                DateUtils.formatDateTime(task.deadline),
                ImportanceLabels.toLabel(task.importance),
                task.priorityScore
            )
            row.contentDescription = getString(
                R.string.cd_home_open_task,
                task.title,
                task.priorityScore
            )
            row.setOnClickListener { openTask(task.id) }
            container.addView(row)
        }
    }

    private fun openTask(taskId: Long) {
        val args = Bundle().apply { putLong(TaskDetailsFragment.ARG_TASK_ID, taskId) }
        findNavController().navigate(R.id.taskDetailsFragment, args)
    }

    private fun navigateToTab(destinationId: Int) {
        if (findNavController().currentDestination?.id == destinationId) return
        findNavController().navigate(
            destinationId,
            null,
            navOptions {
                popUpTo(R.id.homeFragment) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        )
    }

    private fun streakLabel(days: Int): String = when (days) {
        1 -> getString(R.string.progress_streak_one)
        else -> getString(R.string.progress_streak_days, days)
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
}
