package com.example.taski.ui.tasks

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taski.R
import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task
import com.example.taski.utils.DateUtils
import com.example.taski.utils.ImportanceLabels
import com.google.android.material.checkbox.MaterialCheckBox

class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onToggleComplete: (Task, Boolean) -> Unit,
    private val onDelete: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkboxComplete = itemView.findViewById<MaterialCheckBox>(R.id.checkbox_complete)
        private val textTitle = itemView.findViewById<TextView>(R.id.text_title)
        private val textMeta = itemView.findViewById<TextView>(R.id.text_meta)
        private val textImportance = itemView.findViewById<TextView>(R.id.text_importance)
        private val textScore = itemView.findViewById<TextView>(R.id.text_score)
        private val buttonDelete = itemView.findViewById<ImageButton>(R.id.button_delete)

        fun bind(task: Task) {
            val context = itemView.context
            textTitle.text = task.title
            textMeta.text = context.getString(
                R.string.task_meta,
                task.category,
                DateUtils.formatDisplay(task.deadline),
                DateUtils.formatEffortHours(task.estimatedEffort)
            )
            textImportance.text = if (task.completed) {
                context.getString(R.string.task_completed_label)
            } else {
                ImportanceLabels.toLabel(task.importance)
            }
            textImportance.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (task.completed) R.color.completed_text else importanceColor(task.importance)
                )
            )
            textScore.text = context.getString(R.string.task_score, task.priorityScore)

            val strike = if (task.completed) {
                textTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                textTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            textTitle.paintFlags = strike
            val titleColor = ContextCompat.getColor(
                context,
                if (task.completed) R.color.completed_text else R.color.text_primary
            )
            textTitle.setTextColor(titleColor)
            itemView.alpha = if (task.completed) 0.72f else 1f

            checkboxComplete.setOnCheckedChangeListener(null)
            checkboxComplete.isChecked = task.completed
            checkboxComplete.contentDescription = context.getString(
                if (task.completed) R.string.task_incomplete else R.string.task_complete
            )
            checkboxComplete.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != task.completed) {
                    onToggleComplete(task, isChecked)
                }
            }

            itemView.setOnClickListener { onTaskClick(task) }
            buttonDelete.setOnClickListener { onDelete(task) }
        }
    }

    private fun importanceColor(importance: Importance): Int = when (importance) {
        Importance.HIGH -> R.color.priority_high
        Importance.MEDIUM -> R.color.priority_medium
        Importance.LOW -> R.color.priority_low
    }

    private companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean =
                oldItem == newItem
        }
    }
}
