package com.example.taski.ui.plan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taski.R
import com.example.taski.data.entity.Task
import com.example.taski.priority.PriorityLevel
import com.example.taski.utils.DateUtils
import com.example.taski.utils.ImportanceLabels
import com.google.android.material.button.MaterialButton

class PlanTaskAdapter(
    private val onOpenTask: (Task) -> Unit,
    private val onStartFocus: (Task) -> Unit
) : ListAdapter<Task, PlanTaskAdapter.PlanTaskViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plan_task, parent, false)
        return PlanTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanTaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlanTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTitle = itemView.findViewById<TextView>(R.id.text_title)
        private val textScore = itemView.findViewById<TextView>(R.id.text_score)
        private val textLevel = itemView.findViewById<TextView>(R.id.text_priority_level)
        private val textDeadline = itemView.findViewById<TextView>(R.id.text_deadline)
        private val textEffort = itemView.findViewById<TextView>(R.id.text_effort)
        private val textImportance = itemView.findViewById<TextView>(R.id.text_importance)
        private val buttonOpen = itemView.findViewById<MaterialButton>(R.id.button_open)
        private val buttonStartFocus = itemView.findViewById<MaterialButton>(R.id.button_start_focus)

        fun bind(task: Task) {
            val context = itemView.context
            val level = PriorityLevel.fromScore(task.priorityScore)
            textTitle.text = task.title
            textScore.text = context.getString(R.string.plan_score, task.priorityScore)
            textLevel.text = context.getString(R.string.plan_priority_level, levelLabel(level))
            textLevel.setTextColor(ContextCompat.getColor(context, levelColor(level)))
            textDeadline.text = context.getString(
                R.string.plan_deadline,
                DateUtils.formatDateTime(task.deadline)
            )
            textEffort.text = context.getString(
                R.string.plan_effort,
                DateUtils.formatEffortHours(task.estimatedEffort)
            )
            textImportance.text = context.getString(
                R.string.plan_importance,
                ImportanceLabels.toLabel(task.importance)
            )

            itemView.setOnClickListener { onOpenTask(task) }
            buttonOpen.setOnClickListener { onOpenTask(task) }
            buttonStartFocus.setOnClickListener { onStartFocus(task) }
        }

        private fun levelLabel(level: PriorityLevel): String {
            val context = itemView.context
            return when (level) {
                PriorityLevel.HIGH -> context.getString(R.string.priority_level_high)
                PriorityLevel.MEDIUM -> context.getString(R.string.priority_level_medium)
                PriorityLevel.LOW -> context.getString(R.string.priority_level_low)
            }
        }

        private fun levelColor(level: PriorityLevel): Int = when (level) {
            PriorityLevel.HIGH -> R.color.priority_high
            PriorityLevel.MEDIUM -> R.color.priority_medium
            PriorityLevel.LOW -> R.color.accent
        }
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
