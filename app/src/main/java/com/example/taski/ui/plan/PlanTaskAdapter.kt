package com.example.taski.ui.plan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taski.R
import com.example.taski.plan.FocusPlanBuilder
import com.example.taski.plan.PlanUrgency
import com.example.taski.plan.PlannedTask
import com.example.taski.priority.PriorityLevel
import com.example.taski.utils.DateUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class PlanTaskAdapter(
    private val onOpenTask: (PlannedTask) -> Unit,
    private val onStartFocus: (PlannedTask) -> Unit
) : ListAdapter<PlannedTask, PlanTaskAdapter.PlanTaskViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plan_task, parent, false)
        return PlanTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanTaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlanTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card = itemView as MaterialCardView
        private val textTitle = itemView.findViewById<TextView>(R.id.text_title)
        private val textUrgentBadge = itemView.findViewById<TextView>(R.id.text_urgent_badge)
        private val textPriorityScore = itemView.findViewById<TextView>(R.id.text_priority_score)
        private val textUrgency = itemView.findViewById<TextView>(R.id.text_urgency)
        private val textDeadline = itemView.findViewById<TextView>(R.id.text_deadline)
        private val textEffort = itemView.findViewById<TextView>(R.id.text_effort)
        private val textPlannedFocus = itemView.findViewById<TextView>(R.id.text_planned_focus)
        private val buttonOpen = itemView.findViewById<MaterialButton>(R.id.button_open)
        private val buttonStartFocus = itemView.findViewById<MaterialButton>(R.id.button_start_focus)

        fun bind(item: PlannedTask) {
            val context = itemView.context
            val task = item.task
            val level = PriorityLevel.fromScore(task.priorityScore)
            val urgent = PlanUrgency.isUrgent(task)
            textTitle.text = task.title
            textPriorityScore.text = context.getString(
                R.string.plan_priority_score_line,
                levelLabel(level).uppercase(),
                task.priorityScore
            )
            textPriorityScore.setTextColor(ContextCompat.getColor(context, levelColor(level)))
            textUrgency.text = PlanUrgency.label(task)
            textDeadline.text = context.getString(
                R.string.plan_deadline,
                DateUtils.formatDateTime(task.deadline)
            )
            textEffort.text = context.getString(
                R.string.plan_effort,
                FocusPlanBuilder.windowLabel(task.estimatedEffort)
            )
            textPlannedFocus.text = context.getString(
                R.string.plan_planned_focus_value,
                FocusPlanBuilder.windowLabel(item.plannedMinutes)
            )
            textUrgentBadge.isVisible = urgent
            bindUrgentCard(urgent)

            itemView.setOnClickListener { onOpenTask(item) }
            itemView.contentDescription = context.getString(
                R.string.cd_plan_task,
                task.title,
                task.priorityScore
            )
            buttonOpen.setOnClickListener { onOpenTask(item) }
            buttonStartFocus.setOnClickListener { onStartFocus(item) }
        }

        private fun bindUrgentCard(urgent: Boolean) {
            val context = itemView.context
            if (urgent) {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.plan_urgent_background))
                card.strokeColor = ContextCompat.getColor(context, R.color.plan_urgent_stroke)
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
                card.strokeColor = ContextCompat.getColor(context, R.color.outline)
            }
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
        val DiffCallback = object : DiffUtil.ItemCallback<PlannedTask>() {
            override fun areItemsTheSame(oldItem: PlannedTask, newItem: PlannedTask): Boolean =
                oldItem.task.id == newItem.task.id

            override fun areContentsTheSame(oldItem: PlannedTask, newItem: PlannedTask): Boolean =
                oldItem == newItem
        }
    }
}
