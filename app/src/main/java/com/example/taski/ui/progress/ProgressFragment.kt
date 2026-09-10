package com.example.taski.ui.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.taski.R
import com.example.taski.progress.FocusTimeFormatter
import com.example.taski.progress.FocusTimeParts
import com.example.taski.progress.ProgressDashboard
import com.example.taski.viewmodel.ProgressViewModel

class ProgressFragment : Fragment() {

    private val viewModel: ProgressViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_progress, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tasksAdapter = RecentCompletedTaskAdapter()
        val sessionsAdapter = RecentFocusSessionAdapter(::formatFocusTime)
        view.findViewById<RecyclerView>(R.id.recycler_recent_tasks).adapter = tasksAdapter
        view.findViewById<RecyclerView>(R.id.recycler_recent_sessions).adapter = sessionsAdapter

        viewModel.dashboard.observe(viewLifecycleOwner) { dashboard ->
            bindDashboard(view, dashboard, tasksAdapter, sessionsAdapter)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDayBounds()
    }

    private fun bindDashboard(
        view: View,
        dashboard: ProgressDashboard,
        tasksAdapter: RecentCompletedTaskAdapter,
        sessionsAdapter: RecentFocusSessionAdapter
    ) {
        view.findViewById<View>(R.id.progress_empty_intro).isVisible = !dashboard.hasAnyActivity

        view.findViewById<TextView>(R.id.text_insight_title).text = dashboard.insight.title
        view.findViewById<TextView>(R.id.text_insight_body).text = dashboard.insight.body

        val completed = view.findViewById<TextView>(R.id.text_completed_count)
        val pending = view.findViewById<TextView>(R.id.text_pending_count)
        val totalFocus = view.findViewById<TextView>(R.id.text_total_focus)
        val sessions = view.findViewById<TextView>(R.id.text_session_count)
        val todayTasks = view.findViewById<TextView>(R.id.text_today_tasks)
        val todayFocus = view.findViewById<TextView>(R.id.text_today_focus)
        val streakValue = view.findViewById<TextView>(R.id.text_streak_value)
        val streakCaption = view.findViewById<TextView>(R.id.text_streak_caption)

        val totalFocusLabel = formatFocusTime(dashboard.totalFocusMillis)
        val todayFocusLabel = formatFocusTime(dashboard.todayFocusMillis)
        val streakLabel = streakLabel(dashboard.streakDays)

        completed.text = dashboard.completedTasks.toString()
        pending.text = dashboard.pendingTasks.toString()
        totalFocus.text = totalFocusLabel
        sessions.text = dashboard.completedFocusSessions.toString()
        todayTasks.text = dashboard.todayCompletedTasks.toString()
        todayFocus.text = todayFocusLabel
        streakValue.text = streakLabel
        streakCaption.text = if (dashboard.streakDays == 0) {
            getString(R.string.progress_streak_zero)
        } else {
            getString(R.string.progress_streak_caption)
        }

        completed.contentDescription =
            getString(R.string.cd_progress_completed_tasks, dashboard.completedTasks)
        pending.contentDescription =
            getString(R.string.cd_progress_pending_tasks, dashboard.pendingTasks)
        totalFocus.contentDescription =
            getString(R.string.cd_progress_total_focus, totalFocusLabel)
        sessions.contentDescription =
            getString(R.string.cd_progress_sessions, dashboard.completedFocusSessions)
        todayTasks.contentDescription =
            getString(R.string.cd_progress_today_tasks, dashboard.todayCompletedTasks)
        todayFocus.contentDescription =
            getString(R.string.cd_progress_today_focus, todayFocusLabel)
        streakValue.contentDescription = getString(R.string.cd_progress_streak, streakLabel)

        val noRecentActivity = !dashboard.hasRecentTaskActivity && !dashboard.hasRecentFocusActivity
        view.findViewById<View>(R.id.text_recent_empty).isVisible = noRecentActivity
        view.findViewById<View>(R.id.label_recent_tasks).isVisible = !noRecentActivity
        view.findViewById<View>(R.id.label_recent_sessions).isVisible = !noRecentActivity
        view.findViewById<View>(R.id.text_recent_tasks_empty).isVisible =
            !noRecentActivity && !dashboard.hasRecentTaskActivity
        view.findViewById<View>(R.id.text_recent_sessions_empty).isVisible =
            !noRecentActivity && !dashboard.hasRecentFocusActivity

        val tasksRecycler = view.findViewById<RecyclerView>(R.id.recycler_recent_tasks)
        val sessionsRecycler = view.findViewById<RecyclerView>(R.id.recycler_recent_sessions)
        tasksRecycler.isVisible = dashboard.hasRecentTaskActivity
        sessionsRecycler.isVisible = dashboard.hasRecentFocusActivity
        tasksAdapter.submitList(dashboard.recentCompletedTasks)
        sessionsAdapter.submitList(dashboard.recentFocusSessions)
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
