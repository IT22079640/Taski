package com.example.taski.ui.tasks

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.taski.R
import com.example.taski.data.entity.Task
import com.example.taski.reminder.NotificationPermissionHelper
import com.example.taski.ui.priority.PriorityResultFragment
import com.example.taski.utils.DateUtils
import com.example.taski.utils.ImportanceLabels
import com.example.taski.viewmodel.TaskEditorViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class TaskEditorFragment : Fragment() {

    private val viewModel: TaskEditorViewModel by viewModels()

    private var selectedDeadlineMillis: Long? = null
    private var selectedHour: Int? = null
    private var selectedMinute: Int? = null
    private var hasBoundExistingTask = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_task_editor, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val taskId = arguments?.getLong(ARG_TASK_ID, 0L) ?: 0L
        val isEditing = taskId > 0L
        val returnToDetails = arguments?.getBoolean(ARG_RETURN_TO_DETAILS, false) == true
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_DEADLINE)) {
                selectedDeadlineMillis = savedInstanceState.getLong(STATE_DEADLINE)
            }
            if (savedInstanceState.containsKey(STATE_HOUR)) {
                selectedHour = savedInstanceState.getInt(STATE_HOUR)
            }
            if (savedInstanceState.containsKey(STATE_MINUTE)) {
                selectedMinute = savedInstanceState.getInt(STATE_MINUTE)
            }
        }

        val toolbar = view.findViewById<MaterialToolbar>(R.id.editor_toolbar)
        val layoutTitle = view.findViewById<TextInputLayout>(R.id.layout_title)
        val layoutDeadline = view.findViewById<TextInputLayout>(R.id.layout_deadline)
        val layoutTime = view.findViewById<TextInputLayout>(R.id.layout_time)
        val layoutEffort = view.findViewById<TextInputLayout>(R.id.layout_effort)
        val layoutImportance = view.findViewById<TextInputLayout>(R.id.layout_importance)
        val layoutCategory = view.findViewById<TextInputLayout>(R.id.layout_category)
        val inputTitle = view.findViewById<TextInputEditText>(R.id.input_title)
        val inputDescription = view.findViewById<TextInputEditText>(R.id.input_description)
        val inputDeadline = view.findViewById<TextInputEditText>(R.id.input_deadline)
        val inputTime = view.findViewById<TextInputEditText>(R.id.input_time)
        val inputEffort = view.findViewById<TextInputEditText>(R.id.input_effort)
        val inputImportance = view.findViewById<AutoCompleteTextView>(R.id.input_importance)
        val inputCategory = view.findViewById<AutoCompleteTextView>(R.id.input_category)
        val saveButton = view.findViewById<MaterialButton>(R.id.btn_save_task)

        toolbar.setTitle(if (isEditing) R.string.edit_task_title else R.string.add_task_title)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        saveButton.setText(if (isEditing) R.string.task_save_changes else R.string.task_save)
        bindReminderHelper(layoutTime)
        bindDeadlineFields(inputDeadline, inputTime)

        val importanceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.task_importance_options)
        )
        inputImportance.setAdapter(importanceAdapter)
        if (!isEditing) {
            inputImportance.setText(getString(R.string.importance_medium), false)
        }

        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.task_category_options)
        )
        inputCategory.setAdapter(categoryAdapter)

        val openDatePicker = { showDatePicker(inputDeadline, inputTime) }
        inputDeadline.setOnClickListener { openDatePicker() }
        layoutDeadline.setEndIconOnClickListener { openDatePicker() }

        val openTimePicker = { showTimePicker(inputDeadline, inputTime) }
        inputTime.setOnClickListener { openTimePicker() }
        layoutTime.setEndIconOnClickListener { openTimePicker() }

        saveButton.setOnClickListener {
            viewModel.save(
                title = inputTitle.text?.toString().orEmpty(),
                description = inputDescription.text?.toString().orEmpty(),
                deadlineMillis = currentDeadlineMillis(),
                effortText = inputEffort.text?.toString().orEmpty(),
                importanceLabel = inputImportance.text?.toString().orEmpty(),
                category = inputCategory.text?.toString().orEmpty()
            )
        }

        viewModel.saveEnabled.observe(viewLifecycleOwner) { enabled ->
            saveButton.isEnabled = enabled
        }
        viewModel.editMissing.observe(viewLifecycleOwner) { missing ->
            if (missing) {
                findNavController().popBackStack()
            }
        }

        viewModel.formErrors.observe(viewLifecycleOwner) { errors ->
            layoutTitle.error = errors.title
            layoutDeadline.error = errors.deadline
            layoutEffort.error = errors.effort
            layoutImportance.error = errors.importance
            layoutCategory.error = errors.category
        }

        viewModel.savedTaskId.observe(viewLifecycleOwner) { savedId ->
            if (savedId != null && savedId > 0L) {
                val saveResult = viewModel.saveResult.value
                viewModel.onSaveHandled()
                if (returnToDetails) {
                    val change = saveResult?.priorityChange
                    if (saveResult?.isEdit == true && change != null && change.shouldShow) {
                        findNavController().previousBackStackEntry?.savedStateHandle?.set(
                            TaskDetailsFragment.REQUEST_PRIORITY_UPDATED,
                            Bundle().apply {
                                putInt(TaskDetailsFragment.KEY_PREVIOUS_SCORE, change.previousScore)
                                putInt(TaskDetailsFragment.KEY_NEW_SCORE, change.newScore)
                                putString(TaskDetailsFragment.KEY_PREVIOUS_LEVEL, change.previousLevel.name)
                                putString(TaskDetailsFragment.KEY_NEW_LEVEL, change.newLevel.name)
                                putString(TaskDetailsFragment.KEY_LEVEL_LINE, change.levelLine)
                                putString(TaskDetailsFragment.KEY_SCORE_LINE, change.scoreLine)
                                putString(TaskDetailsFragment.KEY_EXPLANATION, change.explanation)
                            }
                        )
                    }
                    findNavController().popBackStack()
                } else {
                    val args = Bundle().apply {
                        putLong(PriorityResultFragment.ARG_TASK_ID, savedId)
                    }
                    findNavController().navigate(
                        R.id.priorityResultFragment,
                        args,
                        navOptions {
                            popUpTo(R.id.taskEditorFragment) { inclusive = true }
                        }
                    )
                }
            }
        }

        viewModel.load(taskId)
        if (isEditing) {
            viewModel.existingTask.observe(viewLifecycleOwner) { task ->
                if (task != null && savedInstanceState == null && !hasBoundExistingTask) {
                    hasBoundExistingTask = true
                    bindTask(
                        task,
                        inputTitle,
                        inputDescription,
                        inputDeadline,
                        inputTime,
                        inputEffort,
                        inputImportance,
                        inputCategory
                    )
                }
            }
        }
    }

    private fun bindTask(
        task: Task,
        inputTitle: TextInputEditText,
        inputDescription: TextInputEditText,
        inputDeadline: TextInputEditText,
        inputTime: TextInputEditText,
        inputEffort: TextInputEditText,
        inputImportance: AutoCompleteTextView,
        inputCategory: AutoCompleteTextView
    ) {
        selectedDeadlineMillis = task.deadline
        selectedHour = DateUtils.hourOfDay(task.deadline)
        selectedMinute = DateUtils.minuteOfHour(task.deadline)
        inputTitle.setText(task.title)
        inputDescription.setText(task.description)
        bindDeadlineFields(inputDeadline, inputTime)
        inputEffort.setText(DateUtils.minutesToHoursText(task.estimatedEffort))
        inputImportance.setText(ImportanceLabels.toLabel(task.importance), false)
        inputCategory.setText(task.category, false)
    }

    private fun showDatePicker(inputDeadline: TextInputEditText, inputTime: TextInputEditText) {
        val current = currentDeadlineMillis()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.task_deadline)
            .setSelection(
                current?.let { DateUtils.localMillisToUtcPickerDate(it) }
                    ?: MaterialDatePicker.todayInUtcMilliseconds()
            )
            .build()
        picker.addOnPositiveButtonClickListener { utcMidnight ->
            ensureDefaultTime()
            selectedDeadlineMillis = DateUtils.combineUtcPickerDateWithLocalTime(
                utcMidnight,
                selectedHour ?: DateUtils.DEFAULT_DEADLINE_HOUR,
                selectedMinute ?: DateUtils.DEFAULT_DEADLINE_MINUTE
            )
            bindDeadlineFields(inputDeadline, inputTime)
        }
        picker.show(parentFragmentManager, DATE_PICKER_TAG)
    }

    private fun showTimePicker(inputDeadline: TextInputEditText, inputTime: TextInputEditText) {
        ensureDefaultTime()
        val clockFormat = if (DateFormat.is24HourFormat(requireContext())) {
            TimeFormat.CLOCK_24H
        } else {
            TimeFormat.CLOCK_12H
        }
        val picker = MaterialTimePicker.Builder()
            .setTitleText(R.string.task_time)
            .setTimeFormat(clockFormat)
            .setHour(selectedHour ?: DateUtils.DEFAULT_DEADLINE_HOUR)
            .setMinute(selectedMinute ?: DateUtils.DEFAULT_DEADLINE_MINUTE)
            .build()
        picker.addOnPositiveButtonClickListener {
            selectedHour = picker.hour
            selectedMinute = picker.minute
            val dateMillis = selectedDeadlineMillis ?: DateUtils.utcMidnightToLocalStartOfDay(
                MaterialDatePicker.todayInUtcMilliseconds()
            )
            selectedDeadlineMillis = DateUtils.withLocalTime(
                dateMillis,
                picker.hour,
                picker.minute
            )
            bindDeadlineFields(inputDeadline, inputTime)
        }
        picker.show(parentFragmentManager, TIME_PICKER_TAG)
    }

    private fun ensureDefaultTime() {
        if (selectedHour == null || selectedMinute == null) {
            selectedHour = DateUtils.DEFAULT_DEADLINE_HOUR
            selectedMinute = DateUtils.DEFAULT_DEADLINE_MINUTE
        }
    }

    private fun currentDeadlineMillis(): Long? {
        val deadline = selectedDeadlineMillis ?: return null
        val hour = selectedHour ?: DateUtils.DEFAULT_DEADLINE_HOUR
        val minute = selectedMinute ?: DateUtils.DEFAULT_DEADLINE_MINUTE
        return DateUtils.withLocalTime(deadline, hour, minute)
    }

    private fun bindDeadlineFields(inputDeadline: TextInputEditText, inputTime: TextInputEditText) {
        val deadline = currentDeadlineMillis()
        inputDeadline.setText(deadline?.let { DateUtils.formatDisplay(it) }.orEmpty())
        if (selectedHour != null && selectedMinute != null) {
            inputTime.setText(DateUtils.formatTime(selectedHour!!, selectedMinute!!))
        } else {
            inputTime.setText("")
        }
    }

    private fun bindReminderHelper(layoutTime: TextInputLayout) {
        layoutTime.helperText = getString(
            if (NotificationPermissionHelper.canPostNotifications(requireContext())) {
                R.string.task_reminder_helper
            } else {
                R.string.task_reminder_disabled_helper
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedDeadlineMillis?.let { outState.putLong(STATE_DEADLINE, it) }
        selectedHour?.let { outState.putInt(STATE_HOUR, it) }
        selectedMinute?.let { outState.putInt(STATE_MINUTE, it) }
    }

    companion object {
        const val ARG_TASK_ID = "taskId"
        const val ARG_RETURN_TO_DETAILS = "returnToDetails"
        private const val DATE_PICKER_TAG = "task_deadline_picker"
        private const val TIME_PICKER_TAG = "task_time_picker"
        private const val STATE_DEADLINE = "selected_deadline"
        private const val STATE_HOUR = "selected_hour"
        private const val STATE_MINUTE = "selected_minute"
    }
}
