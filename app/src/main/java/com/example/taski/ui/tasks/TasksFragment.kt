package com.example.taski.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.taski.R
import com.example.taski.data.entity.Task
import com.example.taski.viewmodel.TasksViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TasksFragment : Fragment() {

    private val viewModel: TasksViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_tasks, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_tasks)
        val emptyState = view.findViewById<View>(R.id.tasks_empty_state)
        val adapter = TaskAdapter(
            onTaskClick = { openDetails(it.id) },
            onToggleComplete = { task, completed -> viewModel.setCompleted(task, completed) },
            onDelete = { confirmDelete(it) }
        )
        recycler.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener {
            openEditor(0L)
        }

        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
            emptyState.isVisible = tasks.isEmpty()
            recycler.isVisible = tasks.isNotEmpty()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDisplayPriority()
    }

    private fun openDetails(taskId: Long) {
        val args = Bundle().apply { putLong(TaskDetailsFragment.ARG_TASK_ID, taskId) }
        findNavController().navigate(R.id.taskDetailsFragment, args)
    }

    private fun openEditor(taskId: Long) {
        val args = Bundle().apply { putLong(TaskEditorFragment.ARG_TASK_ID, taskId) }
        findNavController().navigate(R.id.taskEditorFragment, args)
    }

    private fun confirmDelete(task: Task) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_task_title)
            .setMessage(getString(R.string.delete_task_message, task.title))
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.delete(task) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
