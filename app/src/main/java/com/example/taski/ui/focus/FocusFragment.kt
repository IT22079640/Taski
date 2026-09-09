package com.example.taski.ui.focus

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
import androidx.navigation.navOptions
import com.example.taski.R
import com.example.taski.focus.FocusTimerEngine
import com.example.taski.focus.FocusTimerStatus
import com.example.taski.viewmodel.FocusViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class FocusFragment : Fragment() {

    private val viewModel: FocusViewModel by viewModels()
    private var suppressChipCallback = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_focus, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val taskId = arguments?.getLong(ARG_TASK_ID, 0L) ?: 0L
        viewModel.load(taskId)

        view.findViewById<MaterialToolbar>(R.id.focus_toolbar)
            .setNavigationOnClickListener { findNavController().popBackStack() }

        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_duration)
        val chipCustom = view.findViewById<Chip>(R.id.chip_duration_custom)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressChipCallback) return@setOnCheckedStateChangeListener
            when (checkedIds.firstOrNull()) {
                R.id.chip_duration_25 ->
                    viewModel.setDurationMillis(FocusTimerEngine.DEFAULT_DURATION_MS)
                R.id.chip_duration_50 ->
                    viewModel.setDurationMillis(FocusTimerEngine.PRESET_50_MS)
            }
        }
        chipCustom.setOnClickListener { showCustomDurationDialog() }

        view.findViewById<MaterialButton>(R.id.btn_start).setOnClickListener { viewModel.start() }
        view.findViewById<MaterialButton>(R.id.btn_pause).setOnClickListener { viewModel.pause() }
        view.findViewById<MaterialButton>(R.id.btn_stop).setOnClickListener { viewModel.stop() }
        view.findViewById<MaterialButton>(R.id.btn_back_to_plan).setOnClickListener { goToPlan() }
        view.findViewById<MaterialButton>(R.id.btn_go_to_tasks).setOnClickListener { goToTasks() }

        val loading = view.findViewById<View>(R.id.focus_loading)
        val missing = view.findViewById<View>(R.id.focus_missing)
        val notFound = view.findViewById<View>(R.id.focus_not_found)
        val content = view.findViewById<View>(R.id.focus_content)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            loading.isVisible = state is FocusViewModel.UiState.Loading
            missing.isVisible = state is FocusViewModel.UiState.MissingTask
            notFound.isVisible = state is FocusViewModel.UiState.NotFound
            content.isVisible = state is FocusViewModel.UiState.Ready
            val ready = state as? FocusViewModel.UiState.Ready
            view.keepScreenOn = ready?.status == FocusTimerStatus.Running
            if (ready != null) {
                bindReady(view, ready, chipGroup, chipCustom)
            }
        }
    }

    override fun onDestroyView() {
        view?.keepScreenOn = false
        super.onDestroyView()
    }

    private fun bindReady(
        view: View,
        state: FocusViewModel.UiState.Ready,
        chipGroup: ChipGroup,
        chipCustom: Chip
    ) {
        view.findViewById<TextView>(R.id.text_task_title).text = state.task.title
        view.findViewById<TextView>(R.id.text_countdown).text = state.remainingLabel
        view.findViewById<TextView>(R.id.text_session_status).text = statusLabel(state)
        view.findViewById<CircularProgressIndicator>(R.id.timer_progress).apply {
            max = FocusTimerEngine.PROGRESS_MAX
            setProgressCompat(state.progress, true)
        }

        val startButton = view.findViewById<MaterialButton>(R.id.btn_start)
        startButton.isEnabled = state.startEnabled
        startButton.text = getString(if (state.isResume) R.string.focus_resume else R.string.focus_start)
        view.findViewById<MaterialButton>(R.id.btn_pause).isEnabled = state.pauseEnabled
        view.findViewById<MaterialButton>(R.id.btn_stop).isEnabled = state.stopEnabled

        view.findViewById<View>(R.id.container_controls).isVisible = !state.showFinished
        view.findViewById<View>(R.id.card_duration).isVisible = !state.showFinished
        chipGroup.isEnabled = state.durationEnabled
        chipCustom.isEnabled = state.durationEnabled
        view.findViewById<Chip>(R.id.chip_duration_25).isEnabled = state.durationEnabled
        view.findViewById<Chip>(R.id.chip_duration_50).isEnabled = state.durationEnabled

        val completeCard = view.findViewById<View>(R.id.card_complete)
        completeCard.isVisible = state.showFinished
        if (state.showFinished) {
            view.findViewById<TextView>(R.id.text_complete_title).text = getString(
                if (state.finishedFully) R.string.focus_complete_title else R.string.focus_stopped_title
            )
            view.findViewById<TextView>(R.id.text_complete_body).text = getString(
                if (state.finishedFully) R.string.focus_complete_body else R.string.focus_stopped_body
            )
        }

        bindDurationChips(chipGroup, chipCustom, state.durationMillis)
    }

    private fun statusLabel(state: FocusViewModel.UiState.Ready): String = when (state.status) {
        FocusTimerStatus.Idle -> getString(R.string.focus_status_ready)
        FocusTimerStatus.Running -> getString(R.string.focus_status_running)
        FocusTimerStatus.Paused -> getString(R.string.focus_status_paused)
        FocusTimerStatus.Finished -> getString(
            if (state.finishedFully) R.string.focus_status_complete else R.string.focus_status_stopped
        )
    }

    private fun bindDurationChips(chipGroup: ChipGroup, chipCustom: Chip, durationMillis: Long) {
        val chipId = when (durationMillis) {
            FocusTimerEngine.DEFAULT_DURATION_MS -> R.id.chip_duration_25
            FocusTimerEngine.PRESET_50_MS -> R.id.chip_duration_50
            else -> R.id.chip_duration_custom
        }
        if (chipGroup.checkedChipId != chipId) {
            suppressChipCallback = true
            chipGroup.check(chipId)
            suppressChipCallback = false
        }
        chipCustom.text = if (FocusTimerEngine.isPresetMillis(durationMillis)) {
            getString(R.string.focus_duration_custom)
        } else {
            getString(
                R.string.focus_duration_custom_value,
                FocusTimerEngine.millisToWholeMinutes(durationMillis)
            )
        }
    }

    private fun showCustomDurationDialog() {
        val currentState = viewModel.uiState.value as? FocusViewModel.UiState.Ready
        if (currentState != null && !currentState.durationEnabled) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_focus_duration, null, false)
        val layout = dialogView.findViewById<TextInputLayout>(R.id.layout_custom_minutes)
        val input = dialogView.findViewById<TextInputEditText>(R.id.input_custom_minutes)
        val currentMillis = currentState?.durationMillis ?: FocusTimerEngine.DEFAULT_DURATION_MS
        if (!FocusTimerEngine.isPresetMillis(currentMillis)) {
            input.setText(FocusTimerEngine.millisToWholeMinutes(currentMillis).toString())
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.focus_custom_duration_title)
            .setView(dialogView)
            .setPositiveButton(R.string.focus_custom_duration_apply, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val minutes = input.text?.toString()?.trim()?.toIntOrNull()
                if (minutes == null || !FocusTimerEngine.validateDurationMinutes(minutes)) {
                    layout.error = getString(R.string.focus_custom_duration_error)
                    return@setOnClickListener
                }
                viewModel.setDurationMillis(FocusTimerEngine.minutesToMillis(minutes))
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener {
            val ready = viewModel.uiState.value as? FocusViewModel.UiState.Ready ?: return@setOnDismissListener
            view?.findViewById<ChipGroup>(R.id.chip_group_duration)?.let { group ->
                view?.findViewById<Chip>(R.id.chip_duration_custom)?.let { custom ->
                    bindDurationChips(group, custom, ready.durationMillis)
                }
            }
        }
        dialog.show()
    }

    private fun goToPlan() {
        val popped = findNavController().popBackStack(R.id.planFragment, false)
        if (!popped) {
            findNavController().navigate(
                R.id.planFragment,
                null,
                navOptions { popUpTo(R.id.homeFragment) { inclusive = false } }
            )
        }
    }

    private fun goToTasks() {
        val popped = findNavController().popBackStack(R.id.tasksFragment, false)
        if (!popped) {
            findNavController().navigate(
                R.id.tasksFragment,
                null,
                navOptions { popUpTo(R.id.homeFragment) { inclusive = false } }
            )
        }
    }

    companion object {
        const val ARG_TASK_ID = "taskId"
    }
}
