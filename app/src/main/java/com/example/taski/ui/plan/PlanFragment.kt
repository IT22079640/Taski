package com.example.taski.ui.plan

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.taski.R
import com.example.taski.plan.FocusPlan
import com.example.taski.plan.FocusPlanBuilder
import com.example.taski.ui.focus.FocusFragment
import com.example.taski.ui.tasks.TaskEditorFragment
import com.example.taski.utils.DateUtils
import com.example.taski.viewmodel.PlanViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class PlanFragment : Fragment() {

    private val viewModel: PlanViewModel by viewModels()
    private var suppressChipCallback = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_plan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_plan)
        val adapter = PlanTaskAdapter(
            onOpenTask = { openEditor(it.id) },
            onStartFocus = { openFocus(it.id) }
        )
        recycler.adapter = adapter

        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_time)
        val chipCustom = view.findViewById<Chip>(R.id.chip_time_custom)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressChipCallback) return@setOnCheckedStateChangeListener
            when (checkedIds.firstOrNull()) {
                R.id.chip_time_1h ->
                    viewModel.setAvailableMinutes(FocusPlanBuilder.PRESET_ONE_HOUR_MINUTES)
                R.id.chip_time_2h ->
                    viewModel.setAvailableMinutes(FocusPlanBuilder.PRESET_TWO_HOURS_MINUTES)
                R.id.chip_time_4h ->
                    viewModel.setAvailableMinutes(FocusPlanBuilder.PRESET_FOUR_HOURS_MINUTES)
            }
        }
        chipCustom.setOnClickListener { showCustomTimeDialog() }

        val loading = view.findViewById<View>(R.id.plan_loading)
        val emptyState = view.findViewById<View>(R.id.plan_empty_state)
        val summaryTasks = view.findViewById<TextView>(R.id.text_summary_tasks)
        val summaryPlanned = view.findViewById<TextView>(R.id.text_summary_planned)
        val summaryAvailable = view.findViewById<TextView>(R.id.text_summary_available)
        val overflowNote = view.findViewById<View>(R.id.text_overflow_note)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                PlanViewModel.UiState.Loading -> {
                    loading.isVisible = true
                    emptyState.isVisible = false
                    recycler.isVisible = false
                    overflowNote.isVisible = false
                }
                is PlanViewModel.UiState.Ready -> {
                    loading.isVisible = false
                    bindSummary(state.plan, summaryTasks, summaryPlanned, summaryAvailable)
                    overflowNote.isVisible = state.plan.exceedsAvailableTime
                    emptyState.isVisible = state.plan.isCaughtUp
                    recycler.isVisible = !state.plan.isCaughtUp
                    adapter.submitList(state.plan.selectedTasks)
                    bindTimeChips(chipGroup, chipCustom, state.plan.availableMinutes)
                }
            }
        }
    }

    private fun bindSummary(
        plan: FocusPlan,
        tasksView: TextView,
        plannedView: TextView,
        availableView: TextView
    ) {
        tasksView.text = plan.taskCount.toString()
        tasksView.contentDescription = getString(R.string.plan_summary_tasks_cd, plan.taskCount)
        plannedView.text = DateUtils.formatEffortHours(plan.totalPlannedMinutes)
        plannedView.contentDescription = getString(
            R.string.plan_summary_planned_cd,
            DateUtils.formatEffortHours(plan.totalPlannedMinutes)
        )
        availableView.text = DateUtils.formatEffortHours(plan.availableMinutes)
        availableView.contentDescription = getString(
            R.string.plan_summary_available_cd,
            DateUtils.formatEffortHours(plan.availableMinutes)
        )
    }

    private fun bindTimeChips(chipGroup: ChipGroup, chipCustom: Chip, minutes: Int) {
        val chipId = when (minutes) {
            FocusPlanBuilder.PRESET_ONE_HOUR_MINUTES -> R.id.chip_time_1h
            FocusPlanBuilder.PRESET_TWO_HOURS_MINUTES -> R.id.chip_time_2h
            FocusPlanBuilder.PRESET_FOUR_HOURS_MINUTES -> R.id.chip_time_4h
            else -> R.id.chip_time_custom
        }
        if (chipGroup.checkedChipId != chipId) {
            suppressChipCallback = true
            chipGroup.check(chipId)
            suppressChipCallback = false
        }
        chipCustom.text = if (FocusPlanBuilder.isPresetMinutes(minutes)) {
            getString(R.string.plan_time_custom)
        } else {
            getString(R.string.plan_time_custom_with_value, DateUtils.formatEffortHours(minutes))
        }
    }

    private fun showCustomTimeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_focus_time, null, false)
        val layout = dialogView.findViewById<TextInputLayout>(R.id.layout_custom_hours)
        val input = dialogView.findViewById<TextInputEditText>(R.id.input_custom_hours)
        val currentMinutes = viewModel.currentAvailableMinutes()
        if (!FocusPlanBuilder.isPresetMinutes(currentMinutes) && currentMinutes > 0) {
            input.setText(DateUtils.minutesToHoursText(currentMinutes))
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.plan_custom_time_title)
            .setView(dialogView)
            .setPositiveButton(R.string.plan_custom_time_apply, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val hours = input.text?.toString()?.trim()?.toDoubleOrNull()
                val minutes = hours?.let { DateUtils.hoursToMinutes(it) } ?: 0
                if (hours == null || hours <= 0.0 || minutes <= 0) {
                    layout.error = getString(R.string.plan_custom_time_error)
                    return@setOnClickListener
                }
                if (minutes > FocusPlanBuilder.MAX_AVAILABLE_MINUTES) {
                    layout.error = getString(R.string.plan_custom_time_max_error)
                    return@setOnClickListener
                }
                viewModel.setAvailableMinutes(minutes)
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener {
            view?.findViewById<ChipGroup>(R.id.chip_group_time)?.let { group ->
                view?.findViewById<Chip>(R.id.chip_time_custom)?.let { custom ->
                    bindTimeChips(group, custom, viewModel.currentAvailableMinutes())
                }
            }
        }
        dialog.show()
    }

    private fun openEditor(taskId: Long) {
        val args = Bundle().apply { putLong(TaskEditorFragment.ARG_TASK_ID, taskId) }
        findNavController().navigate(R.id.taskEditorFragment, args)
    }

    private fun openFocus(taskId: Long) {
        val args = Bundle().apply { putLong(FocusFragment.ARG_TASK_ID, taskId) }
        findNavController().navigate(R.id.focusFragment, args)
    }
}
