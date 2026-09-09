package com.example.taski.ui.progress

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taski.R
import com.example.taski.data.entity.FocusSessionWithTask

class RecentFocusSessionAdapter(
    private val formatFocusTime: (Long) -> String
) : ListAdapter<FocusSessionWithTask, RecentFocusSessionAdapter.Holder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_focus_session, parent, false)
        return Holder(view, formatFocusTime)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    class Holder(
        itemView: View,
        private val formatFocusTime: (Long) -> String
    ) : RecyclerView.ViewHolder(itemView) {
        private val textTitle = itemView.findViewById<TextView>(R.id.text_title)
        private val textMeta = itemView.findViewById<TextView>(R.id.text_meta)

        fun bind(item: FocusSessionWithTask) {
            textTitle.text = item.taskTitle
            val status = itemView.context.getString(
                if (item.session.completed) {
                    R.string.progress_session_complete
                } else {
                    R.string.progress_session_stopped
                }
            )
            textMeta.text = itemView.context.getString(
                R.string.progress_session_meta,
                formatFocusTime(item.session.duration),
                status
            )
        }
    }

    private companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<FocusSessionWithTask>() {
            override fun areItemsTheSame(
                oldItem: FocusSessionWithTask,
                newItem: FocusSessionWithTask
            ): Boolean = oldItem.session.id == newItem.session.id

            override fun areContentsTheSame(
                oldItem: FocusSessionWithTask,
                newItem: FocusSessionWithTask
            ): Boolean = oldItem == newItem
        }
    }
}
