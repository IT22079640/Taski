package com.example.taski.ui.progress

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taski.R
import com.example.taski.data.entity.Task
import com.example.taski.utils.DateUtils

class RecentCompletedTaskAdapter :
    ListAdapter<Task, RecentCompletedTaskAdapter.Holder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_completed_task, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTitle = itemView.findViewById<TextView>(R.id.text_title)
        private val textMeta = itemView.findViewById<TextView>(R.id.text_meta)

        fun bind(task: Task) {
            textTitle.text = task.title
            val completedAt = task.completedAt
            textMeta.text = if (completedAt != null) {
                itemView.context.getString(
                    R.string.progress_completed_at,
                    DateUtils.formatDisplay(completedAt)
                )
            } else {
                itemView.context.getString(R.string.progress_no_data)
            }
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
