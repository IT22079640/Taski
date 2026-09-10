package com.example.taski.ui.tasks

import android.os.Bundle
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
import com.example.taski.ui.priority.PriorityResultFragment
import com.example.taski.utils.DateUtils
import com.example.taski.utils.ImportanceLabels
import com.example.taski.viewmodel.TaskEditorViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class TaskEditorFragment : Fragment() {

    private val viewModel: TaskEditorViewModel by viewModels()

    private var selectedDeadlineMillis: Long? = null
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
        if (savedInstanceState?.containsKey(STATE_DEADLINE) == true) {
            selectedDeadlineMillis = savedInstanceState.getLong(STATE_DEADLINE)
        }

        val toolbar = view.findViewById<MaterialToolbar>(R.id.editor_toolbar)
        val layoutTitle = view.findViewById<TextInputLayout>(R.id.layout_title)
        val layoutDeadline = view.findViewById<TextInputLayout>(R.id.layout_deadline)
        val layoutEffort = view.findViewById<TextInputLayout>(R.id.layout_effort)
        val layoutImportance = view.findViewById<TextInputLayout>(R.id.layout_importance)
        val layoutCategory = view.findViewById<TextInputLayout>(R.id.layout_category)
        val inputTitle = view.findViewById<TextInputEditText>(R.id.input_title)
        val inputDescription = view.findViewById<TextInputEditText>(R.id.input_description)
        val inputDeadline = view.findViewById<TextInputEditText>(R.id.input_deadline)
        val inputEffort = view.findViewById<TextInputEditText>(R.id.input_effort)
        val inputImportance = view.findViewById<AutoCompleteTextView>(R.id.input_importance)
        val inputCategory = view.findViewById<AutoCompleteTextView>(R.id.input_category)
        val saveButton = view.findViewById<MaterialButton>(R.id.btn_save_task)

        toolbar.setTitle(if (isEditing) R.string.edit_task_title else R.string.add_task_title)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        saveButton.setText(if (isEditing) R.string.task_save_changes else R.string.task_save)

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

        val openDatePicker = {
            showDatePicker(inputDeadline)
        }
        inputDeadline.setOnClickListener { openDatePicker() }
        layoutDeadline.setEndIconOnClickListener { openDatePicker() }

        saveButton.setOnClickListener {
            viewModel.save(
                title = inputTitle.text?.toString().orEmpty(),
                description = inputDescription.text?.toString().orEmpty(),
                deadlineMillis = selectedDeadlineMillis,
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
                    bindTask(task, inputTitle, inputDescription, inputDeadline, inputEffort, inputImportance, inputCategory)
                }
            }
        }
    }

    private fun bindTask(
        task: Task,
        inputTitle: TextInputEditText,
        inputDescription: TextInputEditText,
        inputDeadline: TextInputEditText,
        inputEffort: TextInputEditText,
        inputImportance: AutoCompleteTextView,
        inputCategory: AutoCompleteTextView
    ) {
        selectedDeadlineMillis = task.deadline
        inputTitle.setText(task.title)
        inputDescription.setText(task.description)
        inputDeadline.setText(DateUtils.formatDisplay(task.deadline))
        inputEffort.setText(DateUtils.minutesToHoursText(task.estimatedEffort))
        inputImportance.setText(ImportanceLabels.toLabel(task.importance), false)
        inputCategory.setText(task.category, false)
    }

    private fun showDatePicker(inputDeadline: TextInputEditText) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.task_deadline)
            .setSelection(selectedDeadlineMillis ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            selectedDeadlineMillis = DateUtils.utcMidnightToLocalStartOfDay(millis)
            inputDeadline.setText(DateUtils.formatDisplay(selectedDeadlineMillis!!))
        }
        picker.show(parentFragmentManager, DATE_PICKER_TAG)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedDeadlineMillis?.let { outState.putLong(STATE_DEADLINE, it) }
    }

    companion object {
        const val ARG_TASK_ID = "taskId"
        const val ARG_RETURN_TO_DETAILS = "returnToDetails"
        private const val DATE_PICKER_TAG = "task_deadline_picker"
        private const val STATE_DEADLINE = "selected_deadline"
    }
}
